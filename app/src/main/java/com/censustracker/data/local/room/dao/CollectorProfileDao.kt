package com.censustracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.censustracker.data.local.room.entity.CollectorProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectorProfileDao {

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CollectorProfile): Long

    @Query("SELECT * FROM collector_profile WHERE id = 1")
    suspend fun getProfile(): CollectorProfile?

    @Query("SELECT * FROM collector_profile WHERE id = 1")
    fun watchProfile(): Flow<CollectorProfile?>

    @Update
    suspend fun update(profile: CollectorProfile): Int
}
