package com.censustracker.domain.repository

import com.censustracker.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    suspend fun addContact(contact: Contact): Long
    suspend fun getContact(id: Long): Contact?
    suspend fun getContactByPhone(phoneNumber: String): Contact?
    suspend fun getAllContacts(): List<Contact>
    fun watchAllContacts(): Flow<List<Contact>>
    suspend fun updateContact(contact: Contact): Int
    suspend fun deleteContact(id: Long): Int
}
