package com.censustracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.censustracker.domain.model.PresetMessage
import com.censustracker.domain.usecase.AddPresetMessageUseCase
import com.censustracker.domain.usecase.DeletePresetMessageUseCase
import com.censustracker.domain.usecase.GetAllPresetMessagesUseCase
import com.censustracker.logging.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PresetMessageViewModel @Inject constructor(
    private val addPresetMessageUseCase: AddPresetMessageUseCase,
    private val getAllPresetMessagesUseCase: GetAllPresetMessagesUseCase,
    private val deletePresetMessageUseCase: DeletePresetMessageUseCase,
    private val errorLogger: ErrorLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val presets: List<PresetMessage> = emptyList(),
        val title: String = "",
        val bodyTemplate: String = "",
        val titleError: String? = null,
        val bodyError: String? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: Boolean = false
    )

    init {
        loadPresetsAndAddDefaults()
    }

    fun loadPresetsAndAddDefaults() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
            val presets = getAllPresetMessagesUseCase()
            if (presets.isEmpty()) {
                // Default template for Phase 1 Census self-enumeration
                val default1 = PresetMessage(
                    title = "Census Self-Enumeration",
                    bodyTemplate = "Census Self-Enumeration details\n" +
                            "Contact Details : {respondent.name} {respondent.phone}\n" +
                            "Go through youtube video on how to do self-enumerate\n" +
                            "{collector.link}\n" +
                            "Please send the self enumeration id back on whatsapp once filled.",
                    dynamicVariables = listOf(
                        "{respondent.name}", "{respondent.phone}", "{collector.link}"
                    )
                )
                addPresetMessageUseCase(default1)
                val reloaded = getAllPresetMessagesUseCase()
                _uiState.update { it.copy(presets = reloaded, isLoading = false) }
            } else {
                _uiState.update { it.copy(presets = presets, isLoading = false) }
            }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "load_presets",
                    message = "Failed to load preset messages",
                    detail = e.message
                )
                _uiState.update { it.copy(isLoading = false, error = "Load failed: ${e.message}") }
            }
        }
    }

    fun onTitleChanged(text: String) {
        _uiState.update {
            it.copy(
                title = text,
                titleError = if (text.isNotBlank() && text.trim().length < 2) "Title must be at least 2 characters" else null,
                error = null
            )
        }
    }

    fun onBodyTemplateChanged(text: String) {
        _uiState.update {
            it.copy(
                bodyTemplate = text,
                bodyError = if (text.isNotBlank() && text.trim().length < 5) "Body template must be at least 5 characters" else null,
                error = null
            )
        }
    }

    fun onInsertPlaceholder(placeholder: String) {
        _uiState.update {
            it.copy(bodyTemplate = it.bodyTemplate + " " + placeholder)
        }
    }

    fun addPreset(): Boolean {
        val state = _uiState.value
        val titleTrim = state.title.trim()
        val bodyTrim = state.bodyTemplate.trim()

        val titleErr = when {
            titleTrim.isBlank() -> "Title is required"
            titleTrim.length < 2 -> "Title must be at least 2 characters"
            else -> null
        }

        val bodyErr = when {
            bodyTrim.isBlank() -> "Body template is required"
            bodyTrim.length < 5 -> "Body template must be at least 5 characters"
            else -> null
        }

        if (titleErr != null || bodyErr != null) {
            _uiState.update {
                it.copy(
                    titleError = titleErr,
                    bodyError = bodyErr,
                    error = "Please fix errors above"
                )
            }
            return false
        }

        val variables = com.censustracker.placeholder.PlaceholderResolver.availablePlaceholders()
            .filter { bodyTrim.contains(it) }

        _uiState.update { it.copy(isLoading = true, error = null, titleError = null, bodyError = null, success = false) }
        viewModelScope.launch {
            try {
                val preset = PresetMessage(
                    title = titleTrim,
                    bodyTemplate = bodyTrim,
                    dynamicVariables = variables
                )
                addPresetMessageUseCase(preset)
                val updated = getAllPresetMessagesUseCase()
                _uiState.update {
                    it.copy(
                        presets = updated,
                        isLoading = false,
                        success = true,
                        title = "",
                        bodyTemplate = ""
                    )
                }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "save_preset",
                    message = "Failed to save preset '$titleTrim'",
                    detail = e.message
                )
                _uiState.update {
                    it.copy(isLoading = false, error = "Save failed: ${e.message}")
                }
            }
        }
        return true
    }

    fun onDeletePreset(id: Long) {
        viewModelScope.launch {
            try {
                deletePresetMessageUseCase(id)
                val updated = getAllPresetMessagesUseCase()
                _uiState.update { it.copy(presets = updated) }
            } catch (e: Exception) {
                errorLogger.log(
                    action = "delete_preset",
                    message = "Failed to delete preset $id",
                    detail = e.message
                )
                _uiState.update { it.copy(error = "Delete failed: ${e.message}") }
            }
        }
    }
}