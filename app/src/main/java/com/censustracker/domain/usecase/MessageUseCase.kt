package com.censustracker.domain.usecase

import com.censustracker.domain.model.Message
import com.censustracker.domain.repository.MessageRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class AddMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(message: Message): Long = repository.addMessage(message)
}

class GetMessagesByContactUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(contactId: Long): List<Message> = repository.getMessagesByContact(contactId)
    fun watch(contactId: Long): Flow<List<Message>> = repository.watchMessagesByContact(contactId)
}

class UpdateMessageStatusUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(id: Long, status: String): Int = repository.updateMessageStatus(id, status)
}

class WatchAllMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    fun watchAll(): Flow<List<Message>> = repository.watchAllMessages()
}

class WatchRetryableMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    fun watch(): Flow<List<Message>> = repository.watchRetryableMessages()
}

class DeleteMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(id: Long): Int = repository.deleteMessage(id)
}

class MarkContactMessagesReadUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(contactId: Long): Int = repository.markContactMessagesRead(contactId)
}
