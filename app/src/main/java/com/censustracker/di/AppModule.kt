package com.censustracker.di

import android.app.Application
import android.content.Context
import com.censustracker.data.local.room.AppDatabase
import com.censustracker.data.json.JsonBackupService
import com.censustracker.data.repository.impl.CollectorProfileRepositoryImpl
import com.censustracker.data.repository.impl.ContactRepositoryImpl
import com.censustracker.data.repository.impl.MessageRepositoryImpl
import com.censustracker.data.repository.impl.PresetMessageRepositoryImpl
import com.censustracker.domain.repository.CollectorProfileRepository
import com.censustracker.domain.repository.ContactRepository
import com.censustracker.domain.repository.MessageRepository
import com.censustracker.domain.repository.PresetMessageRepository
import com.censustracker.logging.ErrorLogger
import com.censustracker.preferences.AppPreferences
import com.censustracker.sms.AndroidSmsSender
import com.censustracker.sms.SmsSender
import com.censustracker.whatsapp.AndroidWhatsAppSender
import com.censustracker.whatsapp.WhatsAppSender
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.encodeToString

@HiltAndroidApp
class AppModule : Application() {

    companion object {
        lateinit var messageRepository: MessageRepository
            private set

        lateinit var contactRepository: ContactRepository
            private set

        lateinit var collectorProfileRepository: CollectorProfileRepository
            private set

        lateinit var presetMessageRepository: PresetMessageRepository
            private set

        lateinit var smsSender: SmsSender
            private set

        lateinit var jsonBackupService: JsonBackupService
            private set

        lateinit var errorLogger: ErrorLogger
            private set

        lateinit var appPreferences: AppPreferences
            private set

        lateinit var whatsappSender: WhatsAppSender
            private set
    }

    override fun onCreate() {
        super.onCreate()

        val database = AppDatabase.getDatabase(this)

        val contactDao = database.contactDao()
        val messageDao = database.messageDao()
        val profileDao = database.collectorProfileDao()
        val presetDao = database.presetMessageDao()

        contactRepository = ContactRepositoryImpl(contactDao)
        messageRepository = MessageRepositoryImpl(messageDao)
        collectorProfileRepository = CollectorProfileRepositoryImpl(profileDao)
        presetMessageRepository = PresetMessageRepositoryImpl(presetDao)
        smsSender = AndroidSmsSender(this)
        jsonBackupService = JsonBackupService(this, database)
        errorLogger = ErrorLogger(this)
        appPreferences = AppPreferences(this)
        whatsappSender = AndroidWhatsAppSender(this)

        installCrashHandler()
    }

    /**
     * Persist uncaught crashes into the error log so beta testers can report
     * issues from Settings -> Error Log without needing logcat.
     */
    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val entry = com.censustracker.logging.ErrorLogEntry(
                    id = System.currentTimeMillis(),
                    timestamp = System.currentTimeMillis(),
                    action = "app_crash",
                    message = "${throwable.javaClass.simpleName}: ${throwable.message ?: "no message"}",
                    detail = throwable.stackTraceToString().take(4000)
                )
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                java.io.File(filesDir, "error_log.jsonl").appendText(
                    json.encodeToString(entry) + "\n"
                )
            } catch (_: Throwable) {
                // Never let the crash handler itself crash
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)

    @Provides
    @Singleton
    fun provideSmsSender(@ApplicationContext context: Context): SmsSender = AndroidSmsSender(context)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideContactRepository(database: AppDatabase): ContactRepository =
        ContactRepositoryImpl(database.contactDao())

    @Provides
    @Singleton
    fun provideMessageRepository(database: AppDatabase): MessageRepository =
        MessageRepositoryImpl(database.messageDao())

    @Provides
    @Singleton
    fun provideCollectorProfileRepository(database: AppDatabase): CollectorProfileRepository =
        CollectorProfileRepositoryImpl(database.collectorProfileDao())

    @Provides
    @Singleton
    fun providePresetMessageRepository(database: AppDatabase): PresetMessageRepository =
        PresetMessageRepositoryImpl(database.presetMessageDao())

    @Provides
    @Singleton
    fun provideJsonBackupService(@ApplicationContext context: Context, database: AppDatabase): JsonBackupService =
        JsonBackupService(context, database)

    @Provides
    @Singleton
    fun provideErrorLogger(@ApplicationContext context: Context): ErrorLogger =
        ErrorLogger(context)

    @Provides
    @Singleton
    fun provideWhatsAppSender(@ApplicationContext context: Context): WhatsAppSender =
        AndroidWhatsAppSender(context)
}
