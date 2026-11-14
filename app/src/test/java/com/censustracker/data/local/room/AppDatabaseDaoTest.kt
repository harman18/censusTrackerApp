package com.censustracker.data.local.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.censustracker.data.local.room.entity.Contact
import com.censustracker.data.local.room.entity.Message
import com.censustracker.data.local.room.entity.PresetMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun contact(phone: String = "+15550001111", name: String = "Respondent A") =
        Contact(name = name, phoneNumber = phone, houseNo = "12", location = "Ward 2")

    // ---------- ContactDao ----------

    @Test
    fun contactInsertAndQueryByPhone() = runTest {
        val id = database.contactDao().insert(contact())
        assertTrue(id > 0)

        val byPhone = database.contactDao().getByPhoneNumber("+15550001111")
        assertNotNull(byPhone)
        assertEquals("Respondent A", byPhone!!.name)
        assertEquals("12", byPhone.houseNo)
    }

    @Test
    fun contactWatchAllEmitsList() = runTest {
        database.contactDao().insert(contact(name = "First"))
        database.contactDao().insert(contact(phone = "+15550002222", name = "Second"))

        val all = database.contactDao().watchAll().first()
        assertEquals(2, all.size)
    }

    // ---------- MessageDao + status transitions ----------

    @Test
    fun messageStatusTransitions() = runTest {
        val contactId = database.contactDao().insert(contact())
        val msgId = database.messageDao().insert(
            Message(
                contactId = contactId,
                phoneNumber = "+15550001111",
                direction = "SENT",
                channel = "SMS",
                body = "Hello",
                status = "PENDING"
            )
        )

        database.messageDao().updateStatus(msgId, "SENT")
        assertEquals("SENT", database.messageDao().getById(msgId)!!.status)

        database.messageDao().updateStatus(msgId, "DELIVERED")
        assertEquals("DELIVERED", database.messageDao().getById(msgId)!!.status)
    }

    @Test
    fun messageFailedStatusAndRetryTransition() = runTest {
        val contactId = database.contactDao().insert(contact())
        val msgId = database.messageDao().insert(
            Message(
                contactId = contactId,
                phoneNumber = "+15550001111",
                direction = "SENT",
                channel = "SMS",
                body = "Retry me",
                status = "FAILED"
            )
        )

        // retry flow: FAILED -> PENDING -> SENT
        database.messageDao().updateStatus(msgId, "PENDING")
        assertEquals("PENDING", database.messageDao().getById(msgId)!!.status)
        database.messageDao().updateStatus(msgId, "SENT")
        assertEquals("SENT", database.messageDao().getById(msgId)!!.status)
    }

    @Test
    fun failPendingMessagesOnlyAffectsPending() = runTest {
        val contactId = database.contactDao().insert(contact())
        val pendingId = database.messageDao().insert(
            Message(contactId = contactId, phoneNumber = "+15550001111", direction = "SENT", channel = "SMS", body = "pending", status = "PENDING")
        )
        val sentId = database.messageDao().insert(
            Message(contactId = contactId, phoneNumber = "+15550001111", direction = "SENT", channel = "SMS", body = "sent", status = "SENT")
        )

        val updated = database.messageDao().failPendingMessages()
        assertEquals(1, updated)
        assertEquals("FAILED", database.messageDao().getById(pendingId)!!.status)
        assertEquals("SENT", database.messageDao().getById(sentId)!!.status)
    }

    @Test
    fun messagesOrderedByCreatedAtAscending() = runTest {
        val contactId = database.contactDao().insert(contact())
        val older = Message(contactId = contactId, phoneNumber = "+15550001111", direction = "SENT", channel = "SMS", body = "first", createdAt = 1000L)
        val newer = Message(contactId = contactId, phoneNumber = "+15550001111", direction = "RECEIVED", channel = "SMS", body = "second", createdAt = 2000L)
        database.messageDao().insert(newer)
        database.messageDao().insert(older)

        val messages = database.messageDao().getByContact(contactId)
        assertEquals(listOf("first", "second"), messages.map { it.body })
    }

    @Test
    fun deletingContactCascadesMessages() = runTest {
        val contactId = database.contactDao().insert(contact())
        database.messageDao().insert(
            Message(contactId = contactId, phoneNumber = "+15550001111", direction = "SENT", channel = "SMS", body = "bye")
        )
        assertEquals(1, database.messageDao().getByContact(contactId).size)

        database.contactDao().delete(contactId)
        assertTrue(database.messageDao().getByContact(contactId).isEmpty())
        assertNull(database.contactDao().getById(contactId))
    }

    @Test
    fun markReceivedAsReadOnlyAffectsUnreadIncomingForThatContact() = runTest {
        val contactA = database.contactDao().insert(contact(phone = "+15550001111", name = "A"))
        val contactB = database.contactDao().insert(contact(phone = "+15550002222", name = "B"))

        val unreadIncomingA = database.messageDao().insert(
            Message(contactId = contactA, phoneNumber = "+15550001111", direction = "RECEIVED", channel = "SMS", body = "hi A", status = "DELIVERED", isRead = false)
        )
        val sentA = database.messageDao().insert(
            Message(contactId = contactA, phoneNumber = "+15550001111", direction = "SENT", channel = "SMS", body = "out A", status = "SENT", isRead = false)
        )
        val unreadIncomingB = database.messageDao().insert(
            Message(contactId = contactB, phoneNumber = "+15550002222", direction = "RECEIVED", channel = "SMS", body = "hi B", status = "DELIVERED", isRead = false)
        )
        val readIncomingA = database.messageDao().insert(
            Message(contactId = contactA, phoneNumber = "+15550001111", direction = "RECEIVED", channel = "SMS", body = "old", status = "DELIVERED", isRead = true)
        )

        val updated = database.messageDao().markReceivedAsRead(contactA)
        assertEquals(1, updated)

        assertTrue(database.messageDao().getById(unreadIncomingA)!!.isRead)
        assertTrue(!database.messageDao().getById(sentA)!!.isRead)
        assertTrue(!database.messageDao().getById(unreadIncomingB)!!.isRead)
        assertTrue(database.messageDao().getById(readIncomingA)!!.isRead)
    }

    @Test
    fun watchRetryableIncludesPendingAndAllFailedVariants() = runTest {
        val contactId = database.contactDao().insert(contact())
        fun msg(body: String, status: String) = Message(
            contactId = contactId, phoneNumber = "+15550001111",
            direction = "SENT", channel = "SMS", body = body, status = status
        )
        database.messageDao().insert(msg("pending", "PENDING"))
        database.messageDao().insert(msg("failed", "FAILED"))
        database.messageDao().insert(msg("no service", "FAILED_NO_SERVICE"))
        database.messageDao().insert(msg("airplane", "FAILED_AIRPLANE_MODE"))
        database.messageDao().insert(msg("no pdu", "FAILED_NO_PDU"))
        database.messageDao().insert(msg("sent", "SENT"))
        database.messageDao().insert(msg("delivered", "DELIVERED"))
        database.messageDao().insert(
            Message(contactId = contactId, phoneNumber = "+15550001111", direction = "RECEIVED", channel = "SMS", body = "incoming", status = "DELIVERED")
        )

        val retryable = database.messageDao().watchRetryable().first()
        assertEquals(5, retryable.size)
        assertTrue(retryable.all { it.status == "PENDING" || it.status.startsWith("FAILED") })
    }

    @Test
    fun deleteMessageRemovesSingleRow() = runTest {
        val contactId = database.contactDao().insert(contact())
        val id1 = database.messageDao().insert(
            Message(contactId = contactId, phoneNumber = "+15550001111", direction = "SENT", channel = "SMS", body = "one", status = "FAILED")
        )
        database.messageDao().insert(
            Message(contactId = contactId, phoneNumber = "+15550001111", direction = "SENT", channel = "SMS", body = "two", status = "SENT")
        )

        assertEquals(1, database.messageDao().delete(id1))
        assertNull(database.messageDao().getById(id1))
        assertEquals(1, database.messageDao().getByContact(contactId).size)
    }

    // ---------- PresetMessageDao ----------

    @Test
    fun presetCrud() = runTest {
        val id = database.presetMessageDao().insert(
            PresetMessage(title = "Reminder", bodyTemplate = "Hi {respondent.name}", dynamicVariables = "{respondent.name}")
        )
        assertEquals(1, database.presetMessageDao().getAll().size)

        val preset = database.presetMessageDao().getById(id)!!
        database.presetMessageDao().update(preset.copy(title = "Reminder v2"))
        assertEquals("Reminder v2", database.presetMessageDao().getById(id)!!.title)

        database.presetMessageDao().delete(id)
        assertTrue(database.presetMessageDao().getAll().isEmpty())
    }

    // ---------- CollectorProfileDao ----------

    @Test
    fun collectorProfileSingleRowUpsert() = runTest {
        assertNull(database.collectorProfileDao().getProfile())

        database.collectorProfileDao().insert(
            com.censustracker.data.local.room.entity.CollectorProfile(
                name = "Collector", phoneNumber = "+10000000000"
            )
        )
        val profile = database.collectorProfileDao().getProfile()
        assertNotNull(profile)
        assertEquals("Collector", profile!!.name)

        database.collectorProfileDao().insert(
            com.censustracker.data.local.room.entity.CollectorProfile(
                id = 1, name = "Collector v2", phoneNumber = "+10000000000"
            )
        )
        assertEquals("Collector v2", database.collectorProfileDao().getProfile()!!.name)
    }
}
