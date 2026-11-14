package com.censustracker.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.censustracker.data.local.room.dao.CollectorProfileDao
import com.censustracker.data.local.room.dao.ContactDao
import com.censustracker.data.local.room.dao.MessageDao
import com.censustracker.data.local.room.dao.PresetMessageDao
import com.censustracker.data.local.room.entity.CollectorProfile
import com.censustracker.data.local.room.entity.Contact
import com.censustracker.data.local.room.entity.Message
import com.censustracker.data.local.room.entity.PresetMessage
import com.censustracker.data.local.room.typeconverters.Converters

@Database(
    entities = [CollectorProfile::class, Contact::class, Message::class, PresetMessage::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectorProfileDao(): CollectorProfileDao
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun presetMessageDao(): PresetMessageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "census_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN isRead INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
