package com.censustracker.logging

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistent, file-backed error log. Every failed action in the app records an
 * entry (timestamp + action + short reason + optional detail) so issues can be
 * reviewed later from the Error Log screen for debugging.
 */
@Singleton
class ErrorLogger @Inject constructor(
    @ApplicationContext context: Context
) {

    private val file = File(context.filesDir, "error_log.jsonl")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _entries = MutableStateFlow<List<ErrorLogEntry>>(emptyList())
    val entries: StateFlow<List<ErrorLogEntry>> = _entries.asStateFlow()

    /** Record a failed action. Safe to call from any coroutine. */
    suspend fun log(action: String, message: String, detail: String? = null) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val entry = ErrorLogEntry(
                    id = System.currentTimeMillis(),
                    timestamp = System.currentTimeMillis(),
                    action = action,
                    message = message,
                    detail = detail
                )
                file.appendText(json.encodeToString(entry) + "\n")
                pruneIfNeeded()
                _entries.value = loadEntries()
            }
        }
    }

    /** Fire-and-forget variant for call sites that are not already in a coroutine. */
    fun logAsync(action: String, message: String, detail: String? = null) {
        CoroutineScope(Dispatchers.IO).launch { log(action, message, detail) }
    }

    /** Reload entries from disk into the StateFlow. */
    suspend fun refresh() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                _entries.value = loadEntries()
            }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (file.exists()) file.delete()
                _entries.value = emptyList()
            }
        }
    }

    private fun loadEntries(): List<ErrorLogEntry> {
        if (!file.exists()) return emptyList()
        return file.readLines()
            .mapNotNull { line ->
                try {
                    json.decodeFromString<ErrorLogEntry>(line)
                } catch (_: Exception) {
                    null
                }
            }
            .sortedByDescending { it.timestamp }
    }

    private fun pruneIfNeeded() {
        if (!file.exists()) return
        val lines = file.readLines()
        if (lines.size > MAX_ENTRIES) {
            file.writeText(lines.takeLast(MAX_ENTRIES).joinToString("\n") + "\n")
        }
    }

    companion object {
        private const val MAX_ENTRIES = 500
    }
}
