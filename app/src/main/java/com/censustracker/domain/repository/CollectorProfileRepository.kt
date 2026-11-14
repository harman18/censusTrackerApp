package com.censustracker.domain.repository

import com.censustracker.domain.model.CollectorProfile
import com.censustracker.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface CollectorProfileRepository {
    suspend fun getProfile(): CollectorProfile?
    suspend fun saveProfile(profile: CollectorProfile): Long
    fun watchProfile(): Flow<CollectorProfile?>
}
