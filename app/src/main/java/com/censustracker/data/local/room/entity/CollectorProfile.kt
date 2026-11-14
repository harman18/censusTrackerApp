package com.censustracker.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "collector_profile")
data class CollectorProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val referenceLink: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
