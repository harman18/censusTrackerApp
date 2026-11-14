package com.censustracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.censustracker.domain.model.CollectorProfile
import com.censustracker.domain.usecase.GetCollectorProfileUseCase
import com.censustracker.domain.usecase.SaveCollectorProfileUseCase
import com.censustracker.logging.ErrorLogger
import com.censustracker.preferences.AppPreferences
import com.censustracker.util.PhoneNumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CollectorSetupViewModel @Inject constructor(
    private val saveCollectorProfileUseCase: SaveCollectorProfileUseCase,
    private val getCollectorProfileUseCase: GetCollectorProfileUseCase,
    private val appPreferences: AppPreferences,
    private val errorLogger: ErrorLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val name: String = "",
        val phoneNumber: String = "",
        val countryCode: String = "+91",
        val email: String = "",
        val referenceLink: String = "",
        val nameError: String? = null,
        val phoneError: String? = null,
        val emailError: String? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val isProfileComplete: Boolean = false,
        val existingProfile: CollectorProfile? = null
    )

    init {
        loadExistingProfile()
    }

    fun loadExistingProfile() {
        viewModelScope.launch {
            try {
            val profile = getCollectorProfileUseCase()
            val national = profile?.phoneNumber?.filter { it.isDigit() }?.let {
                // Strip the default country code digits if present so the field shows the national part
                val cc = appPreferences.defaultCountryCode.filter { c -> c.isDigit() }
                if (cc.isNotEmpty() && it.startsWith(cc) && it.length > cc.length + 6) it.substring(cc.length) else it
            } ?: ""
            if (profile != null) {
                _uiState.update {
                    it.copy(
                        name = profile.name,
                        phoneNumber = national,
                        email = profile.email ?: "",
                        referenceLink = profile.referenceLink ?: "",
                        existingProfile = profile,
                        isProfileComplete = true,
                        countryCode = appPreferences.defaultCountryCode
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        existingProfile = null,
                        isProfileComplete = false,
                        countryCode = appPreferences.defaultCountryCode
                    )
                }
            }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "load_collector_profile",
                    message = "Failed to load collector profile",
                    detail = e.message
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

    fun onCountryCodeChanged(text: String) {
        val cleaned = text.trim().let { if (it.startsWith("+")) it else "+$it" }
            .filter { it == '+' || it.isDigit() }
            .ifBlank { "+91" }
        appPreferences.defaultCountryCode = cleaned
        _uiState.update { it.copy(countryCode = cleaned) }
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

    fun onReferenceLinkChanged(text: String) {
        _uiState.update { it.copy(referenceLink = text) }
    }

    fun saveProfile(): Boolean {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val trimmedPhone = PhoneNumberFormatter.normalize(state.phoneNumber.trim(), state.countryCode)
        val trimmedEmail = state.email.trim()

        val nameErr = when {
            trimmedName.isBlank() -> "Collector name is required"
            trimmedName.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }

        val phoneErr = when {
            state.phoneNumber.isBlank() -> "Collector phone number is required"
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
                    error = "Please fix the validation errors above"
                )
            }
            return false
        }

        _uiState.update { it.copy(isLoading = true, error = null, nameError = null, phoneError = null, emailError = null) }
        viewModelScope.launch {
            try {
                val profile = CollectorProfile(
                    id = state.existingProfile?.id ?: 1,
                    name = trimmedName,
                    phoneNumber = trimmedPhone,
                    email = trimmedEmail.takeIf { it.isNotBlank() },
                    referenceLink = state.referenceLink.trim().takeIf { it.isNotBlank() }
                )
                saveCollectorProfileUseCase(profile)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isProfileComplete = true,
                        existingProfile = profile
                    )
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "save_collector_profile",
                    message = "Failed to save collector profile",
                    detail = e.message
                )
                _uiState.update {
                    it.copy(isLoading = false, error = "Save failed: ${e.message}")
                }
            }
        }
        return true
    }
}