package com.censustracker.data.repository.impl

import com.censustracker.data.local.room.dao.CollectorProfileDao
import com.censustracker.data.local.room.entity.CollectorProfile
import com.censustracker.domain.model.CollectorProfile as DomainCollectorProfile
import com.censustracker.domain.repository.CollectorProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectorProfileRepositoryImpl(
    private val dao: CollectorProfileDao
) : CollectorProfileRepository {

    override suspend fun getProfile(): DomainCollectorProfile? {
        val entity = dao.getProfile()
        return entity?.toDomain()
    }

    override suspend fun saveProfile(profile: DomainCollectorProfile): Long {
        val entity = CollectorProfile(
            id = profile.id,
            name = profile.name,
            phoneNumber = profile.phoneNumber,
            email = profile.email,
            referenceLink = profile.referenceLink,
            createdAt = profile.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        return dao.insert(entity)
    }

    override fun watchProfile(): Flow<DomainCollectorProfile?> {
        return dao.watchProfile().map { it?.toDomain() }
    }

    private fun CollectorProfile.toDomain(): DomainCollectorProfile =
        DomainCollectorProfile(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            referenceLink = referenceLink,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
