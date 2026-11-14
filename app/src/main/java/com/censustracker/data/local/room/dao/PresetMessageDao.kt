package com.censustracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.censustracker.data.local.room.entity.PresetMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: PresetMessage): Long

    @Query("SELECT * FROM preset_messages ORDER BY createdAt DESC")
    suspend fun getAll(): List<PresetMessage>

    @Query("SELECT * FROM preset_messages ORDER BY createdAt DESC")
    fun watchAll(): Flow<List<PresetMessage>>

    @Query("SELECT * FROM preset_messages WHERE id = :id")
    suspend fun getById(id: Long): PresetMessage?

    @Update
    suspend fun update(preset: PresetMessage): Int

    @Query("DELETE FROM preset_messages WHERE id = :id")
    suspend fun delete(id: Long): Int
}
