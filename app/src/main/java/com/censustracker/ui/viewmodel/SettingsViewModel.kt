package com.censustracker.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.censustracker.data.json.JsonBackupService
import com.censustracker.domain.model.CollectorProfile
import com.censustracker.domain.usecase.GetCollectorProfileUseCase
import com.censustracker.logging.ErrorLogger
import com.censustracker.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val getCollectorProfileUseCase: GetCollectorProfileUseCase,
    private val jsonBackupService: JsonBackupService,
    private val errorLogger: ErrorLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val collectorProfile: CollectorProfile? = null,
        val defaultSaveToPhone: Boolean = true,
        val defaultSendNow: Boolean = false,
        val defaultCountryCode: String = "+91",
        val smsEnabled: Boolean = true,
        val whatsappEnabled: Boolean = true,
        val isProcessing: Boolean = false,
        val processingTitle: String = "",
        val toastSuccess: String? = null,
        val toastError: String? = null
    )

    init {
        _uiState.update {
            it.copy(
                defaultSaveToPhone = appPreferences.defaultSaveToPhone,
                defaultSendNow = appPreferences.defaultSendNow,
                defaultCountryCode = appPreferences.defaultCountryCode,
                smsEnabled = appPreferences.smsEnabled,
                whatsappEnabled = appPreferences.whatsappEnabled
            )
        }
        viewModelScope.launch {
            try {
                val profile = getCollectorProfileUseCase()
                _uiState.update { it.copy(collectorProfile = profile) }
            } catch (e: Exception) {
                errorLogger.log("load_settings", "Failed to load collector profile", e.message)
            }
        }
    }

    fun onSaveToPhoneDefaultChanged(checked: Boolean) {
        appPreferences.defaultSaveToPhone = checked
        _uiState.update { it.copy(defaultSaveToPhone = checked) }
    }

    fun onSendNowDefaultChanged(checked: Boolean) {
        appPreferences.defaultSendNow = checked
        _uiState.update { it.copy(defaultSendNow = checked) }
    }

    fun onDefaultCountryCodeChanged(text: String) {
        val cleaned = text.trim().let { if (it.startsWith("+")) it else "+$it" }
            .filter { it == '+' || it.isDigit() }
            .ifBlank { "+91" }
        appPreferences.defaultCountryCode = cleaned
        _uiState.update { it.copy(defaultCountryCode = cleaned) }
    }

    fun onSmsEnabledChanged(checked: Boolean) {
        appPreferences.smsEnabled = checked
        _uiState.update { it.copy(smsEnabled = checked) }
    }

    fun onWhatsappEnabledChanged(checked: Boolean) {
        appPreferences.whatsappEnabled = checked
        _uiState.update { it.copy(whatsappEnabled = checked) }
    }

    fun exportBackup(destination: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingTitle = "Exporting Backup...") }
            try {
                jsonBackupService.exportToUri(destination)
                _uiState.update { it.copy(toastSuccess = "Backup exported successfully") }
            } catch (e: Exception) {
                errorLogger.log("export_backup", "Backup export failed", e.message)
                _uiState.update { it.copy(toastError = "Export failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun importBackup(source: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingTitle = "Importing Registry...") }
            try {
                val summary = jsonBackupService.importFromUri(source)
                _uiState.update {
                    it.copy(
                        toastSuccess = "Import complete: ${summary.contactsAdded} added, " +
                                "${summary.contactsUpdated} updated, ${summary.messagesImported} messages"
                    )
                }
            } catch (e: Exception) {
                errorLogger.log("import_backup", "Backup import failed", e.message)
                _uiState.update { it.copy(toastError = "Import failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastSuccess = null, toastError = null) }
    }
}
