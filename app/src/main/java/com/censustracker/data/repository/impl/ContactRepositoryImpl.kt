package com.censustracker.data.repository.impl

import com.censustracker.data.local.room.dao.ContactDao
import com.censustracker.data.local.room.entity.Contact
import com.censustracker.domain.model.Contact as DomainContact
import com.censustracker.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val dao: ContactDao
) : ContactRepository {

    override suspend fun addContact(contact: DomainContact): Long {
        val entity = Contact(
            id = contact.id,
            name = contact.name,
            phoneNumber = contact.phoneNumber,
            houseNo = contact.houseNo,
            location = contact.location,
            email = contact.email,
            savedToPhone = contact.savedToPhone,
            channelPreference = contact.channelPreference,
            createdAt = contact.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        return dao.insert(entity)
    }

    override suspend fun getContact(id: Long): DomainContact? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getContactByPhone(phoneNumber: String): DomainContact? {
        return dao.getByPhoneNumber(phoneNumber)?.toDomain()
    }

    override suspend fun getAllContacts(): List<DomainContact> {
        return dao.getAll().map { it.toDomain() }
    }

    override fun watchAllContacts(): Flow<List<DomainContact>> {
        return dao.watchAll().map { it.map { it.toDomain() } }
    }

    override suspend fun updateContact(contact: DomainContact): Int {
        val entity = Contact(
            id = contact.id,
            name = contact.name,
            phoneNumber = contact.phoneNumber,
            houseNo = contact.houseNo,
            location = contact.location,
            email = contact.email,
            savedToPhone = contact.savedToPhone,
            channelPreference = contact.channelPreference,
            createdAt = contact.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        return dao.update(entity)
    }

    override suspend fun deleteContact(id: Long): Int {
        return dao.delete(id)
    }

    private fun Contact.toDomain(): DomainContact =
        DomainContact(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            houseNo = houseNo,
            location = location,
            email = email,
            savedToPhone = savedToPhone,
            channelPreference = channelPreference,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
