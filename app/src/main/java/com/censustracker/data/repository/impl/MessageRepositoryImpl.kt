package com.censustracker.data.repository.impl

import com.censustracker.data.local.room.dao.MessageDao
import com.censustracker.data.local.room.entity.Message
import com.censustracker.domain.model.Message as DomainMessage
import com.censustracker.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepositoryImpl(
    private val dao: MessageDao
) : MessageRepository {

    override suspend fun addMessage(message: DomainMessage): Long {
        val entity = Message(
            id = message.id,
            contactId = message.contactId,
            phoneNumber = message.phoneNumber,
            direction = message.direction,
            channel = message.channel,
            body = message.body,
            status = message.status,
            sentAt = message.sentAt,
            receivedAt = message.receivedAt,
            smsMessageId = message.smsMessageId,
            isRead = message.isRead,
            createdAt = message.createdAt
        )
        return dao.insert(entity)
    }

    override suspend fun getMessage(id: Long): DomainMessage? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getMessagesByContact(contactId: Long): List<DomainMessage> {
        return dao.getByContact(contactId).map { it.toDomain() }
    }

    override fun watchMessagesByContact(contactId: Long): Flow<List<DomainMessage>> {
        return dao.watchByContact(contactId).map { it.map { it.toDomain() } }
    }

    override suspend fun updateMessage(message: DomainMessage): Int {
        val entity = Message(
            id = message.id,
            contactId = message.contactId,
            phoneNumber = message.phoneNumber,
            direction = message.direction,
            channel = message.channel,
            body = message.body,
            status = message.status,
            sentAt = message.sentAt,
            receivedAt = message.receivedAt,
            smsMessageId = message.smsMessageId,
            isRead = message.isRead,
            createdAt = message.createdAt
        )
        return dao.update(entity)
    }

    override suspend fun updateMessageStatus(id: Long, status: String): Int {
        return dao.updateStatus(id, status)
    }

    override suspend fun deleteMessagesByContact(contactId: Long): Int {
        return dao.deleteByContact(contactId)
    }

    override suspend fun failPendingMessages(): Int {
        return dao.failPendingMessages()
    }

    override fun watchAllMessages(): Flow<List<DomainMessage>> {
        return dao.watchAll().map { it.map { m -> m.toDomain() } }
    }

    override fun watchRetryableMessages(): Flow<List<DomainMessage>> {
        return dao.watchRetryable().map { it.map { m -> m.toDomain() } }
    }

    override suspend fun deleteMessage(id: Long): Int {
        return dao.delete(id)
    }

    override suspend fun markContactMessagesRead(contactId: Long): Int {
        return dao.markReceivedAsRead(contactId)
    }

    private fun Message.toDomain(): DomainMessage =
        DomainMessage(
            id = id,
            contactId = contactId,
            phoneNumber = phoneNumber,
            direction = direction,
            channel = channel,
            body = body,
            status = status,
            sentAt = sentAt,
            receivedAt = receivedAt,
            smsMessageId = smsMessageId,
            isRead = isRead,
            createdAt = createdAt
        )
}
