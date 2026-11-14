package com.censustracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.censustracker.data.local.room.entity.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg messages: Message): List<Long>

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY createdAt ASC")
    suspend fun getByContact(contactId: Long): List<Message>

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY createdAt ASC")
    fun watchByContact(contactId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    suspend fun getAll(): List<Message>

    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    fun watchAll(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE status = 'PENDING' OR status LIKE 'FAILED%' ORDER BY createdAt DESC")
    fun watchRetryable(): Flow<List<Message>>

    @Query("UPDATE messages SET isRead = 1 WHERE contactId = :contactId AND direction = 'RECEIVED' AND isRead = 0")
    suspend fun markReceivedAsRead(contactId: Long): Int

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): Message?

    @Update
    suspend fun update(message: Message): Int

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String): Int

    @Query("DELETE FROM messages WHERE contactId = :contactId")
    suspend fun deleteByContact(contactId: Long): Int

    @Query("UPDATE messages SET status = 'FAILED' WHERE status = 'PENDING'")
    suspend fun failPendingMessages(): Int
}
