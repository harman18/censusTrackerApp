package com.censustracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.censustracker.domain.model.CollectorProfile
import com.censustracker.domain.model.Contact
import com.censustracker.domain.model.Message
import com.censustracker.domain.model.PresetMessage
import com.censustracker.domain.usecase.AddMessageUseCase
import com.censustracker.domain.usecase.GetAllPresetMessagesUseCase
import com.censustracker.domain.usecase.GetCollectorProfileUseCase
import com.censustracker.domain.usecase.GetContactUseCase
import com.censustracker.domain.usecase.GetMessagesByContactUseCase
import com.censustracker.domain.usecase.MarkContactMessagesReadUseCase
import com.censustracker.domain.usecase.UpdateContactUseCase
import com.censustracker.domain.usecase.UpdateMessageStatusUseCase
import com.censustracker.logging.ErrorLogger
import com.censustracker.placeholder.PlaceholderResolver
import com.censustracker.preferences.AppPreferences
import com.censustracker.sms.SmsSender
import com.censustracker.whatsapp.WhatsAppSender
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getContactUseCase: GetContactUseCase,
    private val getCollectorProfileUseCase: GetCollectorProfileUseCase,
    private val getAllPresetMessagesUseCase: GetAllPresetMessagesUseCase,
    private val addMessageUseCase: AddMessageUseCase,
    private val getMessagesByContactUseCase: GetMessagesByContactUseCase,
    private val updateMessageStatusUseCase: UpdateMessageStatusUseCase,
    private val updateContactUseCase: UpdateContactUseCase,
    private val markContactMessagesReadUseCase: MarkContactMessagesReadUseCase,
    private val smsSender: SmsSender,
    private val whatsAppSender: WhatsAppSender,
    private val appPreferences: AppPreferences,
    private val errorLogger: ErrorLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    data class UiState(
        val contact: Contact? = null,
        val collectorProfile: CollectorProfile? = null,
        val presets: List<PresetMessage> = emptyList(),
        val selectedPreset: PresetMessage? = null,
        val messages: List<Message> = emptyList(),
        val inputText: String = "",
        val selectedChannel: String = "SMS",
        val smsEnabled: Boolean = true,
        val whatsappEnabled: Boolean = true,
        val isLoading: Boolean = false,
        val isSendingMessage: Boolean = false,
        val error: String? = null,
        val toastSuccess: String? = null,
        val toastError: String? = null
    )

    fun init(contactId: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val contact = getContactUseCase(contactId)
            val profile = getCollectorProfileUseCase()
            val presetList = getAllPresetMessagesUseCase()
            // Mark incoming messages as read so the unread badge clears
            try { markContactMessagesReadUseCase(contactId) } catch (_: Exception) {}
            _uiState.update {
                it.copy(
                    contact = contact,
                    collectorProfile = profile,
                    presets = presetList,
                    smsEnabled = appPreferences.smsEnabled,
                    whatsappEnabled = appPreferences.whatsappEnabled,
                    // WhatsApp-only build: never default to the disabled SMS channel
                    selectedChannel = if (appPreferences.smsEnabled) "SMS" else "WHATSAPP",
                    isLoading = false
                )
            }
        }

        // Real-time reactive flow observation for chat messages
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getMessagesByContactUseCase.watch(contactId)
                .catch { e ->
                    _uiState.update { it.copy(error = "Message stream error: ${e.message}") }
                }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, selectedPreset = null) }
    }

    fun onSelectPreset(preset: PresetMessage) {
        val contact = _uiState.value.contact
        val profile = _uiState.value.collectorProfile
        val resolver = PlaceholderResolver(collectorProfile = profile, contact = contact)
        val resolved = resolver.resolve(preset.bodyTemplate)

        _uiState.update {
            it.copy(
                selectedPreset = preset,
                inputText = resolved
            )
        }
    }

    fun onInsertPlaceholder(placeholder: String) {
        _uiState.update {
            it.copy(inputText = it.inputText + " " + placeholder)
        }
    }

    fun onChannelChanged(channel: String) {
        _uiState.update {
            // Respect the system-level kill switches
            val allowed = when (channel) {
                "WHATSAPP" -> it.whatsappEnabled
                else -> it.smsEnabled
            }
            if (allowed) it.copy(selectedChannel = channel) else it
        }
    }

    /** True when the given channel is currently enabled by the system knobs. */
    private fun channelEnabled(channel: String): Boolean =
        when (channel) {
            "WHATSAPP" -> appPreferences.whatsappEnabled
            else -> appPreferences.smsEnabled
        }

    fun onSendMessage() {
        val state = _uiState.value
        val contact = state.contact ?: return
        val text = state.inputText.trim()
        if (text.isBlank()) return

        val channel = state.selectedChannel
        if (!channelEnabled(channel)) {
            errorLogger.logAsync(
                action = "send_blocked",
                message = "Send via $channel blocked: channel disabled in settings",
                detail = "contact=${contact.phoneNumber}"
            )
            _uiState.update {
                it.copy(toastError = "$channel messaging is disabled in Settings")
            }
            return
        }

        _uiState.update { it.copy(isSendingMessage = true, inputText = "", selectedPreset = null) }

        val resolver = PlaceholderResolver(
            collectorProfile = state.collectorProfile,
            contact = contact
        )
        val resolvedBody = resolver.resolve(text)

        viewModelScope.launch {
            var dbMsgId = 0L
            try {
                // Store as PENDING in Room
                val pendingMessage = Message(
                    contactId = contact.id,
                    phoneNumber = contact.phoneNumber,
                    direction = "SENT",
                    channel = channel,
                    body = resolvedBody,
                    status = "PENDING",
                    sentAt = System.currentTimeMillis()
                )
                dbMsgId = addMessageUseCase(pendingMessage)

                val result = if (channel == "WHATSAPP") {
                    val r = whatsAppSender.send(contact.phoneNumber, resolvedBody)
                    SmsSendOutcome(success = r.success, error = r.error)
                } else {
                    val r = smsSender.send(contact.phoneNumber, resolvedBody, dbMsgId)
                    SmsSendOutcome(success = r.success, error = r.error)
                }
                val status = if (result.success) "SENT" else "FAILED"
                updateMessageStatusUseCase(dbMsgId, status)

                if (result.success) {
                    _uiState.update {
                        it.copy(toastSuccess = if (channel == "WHATSAPP") "WhatsApp chat opened" else "SMS dispatched to carrier")
                    }
                } else {
                    _uiState.update { it.copy(toastError = "Failed: ${result.error}") }
                }
            } catch (e: Exception) {
                if (dbMsgId > 0L) {
                    try { updateMessageStatusUseCase(dbMsgId, "FAILED") } catch (_: Exception) {}
                }
                errorLogger.log(
                    action = "send_$channel",
                    message = "Failed to send via $channel to ${contact.phoneNumber}",
                    detail = e.message
                )
                _uiState.update {
                    it.copy(
                        error = "Send error: ${e.message}",
                        toastError = "Error: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isSendingMessage = false) }
            }
        }
    }

    private data class SmsSendOutcome(val success: Boolean, val error: String?)

    fun retrySendMessage(message: Message) {
        val state = _uiState.value
        if (state.contact == null) return
        if (!channelEnabled(message.channel)) {
            errorLogger.logAsync(
                action = "retry_blocked",
                message = "Retry of message ${message.id} blocked: ${message.channel} disabled",
                detail = null
            )
            _uiState.update { it.copy(toastError = "${message.channel} messaging is disabled in Settings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true) }
            try {
                updateMessageStatusUseCase(message.id, "PENDING")
                val result = if (message.channel == "WHATSAPP") {
                    val r = whatsAppSender.send(message.phoneNumber, message.body)
                    r.success to r.error
                } else {
                    val r = smsSender.send(message.phoneNumber, message.body, message.id)
                    r.success to r.error
                }
                val status = if (result.first) "SENT" else "FAILED"
                updateMessageStatusUseCase(message.id, status)

                if (result.first) {
                    _uiState.update { it.copy(toastSuccess = "Message re-sent") }
                } else {
                    _uiState.update { it.copy(toastError = "Re-send failed: ${result.second}") }
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "retry_${message.channel}",
                    message = "Retry of message ${message.id} failed",
                    detail = e.message
                )
                _uiState.update { it.copy(toastError = "Failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSendingMessage = false) }
            }
        }
    }

    /** Persist edits to the contact's name / house / location / email fields. */
    fun updateContact(name: String, houseNo: String, location: String, email: String) {
        val contact = _uiState.value.contact ?: return
        viewModelScope.launch {
            try {
                val updated = contact.copy(
                    name = name.trim().ifBlank { contact.name },
                    houseNo = houseNo.trim().takeIf { it.isNotBlank() },
                    location = location.trim().takeIf { it.isNotBlank() },
                    email = email.trim().takeIf { it.isNotBlank() }
                )
                updateContactUseCase(updated)
                _uiState.update { it.copy(contact = updated, toastSuccess = "Contact updated") }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "update_contact",
                    message = "Failed to update contact '${contact.name}'",
                    detail = e.message
                )
                _uiState.update { it.copy(toastError = "Update failed: ${e.message}") }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastSuccess = null, toastError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
    }
}
