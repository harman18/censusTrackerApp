package com.censustracker.domain.usecase

import com.censustracker.domain.model.Contact
import com.censustracker.domain.repository.ContactRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class AddContactUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(contact: Contact): Long = repository.addContact(contact)
}

class GetContactUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(id: Long): Contact? = repository.getContact(id)
    suspend operator fun invoke(phone: String): Contact? = repository.getContactByPhone(phone)
}

class GetAllContactsUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(): List<Contact> = repository.getAllContacts()
    fun watch(): Flow<List<Contact>> = repository.watchAllContacts()
}

class UpdateContactUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(contact: Contact): Int = repository.updateContact(contact)
}

class DeleteContactUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(id: Long): Int = repository.deleteContact(id)
}