package com.censustracker.data.json

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.censustracker.data.local.room.AppDatabase
import com.censustracker.data.local.room.entity.Contact
import com.censustracker.data.local.room.entity.Message
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JsonBackupServiceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var service: JsonBackupService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        service = JsonBackupService(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedData() {
        val contactId = database.contactDao().insert(
            Contact(name = "Alice", phoneNumber = "+15551112222", houseNo = "7")
        )
        database.messageDao().insert(
            Message(
                contactId = contactId,
                phoneNumber = "+15551112222",
                direction = "SENT",
                channel = "SMS",
                body = "Survey notice",
                status = "DELIVERED",
                sentAt = 1700000000000L,
                createdAt = 1700000000000L
            )
        )
        database.presetMessageDao().insert(
            com.censustracker.data.local.room.entity.PresetMessage(
                title = "Reminder", bodyTemplate = "Hi {respondent.name}"
            )
        )
    }

    @Test
    fun exportThenImportRoundTrip() = runTest {
        seedData()
        val file = service.exportToJson()
        assertTrue(file.exists() && file.length() > 0)

        // Wipe tables to simulate a fresh install, then import
        database.contactDao().getAll().forEach { database.contactDao().delete(it.id) }
        database.messageDao().getAll().forEach { database.messageDao().deleteByContact(it.contactId) }
        database.presetMessageDao().getAll().forEach { database.presetMessageDao().delete(it.id) }
        assertTrue(database.contactDao().getAll().isEmpty())

        val summary = service.importFromJson(file)
        assertEquals(1, summary.contactsAdded)
        assertEquals(1, summary.messagesImported)
        assertEquals(1, summary.presetsImported)
        assertEquals(1, database.contactDao().getAll().size)
        assertEquals("Alice", database.contactDao().getAll()[0].name)
    }

    @Test
    fun reimportIsIdempotent() = runTest {
        seedData()
        val file = service.exportToJson()

        val first = service.importFromJson(file)
        assertEquals(0, first.contactsAdded) // phone already exists -> update path
        assertEquals(1, first.contactsUpdated)
        assertEquals(0, first.messagesImported) // duplicate skipped
        assertEquals(1, first.messagesSkipped)

        val second = service.importFromJson(file)
        assertEquals(0, second.contactsAdded)
        assertEquals(0, second.messagesImported)

        // Still exactly one contact and one message
        assertEquals(1, database.contactDao().getAll().size)
        val contact = database.contactDao().getAll()[0]
        assertEquals(1, database.messageDao().getByContact(contact.id).size)
    }

    @Test
    fun importMatchesContactByPhoneAcrossFormats() = runTest {
        // Local contact stored with formatting; backup has plain digits
        val localId = database.contactDao().insert(
            Contact(name = "Local Alice", phoneNumber = "+1 (555) 111-2222")
        )
        val backupJson = """
        {
          "format": "com.censustracker.backup",
          "formatVersion": 1,
          "collectorProfile": null,
          "contacts": [
            {"id": 99, "name": "Alice Updated", "phoneNumber": "15551112222", "houseNo": "9", "savedToPhone": false, "channelPreference": "SMS", "createdAt": 1, "updatedAt": 1}
          ],
          "messages": [],
          "presets": []
        }
        """.trimIndent()

        val summary = service.importFromJsonString(backupJson)
        assertEquals(0, summary.contactsAdded)
        assertEquals(1, summary.contactsUpdated)

        val contacts = database.contactDao().getAll()
        assertEquals(1, contacts.size)
        assertEquals(localId, contacts[0].id)
        assertEquals("Alice Updated", contacts[0].name)
        assertEquals("9", contacts[0].houseNo)
    }

    @Test
    fun importSkipsMessagesWithUnknownContact() = runTest {
        val backupJson = """
        {
          "format": "com.censustracker.backup",
          "formatVersion": 1,
          "collectorProfile": null,
          "contacts": [],
          "messages": [
            {"id": 1, "contactId": 555, "phoneNumber": "+19998887777", "direction": "SENT", "channel": "SMS", "body": "orphan", "status": "SENT", "createdAt": 1}
          ],
          "presets": []
        }
        """.trimIndent()

        val summary = service.importFromJsonString(backupJson)
        assertEquals(0, summary.messagesImported)
        assertTrue(database.messageDao().getAll().isEmpty())
    }

    @Test
    fun importRejectsNonAppJson() = runTest {
        val foreignJson = """{"someOtherApp": true, "contacts": []}"""
        try {
            service.importFromJsonString(foreignJson)
            org.junit.Assert.fail("Expected import to reject a non-Census-Tracker file")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("not a Census Tracker backup"))
        }
        assertTrue(database.contactDao().getAll().isEmpty())
    }

    @Test
    fun exportContainsAppFormatMarker() = runTest {
        seedData()
        val file = service.exportToJson()
        val content = file.readText()
        assertTrue(content.contains("\"format\": \"com.censustracker.backup\""))
        assertTrue(content.contains("\"app\": \"Census Tracker\""))
    }
}
