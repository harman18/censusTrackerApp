package com.censustracker.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index("contactId"), Index("phoneNumber")],
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val phoneNumber: String,
    val direction: String,
    val channel: String,
    val body: String,
    val status: String = "PENDING",
    val sentAt: Long? = null,
    val receivedAt: Long? = null,
    val smsMessageId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
