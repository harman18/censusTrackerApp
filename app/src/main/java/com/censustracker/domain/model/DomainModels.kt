package com.censustracker.domain.model

data class CollectorProfile(
    val id: Int = 1,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val referenceLink: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

data class Contact(
    val id: Long = 0,
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

data class Message(
    val id: Long = 0,
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

/** Per-contact messaging summary used by the contact list (last message + unread count). */
data class ContactActivity(
    val contactId: Long,
    val lastBody: String,
    val lastAt: Long,
    val lastDirection: String,
    val unreadCount: Int
)

data class PresetMessage(
    val id: Long = 0,
    val title: String,
    val bodyTemplate: String,
    val dynamicVariables: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class MessageDirection {
    SENT, RECEIVED
}

enum class MessageChannel {
    SMS, WHATSAPP
}

enum class MessageStatus {
    PENDING, SENT, DELIVERED, FAILED
}
