package com.censustracker.domain.usecase

import com.censustracker.domain.model.PresetMessage
import com.censustracker.domain.repository.PresetMessageRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class AddPresetMessageUseCase @Inject constructor(
    private val repository: PresetMessageRepository
) {
    suspend operator fun invoke(preset: PresetMessage): Long = repository.addPreset(preset)
}

class GetAllPresetMessagesUseCase @Inject constructor(
    private val repository: PresetMessageRepository
) {
    suspend operator fun invoke(): List<PresetMessage> = repository.getAllPresets()
    fun watch(): Flow<List<PresetMessage>> = repository.watchAllPresets()
}

class DeletePresetMessageUseCase @Inject constructor(
    private val repository: PresetMessageRepository
) {
    suspend operator fun invoke(id: Long): Int = repository.deletePreset(id)
}
