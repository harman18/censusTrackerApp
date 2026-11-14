package com.censustracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.censustracker.domain.model.Message
import com.censustracker.domain.usecase.DeleteMessageUseCase
import com.censustracker.domain.usecase.GetAllContactsUseCase
import com.censustracker.domain.usecase.UpdateMessageStatusUseCase
import com.censustracker.domain.usecase.WatchRetryableMessagesUseCase
import com.censustracker.logging.ErrorLogger
import com.censustracker.preferences.AppPreferences
import com.censustracker.sms.SmsSender
import com.censustracker.whatsapp.WhatsAppSender
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Retry queue for outgoing messages that are PENDING (never dispatched) or
 * FAILED*. Each entry can be retried or deleted (cancelled) individually.
 */
@HiltViewModel
class FailedMessagesViewModel @Inject constructor(
    private val watchRetryableMessagesUseCase: WatchRetryableMessagesUseCase,
    private val getAllContactsUseCase: GetAllContactsUseCase,
    private val updateMessageStatusUseCase: UpdateMessageStatusUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val smsSender: SmsSender,
    private val whatsAppSender: WhatsAppSender,
    private val appPreferences: AppPreferences,
    private val errorLogger: ErrorLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val messages: List<Message> = emptyList(),
        val contactNames: Map<Long, String> = emptyMap(),
        val retryingId: Long? = null,
        val isLoading: Boolean = true,
        val toastSuccess: String? = null,
        val toastError: String? = null
    )

    init {
        viewModelScope.launch {
            watchRetryableMessagesUseCase.watch()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, toastError = "Load failed: ${e.message}") }
                    errorLogger.log("load_retry_queue", "Failed to load retry queue", e.message)
                }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                }
        }
        viewModelScope.launch {
            try {
                val contacts = getAllContactsUseCase()
                _uiState.update { it.copy(contactNames = contacts.associate { c -> c.id to c.name }) }
            } catch (e: Exception) {
                errorLogger.log("load_retry_queue", "Failed to load contact names", e.message)
            }
        }
    }

    fun contactName(message: Message): String =
        _uiState.value.contactNames[message.contactId] ?: message.phoneNumber

    fun retry(message: Message) {
        val channelEnabled = when (message.channel) {
            "WHATSAPP" -> appPreferences.whatsappEnabled
            else -> appPreferences.smsEnabled
        }
        if (!channelEnabled) {
            errorLogger.logAsync(
                action = "retry_blocked",
                message = "Retry of message ${message.id} blocked: ${message.channel} disabled",
                detail = null
            )
            _uiState.update {
                it.copy(toastError = "${message.channel} messaging is disabled in Settings")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(retryingId = message.id) }
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
                _uiState.update {
                    it.copy(
                        toastSuccess = if (result.first) "Message re-dispatched" else null,
                        toastError = if (result.first) null else "Retry failed: ${result.second}"
                    )
                }
            } catch (e: Exception) {
                errorLogger.log("retry_queued_sms", "Retry of message ${message.id} failed", e.message)
                _uiState.update { it.copy(toastError = "Retry failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(retryingId = null) }
            }
        }
    }

    fun delete(message: Message) {
        viewModelScope.launch {
            try {
                deleteMessageUseCase(message.id)
                _uiState.update { it.copy(toastSuccess = "Queued message removed") }
            } catch (e: Exception) {
                errorLogger.log("delete_queued_sms", "Failed to delete message ${message.id}", e.message)
                _uiState.update { it.copy(toastError = "Delete failed: ${e.message}") }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastSuccess = null, toastError = null) }
    }
}
