package com.censustracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DomainModelsTest {

    @Test
    fun testContactCreation() {
        val contact = Contact(
            id = 1L,
            name = "Test Respondent",
            phoneNumber = "+123456789",
            houseNo = "10A",
            location = "North Sector",
            email = "test@domain.com",
            savedToPhone = true,
            channelPreference = "SMS"
        )

        assertEquals("Test Respondent", contact.name)
        assertEquals("+123456789", contact.phoneNumber)
        assertEquals("10A", contact.houseNo)
        assertEquals(true, contact.savedToPhone)
    }

    @Test
    fun testMessageCreationAndStatus() {
        val message = Message(
            id = 10L,
            contactId = 1L,
            phoneNumber = "+123456789",
            direction = MessageDirection.SENT.name,
            channel = MessageChannel.SMS.name,
            body = "Hello from Census",
            status = MessageStatus.SENT.name
        )

        assertEquals("SENT", message.direction)
        assertEquals("SMS", message.channel)
        assertEquals("SENT", message.status)
        assertNotNull(message.createdAt)
    }

    @Test
    fun testPresetMessageCreation() {
        val preset = PresetMessage(
            id = 5L,
            title = "Census Intro",
            bodyTemplate = "Hello {respondent.name}",
            dynamicVariables = listOf("{respondent.name}")
        )

        assertEquals("Census Intro", preset.title)
        assertEquals(1, preset.dynamicVariables.size)
    }
}
