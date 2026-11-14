package com.censustracker.domain.repository

import com.censustracker.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun addMessage(message: Message): Long
    suspend fun getMessage(id: Long): Message?
    suspend fun getMessagesByContact(contactId: Long): List<Message>
    fun watchMessagesByContact(contactId: Long): Flow<List<Message>>
    suspend fun updateMessage(message: Message): Int
    suspend fun updateMessageStatus(id: Long, status: String): Int
    suspend fun deleteMessagesByContact(contactId: Long): Int
    suspend fun failPendingMessages(): Int
    fun watchAllMessages(): Flow<List<Message>>
    fun watchRetryableMessages(): Flow<List<Message>>
    suspend fun deleteMessage(id: Long): Int
    suspend fun markContactMessagesRead(contactId: Long): Int
}
