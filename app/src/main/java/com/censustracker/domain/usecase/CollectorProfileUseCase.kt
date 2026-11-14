package com.censustracker.domain.usecase

import com.censustracker.domain.model.CollectorProfile
import com.censustracker.domain.repository.CollectorProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetCollectorProfileUseCase @Inject constructor(
    private val repository: CollectorProfileRepository
) {
    suspend operator fun invoke(): CollectorProfile? = repository.getProfile()
    fun watch(): Flow<CollectorProfile?> = repository.watchProfile()
}

class SaveCollectorProfileUseCase @Inject constructor(
    private val repository: CollectorProfileRepository
) {
    suspend operator fun invoke(profile: CollectorProfile): Long = repository.saveProfile(profile)
}