package com.censustracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.censustracker.logging.ErrorLogger
import com.censustracker.logging.ErrorLogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ErrorLogViewModel @Inject constructor(
    private val errorLogger: ErrorLogger
) : ViewModel() {

    val entries: StateFlow<List<ErrorLogEntry>> = errorLogger.entries

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { errorLogger.refresh() }
    }

    fun clearAll() {
        viewModelScope.launch { errorLogger.clear() }
    }
}
