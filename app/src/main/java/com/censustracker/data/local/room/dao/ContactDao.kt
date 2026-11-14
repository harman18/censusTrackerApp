package com.censustracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.censustracker.data.local.room.entity.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg contacts: Contact): List<Long>

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    suspend fun getAll(): List<Contact>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): Contact?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber")
    suspend fun getByPhoneNumber(phoneNumber: String): Contact?

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    fun watchAll(): Flow<List<Contact>>

    @Update
    suspend fun update(contact: Contact): Int

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun delete(id: Long): Int
}
