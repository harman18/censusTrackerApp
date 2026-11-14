package com.censustracker.data.repository.impl

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.censustracker.data.local.room.AppDatabase
import com.censustracker.domain.model.Contact as DomainContact
import com.censustracker.domain.model.Message as DomainMessage
import com.censustracker.domain.model.PresetMessage as DomainPresetMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryMapperTest {

    private lateinit var database: AppDatabase
    private lateinit var contactRepository: ContactRepositoryImpl
    private lateinit var messageRepository: MessageRepositoryImpl
    private lateinit var presetRepository: PresetMessageRepositoryImpl
    private lateinit var profileRepository: CollectorProfileRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contactRepository = ContactRepositoryImpl(database.contactDao())
        messageRepository = MessageRepositoryImpl(database.messageDao())
        presetRepository = PresetMessageRepositoryImpl(database.presetMessageDao())
        profileRepository = CollectorProfileRepositoryImpl(database.collectorProfileDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun contactRepositoryRoundTrip() = runTest {
        val domain = DomainContact(
            name = "Jane Doe",
            phoneNumber = "+15551234567",
            houseNo = "42-B",
            location = "Green Valley",
            email = "jane@example.com",
            savedToPhone = true,
            channelPreference = "SMS"
        )
        val id = contactRepository.addContact(domain)
        assertNotNull(id.takeIf { it > 0 })

        val fetched = contactRepository.getContact(id)
        assertNotNull(fetched)
        assertEquals("Jane Doe", fetched!!.name)
        assertEquals("42-B", fetched.houseNo)
        assertEquals("Green Valley", fetched.location)
        assertEquals("jane@example.com", fetched.email)
        assertEquals(true, fetched.savedToPhone)

        val byPhone = contactRepository.getContactByPhone("+15551234567")
        assertEquals(id, byPhone?.id)

        val updated = fetched.copy(name = "Jane Smith")
        contactRepository.updateContact(updated)
        assertEquals("Jane Smith", contactRepository.getContact(id)!!.name)

        contactRepository.deleteContact(id)
        assertNull(contactRepository.getContact(id))
    }

    @Test
    fun messageRepositoryStatusUpdatesAndWatch() = runTest {
        val contactId = contactRepository.addContact(
            DomainContact(name = "Bob", phoneNumber = "+15550000001")
        )
        val msgId = messageRepository.addMessage(
            DomainMessage(
                contactId = contactId,
                phoneNumber = "+15550000001",
                direction = "SENT",
                channel = "SMS",
                body = "Hello",
                status = "PENDING"
            )
        )

        messageRepository.updateMessageStatus(msgId, "SENT")
        assertEquals("SENT", messageRepository.getMessage(msgId)!!.status)

        messageRepository.updateMessageStatus(msgId, "DELIVERED")
        val messages = messageRepository.watchMessagesByContact(contactId).first()
        assertEquals(1, messages.size)
        assertEquals("DELIVERED", messages[0].status)
        assertEquals("Hello", messages[0].body)
    }

    @Test
    fun presetRepositoryDynamicVariablesMapping() = runTest {
        val preset = DomainPresetMessage(
            title = "Survey",
            bodyTemplate = "Hi {respondent.name}",
            dynamicVariables = listOf("{respondent.name}", "{collector.phone}")
        )
        val id = presetRepository.addPreset(preset)

        val fetched = presetRepository.getPreset(id)!!
        assertEquals(listOf("{respondent.name}", "{collector.phone}"), fetched.dynamicVariables)
        assertEquals("Hi {respondent.name}", fetched.bodyTemplate)

        val all = presetRepository.getAllPresets()
        assertEquals(1, all.size)
    }

    @Test
    fun collectorProfileRepositoryRoundTrip() = runTest {
        val profile = com.censustracker.domain.model.CollectorProfile(
            name = "Collector",
            phoneNumber = "+10000000000",
            email = "c@example.com",
            referenceLink = "https://example.com"
        )
        profileRepository.saveProfile(profile)

        val fetched = profileRepository.getProfile()!!
        assertEquals("Collector", fetched.name)
        assertEquals("c@example.com", fetched.email)
        assertEquals("https://example.com", fetched.referenceLink)

        val watched = profileRepository.watchProfile().first()
        assertEquals("Collector", watched?.name)
    }
}
