package com.censustracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.censustracker.domain.model.Contact
import com.censustracker.domain.model.Message
import com.censustracker.domain.model.PresetMessage
import com.censustracker.domain.usecase.AddMessageUseCase
import com.censustracker.domain.usecase.GetAllContactsUseCase
import com.censustracker.domain.usecase.GetAllPresetMessagesUseCase
import com.censustracker.domain.usecase.GetCollectorProfileUseCase
import com.censustracker.domain.usecase.UpdateMessageStatusUseCase
import com.censustracker.logging.ErrorLogger
import com.censustracker.placeholder.PlaceholderResolver
import com.censustracker.preferences.AppPreferences
import com.censustracker.sms.SmsSender
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BulkSmsViewModel @Inject constructor(
    private val getAllContactsUseCase: GetAllContactsUseCase,
    private val getCollectorProfileUseCase: GetCollectorProfileUseCase,
    private val getAllPresetMessagesUseCase: GetAllPresetMessagesUseCase,
    private val addMessageUseCase: AddMessageUseCase,
    private val updateMessageStatusUseCase: UpdateMessageStatusUseCase,
    private val smsSender: SmsSender,
    private val appPreferences: AppPreferences,
    private val errorLogger: ErrorLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val contacts: List<Contact> = emptyList(),
        val selectedIds: Set<Long> = emptySet(),
        val presets: List<PresetMessage> = emptyList(),
        val body: String = "",
        val isLoading: Boolean = true,
        val isSending: Boolean = false,
        val sentCount: Int = 0,
        val failedCount: Int = 0,
        val totalToSend: Int = 0,
        val finished: Boolean = false,
        val error: String? = null,
        val toastError: String? = null
    )

    init {
        viewModelScope.launch {
            try {
                val contacts = getAllContactsUseCase()
                val presets = getAllPresetMessagesUseCase()
                _uiState.update { it.copy(contacts = contacts, presets = presets, isLoading = false) }
            } catch (e: Exception) {
                errorLogger.log("load_broadcast", "Failed to load contacts/presets", e.message)
                _uiState.update { it.copy(isLoading = false, error = "Load failed: ${e.message}") }
            }
        }
    }

    fun onToggleContact(contactId: Long) {
        _uiState.update {
            val sel = it.selectedIds.toMutableSet()
            if (!sel.add(contactId)) sel.remove(contactId)
            it.copy(selectedIds = sel, finished = false)
        }
    }

    fun onSelectAll() {
        _uiState.update { it.copy(selectedIds = it.contacts.map { c -> c.id }.toSet(), finished = false) }
    }

    fun onClearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), finished = false) }
    }

    fun onPresetSelected(preset: PresetMessage?) {
        _uiState.update {
            it.copy(body = preset?.bodyTemplate ?: it.body, finished = false)
        }
    }

    fun onBodyChanged(text: String) {
        _uiState.update { it.copy(body = text, finished = false) }
    }

    fun sendBroadcast() {
        val state = _uiState.value
        if (state.isSending || state.selectedIds.isEmpty() || state.body.isBlank()) return
        if (!appPreferences.smsEnabled) {
            errorLogger.logAsync("broadcast_blocked", "Broadcast SMS blocked: SMS disabled in settings", null)
            _uiState.update { it.copy(toastError = "SMS messaging is disabled in Settings") }
            return
        }

        val targets = state.contacts.filter { it.id in state.selectedIds }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    finished = false,
                    sentCount = 0,
                    failedCount = 0,
                    totalToSend = targets.size
                )
            }
            try {
                val profile = getCollectorProfileUseCase()
                for (contact in targets) {
                    val resolver = PlaceholderResolver(collectorProfile = profile, contact = contact)
                    val resolvedBody = resolver.resolve(state.body)

                    var dbMsgId = 0L
                    try {
                        dbMsgId = addMessageUseCase(
                            Message(
                                contactId = contact.id,
                                phoneNumber = contact.phoneNumber,
                                direction = "SENT",
                                channel = "SMS",
                                body = resolvedBody,
                                status = "PENDING",
                                sentAt = System.currentTimeMillis()
                            )
                        )
                        val result = smsSender.send(contact.phoneNumber, resolvedBody, dbMsgId)
                        val status = if (result.success) "SENT" else "FAILED"
                        updateMessageStatusUseCase(dbMsgId, status)
                        _uiState.update {
                            it.copy(
                                sentCount = it.sentCount + if (result.success) 1 else 0,
                                failedCount = it.failedCount + if (result.success) 0 else 1
                            )
                        }
                    } catch (e: Exception) {
                        if (dbMsgId > 0L) {
                            try { updateMessageStatusUseCase(dbMsgId, "FAILED") } catch (_: Exception) {}
                        }
                        errorLogger.log(
                            action = "broadcast_sms",
                            message = "Broadcast SMS to '${contact.name}' failed",
                            detail = e.message
                        )
                        _uiState.update { it.copy(failedCount = it.failedCount + 1) }
                    }
                }
            } catch (e: Exception) {
                errorLogger.log("broadcast_sms", "Broadcast run failed", e.message)
                _uiState.update { it.copy(toastError = "Broadcast failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSending = false, finished = true) }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastError = null, error = null) }
    }
}
