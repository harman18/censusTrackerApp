package com.censustracker.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [Index("phoneNumber")],
    foreignKeys = []
)
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val houseNo: String? = null,
    val location: String? = null,
    val email: String? = null,
    val savedToPhone: Boolean = false,
    val channelPreference: String = "SMS",
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
