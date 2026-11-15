package com.censustracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.censustracker.contacts.PhoneContactWriter
import com.censustracker.domain.model.CollectorProfile
import com.censustracker.domain.model.Contact
import com.censustracker.domain.model.Message
import com.censustracker.domain.model.PresetMessage
import com.censustracker.domain.usecase.AddContactUseCase
import com.censustracker.domain.usecase.AddMessageUseCase
import com.censustracker.domain.usecase.GetAllPresetMessagesUseCase
import com.censustracker.domain.usecase.GetCollectorProfileUseCase
import com.censustracker.domain.usecase.UpdateMessageStatusUseCase
import com.censustracker.logging.ErrorLogger
import com.censustracker.placeholder.PlaceholderResolver
import com.censustracker.preferences.AppPreferences
import com.censustracker.sms.SmsSender
import com.censustracker.util.PhoneNumberFormatter
import com.censustracker.whatsapp.WhatsAppSender
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val addContactUseCase: AddContactUseCase,
    private val addMessageUseCase: AddMessageUseCase,
    private val updateMessageStatusUseCase: UpdateMessageStatusUseCase,
    private val getAllPresetMessagesUseCase: GetAllPresetMessagesUseCase,
    private val getCollectorProfileUseCase: GetCollectorProfileUseCase,
    private val smsSender: SmsSender,
    private val whatsAppSender: WhatsAppSender,
    private val phoneContactWriter: PhoneContactWriter,
    private val appPreferences: AppPreferences,
    private val errorLogger: ErrorLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val name: String = "",
        val phoneNumber: String = "",
        val buildingId: String = "",
        val houseNo: String = "",
        val autoFormatName: Boolean = true,
        val email: String = "",
        val savedToPhone: Boolean = true,
        val sendNow: Boolean = false,
        val channel: String = "SMS",
        val countryCode: String = "+91",
        val smsEnabled: Boolean = true,
        val whatsappEnabled: Boolean = true,
        val presetMessages: List<PresetMessage> = emptyList(),
        val selectedPreset: PresetMessage? = null,
        val customBody: String = "",
        val selectedPlaceholders: List<String> = emptyList(),
        val collectorProfile: CollectorProfile? = null,
        val nameError: String? = null,
        val phoneError: String? = null,
        val emailError: String? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val toastSuccess: String? = null,
        val toastError: String? = null,
        val success: Boolean = false
    )

    init {
        // Restore user's last selected preferences dynamically
        _uiState.update {
            it.copy(
                savedToPhone = appPreferences.defaultSaveToPhone,
                sendNow = appPreferences.defaultSendNow,
                customBody = appPreferences.lastCustomBody,
                countryCode = appPreferences.defaultCountryCode,
                channel = appPreferences.lastChannel,
                autoFormatName = appPreferences.autoFormatName,
                smsEnabled = appPreferences.smsEnabled,
                whatsappEnabled = appPreferences.whatsappEnabled
            ).let { s ->
                // Fall back to an enabled channel if the remembered one is disabled
                val channelOk = when (s.channel) {
                    "WHATSAPP" -> s.whatsappEnabled
                    else -> s.smsEnabled
                }
                if (channelOk) s
                else s.copy(channel = if (s.whatsappEnabled) "WHATSAPP" else "SMS")
            }
        }
        loadPresetsAndProfile()
    }

    fun loadPresetsAndProfile() {
        viewModelScope.launch {
            val presets = getAllPresetMessagesUseCase()
            val profile = getCollectorProfileUseCase()

            val savedPresetId = appPreferences.lastSelectedPresetId
            val matchedPreset = presets.find { it.id == savedPresetId }

            _uiState.update { current ->
                current.copy(
                    presetMessages = presets,
                    collectorProfile = profile,
                    selectedPreset = matchedPreset ?: current.selectedPreset,
                    customBody = if (current.customBody.isBlank() && matchedPreset != null) matchedPreset.bodyTemplate else current.customBody
                )
            }
        }
    }

    fun onNameChanged(text: String) {
        _uiState.update {
            it.copy(
                name = text,
                nameError = if (text.isNotBlank() && text.trim().length < 2) "Name must be at least 2 characters" else null,
                error = null
            )
        }
    }

    fun onPhoneChanged(text: String) {
        // National number only — the country code is held separately. Max 10 digits.
        val filtered = text.filter { it.isDigit() }.take(10)
        _uiState.update {
            it.copy(
                phoneNumber = filtered,
                phoneError = null,
                error = null
            )
        }
    }

    fun onAutoFormatNameChanged(checked: Boolean) {
        appPreferences.autoFormatName = checked
        _uiState.update { it.copy(autoFormatName = checked) }
    }

    fun onCountryCodeChanged(text: String) {
        val cleaned = text.trim().let { if (it.startsWith("+")) it else "+$it" }.filter { it == '+' || it.isDigit() }
        appPreferences.defaultCountryCode = cleaned.ifBlank { "+91" }
        _uiState.update { it.copy(countryCode = cleaned.ifBlank { "+91" }) }
    }

    fun onBuildingIdChanged(text: String) {
        _uiState.update { it.copy(buildingId = text) }
    }

    fun onHouseNoChanged(text: String) {
        _uiState.update { it.copy(houseNo = text) }
    }

    fun onEmailChanged(text: String) {
        val emailValid = text.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(text.trim()).matches()
        _uiState.update {
            it.copy(
                email = text,
                emailError = if (!emailValid) "Enter a valid email address" else null,
                error = null
            )
        }
    }

    fun onSavedToPhoneChanged(checked: Boolean) {
        appPreferences.defaultSaveToPhone = checked
        _uiState.update { it.copy(savedToPhone = checked) }
    }

    fun onSendNowChanged(checked: Boolean) {
        appPreferences.defaultSendNow = checked
        _uiState.update { it.copy(sendNow = checked) }
    }

    fun onChannelChanged(channel: String) {
        val allowed = when (channel) {
            "WHATSAPP" -> _uiState.value.whatsappEnabled
            else -> _uiState.value.smsEnabled
        }
        if (allowed) {
            appPreferences.lastChannel = channel
            _uiState.update { it.copy(channel = channel) }
        } else {
            _uiState.update { it.copy(toastError = "$channel messaging is disabled in Settings") }
        }
    }

    fun onPresetSelected(preset: PresetMessage?) {
        val body = preset?.bodyTemplate ?: _uiState.value.customBody
        appPreferences.lastSelectedPresetId = preset?.id ?: -1L
        appPreferences.lastCustomBody = body
        _uiState.update { it.copy(selectedPreset = preset, customBody = body) }
    }

    fun onCustomBodyChanged(text: String) {
        appPreferences.lastCustomBody = text
        _uiState.update { it.copy(customBody = text, selectedPreset = null) }
    }

    fun onInsertPlaceholder(placeholder: String) {
        val newBody = _uiState.value.customBody + " " + placeholder
        appPreferences.lastCustomBody = newBody
        _uiState.update {
            it.copy(customBody = newBody)
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastSuccess = null, toastError = null) }
    }

    fun renderBody(): String {
        val state = _uiState.value
        val tempContact = Contact(
            name = if (_uiState.value.autoFormatName) {
                listOfNotNull(
                    _uiState.value.buildingId.trim().takeIf { it.isNotBlank() }?.let { "B$it" },
                    _uiState.value.houseNo.trim().takeIf { it.isNotBlank() }?.let { "H$it" },
                    state.name.trim().takeIf { it.isNotBlank() } ?: "Respondent Name"
                ).joinToString(" ")
            } else {
                state.name.ifBlank { "Respondent Name" }
            },
            phoneNumber = PhoneNumberFormatter.normalize(state.phoneNumber, state.countryCode).ifBlank { "+919999999999" },
            houseNo = state.houseNo.ifBlank { "House No" },
            location = null,
            email = state.email.ifBlank { "email@example.com" }
        )
        val resolver = PlaceholderResolver(
            collectorProfile = state.collectorProfile,
            contact = tempContact
        )
        return resolver.resolve(state.customBody)
    }

    fun saveContact(): Boolean {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val trimmedPhone = PhoneNumberFormatter.normalize(state.phoneNumber.trim(), state.countryCode)
        val trimmedEmail = state.email.trim()

        val nameErr = when {
            trimmedName.isBlank() -> "Respondent name is required"
            trimmedName.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }

        val phoneErr = when {
            state.phoneNumber.isBlank() -> "Phone number is required"
            state.phoneNumber.length != 10 -> "Phone number must be exactly 10 digits"
            else -> null
        }

        val emailErr = if (trimmedEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            "Enter a valid email address"
        } else null

        if (nameErr != null || phoneErr != null || emailErr != null) {
            _uiState.update {
                it.copy(
                    nameError = nameErr,
                    phoneError = phoneErr,
                    emailError = emailErr,
                    toastError = "Please fix errors before saving",
                    error = "Please fix the validation errors above"
                )
            }
            return false
        }

        _uiState.update { it.copy(isLoading = true, error = null, nameError = null, phoneError = null, emailError = null) }

        // When auto-format is on, the saved contact name becomes "B<id> H<house> <owner name>"
        // even though the name field itself keeps showing only the owner name.
        val savedName = if (state.autoFormatName) {
            listOfNotNull(
                state.buildingId.trim().takeIf { it.isNotBlank() }?.let { "B$it" },
                state.houseNo.trim().takeIf { it.isNotBlank() }?.let { "H$it" },
                trimmedName
            ).joinToString(" ")
        } else {
            trimmedName
        }

        val contact = Contact(
            name = savedName,
            phoneNumber = trimmedPhone,
            houseNo = state.houseNo.trim().takeIf { it.isNotBlank() },
            location = null,
            email = trimmedEmail.takeIf { it.isNotBlank() },
            savedToPhone = state.savedToPhone,
            channelPreference = state.channel
        )

        viewModelScope.launch {
            try {
                val contactId = addContactUseCase(contact)

                if (state.savedToPhone) {
                    phoneContactWriter.saveContact(contact.name, contact.phoneNumber, contact.email)
                }

                val channelEnabled = when (state.channel) {
                    "WHATSAPP" -> appPreferences.whatsappEnabled
                    else -> appPreferences.smsEnabled
                }
                if (state.sendNow && state.customBody.isNotBlank() && channelEnabled) {
                    val resolver = PlaceholderResolver(
                        collectorProfile = state.collectorProfile,
                        contact = contact.copy(id = contactId)
                    )
                    val body = resolver.resolve(state.customBody)

                    val initialMessage = Message(
                        contactId = contactId,
                        phoneNumber = contact.phoneNumber,
                        direction = "SENT",
                        channel = state.channel,
                        body = body,
                        status = "PENDING",
                        sentAt = System.currentTimeMillis()
                    )
                    val dbMsgId = addMessageUseCase(initialMessage)

                    try {
                        val sendResult = if (state.channel == "WHATSAPP") {
                            val r = whatsAppSender.send(contact.phoneNumber, body)
                            r.success to r.error
                        } else {
                            val r = smsSender.send(contact.phoneNumber, body, dbMsgId)
                            r.success to r.error
                        }
                        val finalStatus = if (sendResult.first) "SENT" else "FAILED"
                        updateMessageStatusUseCase(dbMsgId, finalStatus)
                        if (!sendResult.first) {
                            errorLogger.log(
                                action = "send_${state.channel.lowercase()}_on_add",
                                message = "Initial ${state.channel} to '$trimmedName' failed",
                                detail = sendResult.second
                            )
                        }
                    } catch (sendError: Exception) {
                        updateMessageStatusUseCase(dbMsgId, "FAILED")
                        errorLogger.log(
                            action = "send_${state.channel.lowercase()}_on_add",
                            message = "Initial ${state.channel} to '$trimmedName' failed",
                            detail = sendError.message
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        success = true,
                        toastSuccess = "Contact '$savedName' saved!"
                    )
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "save_contact",
                    message = "Failed to save contact '$trimmedName'",
                    detail = e.message
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to save contact: ${e.message}",
                        toastError = "Failed: ${e.message}"
                    )
                }
            }
        }
        return true
    }
}
