package com.censustracker.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preset_messages")
data class PresetMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val bodyTemplate: String,
    val dynamicVariables: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)
