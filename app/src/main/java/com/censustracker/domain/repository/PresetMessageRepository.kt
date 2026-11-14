package com.censustracker.domain.repository

import com.censustracker.domain.model.PresetMessage
import kotlinx.coroutines.flow.Flow

interface PresetMessageRepository {
    suspend fun addPreset(preset: PresetMessage): Long
    suspend fun getPreset(id: Long): PresetMessage?
    suspend fun getAllPresets(): List<PresetMessage>
    fun watchAllPresets(): Flow<List<PresetMessage>>
    suspend fun updatePreset(preset: PresetMessage): Int
    suspend fun deletePreset(id: Long): Int
}
