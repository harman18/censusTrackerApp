package com.censustracker.data.repository.impl

import com.censustracker.data.local.room.dao.PresetMessageDao
import com.censustracker.data.local.room.entity.PresetMessage
import com.censustracker.domain.model.PresetMessage as DomainPresetMessage
import com.censustracker.domain.repository.PresetMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PresetMessageRepositoryImpl(
    private val dao: PresetMessageDao
) : PresetMessageRepository {

    override suspend fun addPreset(preset: DomainPresetMessage): Long {
        val entity = PresetMessage(
            id = preset.id,
            title = preset.title,
            bodyTemplate = preset.bodyTemplate,
            dynamicVariables = preset.dynamicVariables.joinToString(","),
            createdAt = preset.createdAt
        )
        return dao.insert(entity)
    }

    override suspend fun getPreset(id: Long): DomainPresetMessage? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getAllPresets(): List<DomainPresetMessage> {
        return dao.getAll().map { it.toDomain() }
    }

    override fun watchAllPresets(): Flow<List<DomainPresetMessage>> {
        return dao.watchAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun updatePreset(preset: DomainPresetMessage): Int {
        val entity = PresetMessage(
            id = preset.id,
            title = preset.title,
            bodyTemplate = preset.bodyTemplate,
            dynamicVariables = preset.dynamicVariables.joinToString(","),
            createdAt = preset.createdAt
        )
        return dao.update(entity)
    }

    override suspend fun deletePreset(id: Long): Int {
        return dao.delete(id)
    }

    private fun PresetMessage.toDomain(): DomainPresetMessage {
        val variables = if (dynamicVariables.isNotBlank()) {
            dynamicVariables.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }
        return DomainPresetMessage(
            id = id,
            title = title,
            bodyTemplate = bodyTemplate,
            dynamicVariables = variables,
            createdAt = createdAt
        )
    }
}