package com.censustracker.placeholder

import com.censustracker.domain.model.CollectorProfile
import com.censustracker.domain.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceholderResolverTest {

    @Test
    fun testResolveTeacherPlaceholders() {
        val profile = CollectorProfile(
            name = "Sarah Connor",
            phoneNumber = "+15551234567",
            email = "sarah@census.gov",
            referenceLink = "https://census.gov/survey/101"
        )
        val resolver = PlaceholderResolver(collectorProfile = profile, contact = null)

        val template = "Notice from {teacher.name} ({teacher.phone}). Email: {teacher.email}, link: {teacher.link}"
        val resolved = resolver.resolve(template)

        assertEquals(
            "Notice from Sarah Connor (+15551234567). Email: sarah@census.gov, link: https://census.gov/survey/101",
            resolved
        )
    }

    @Test
    fun testResolveCollectorPlaceholders() {
        val profile = CollectorProfile(
            name = "John Smith",
            phoneNumber = "+19876543210",
            email = "john@census.org",
            referenceLink = "https://census.org/ref"
        )
        val resolver = PlaceholderResolver(collectorProfile = profile, contact = null)

        val template = "Collector: {collector.name}, Phone: {collector.phone}"
        val resolved = resolver.resolve(template)

        assertEquals("Collector: John Smith, Phone: +19876543210", resolved)
    }

    @Test
    fun testResolveRespondentPlaceholders() {
        val contact = Contact(
            name = "Jane Doe",
            phoneNumber = "+1122334455",
            houseNo = "42-B",
            location = "Green Valley, Ward 3",
            email = "jane@example.com"
        )
        val resolver = PlaceholderResolver(collectorProfile = null, contact = contact)

        val template = "Dear {respondent.name}, House {respondent.houseNo} at {respondent.location}, phone: {respondent.phone}"
        val resolved = resolver.resolve(template)

        assertEquals(
            "Dear Jane Doe, House 42-B at Green Valley, Ward 3, phone: +1122334455",
            resolved
        )
    }

    @Test
    fun testResolveCombinedPlaceholders() {
        val profile = CollectorProfile(
            name = "Teacher Alex",
            phoneNumber = "+1000000000",
            referenceLink = "https://school.edu"
        )
        val contact = Contact(
            name = "Bob Marley",
            phoneNumber = "+2000000000",
            houseNo = "7"
        )
        val resolver = PlaceholderResolver(collectorProfile = profile, contact = contact)

        val template = "Hi {respondent.name} of House {respondent.houseNo}, this is {teacher.name}. Info: {teacher.link}"
        val resolved = resolver.resolve(template)

        assertEquals(
            "Hi Bob Marley of House 7, this is Teacher Alex. Info: https://school.edu",
            resolved
        )
    }

    @Test
    fun testAvailablePlaceholders() {
        val placeholders = PlaceholderResolver.availablePlaceholders()
        assertTrue(placeholders.contains("{collector.name}"))
        assertTrue(placeholders.contains("{collector.phone}"))
        assertTrue(placeholders.contains("{collector.email}"))
        assertTrue(placeholders.contains("{collector.link}"))
        assertTrue(placeholders.contains("{respondent.name}"))
        assertTrue(placeholders.contains("{respondent.phone}"))
        assertTrue(placeholders.contains("{respondent.houseNo}"))
        assertTrue(placeholders.contains("{respondent.location}"))
        assertTrue(placeholders.contains("{respondent.email}"))
    }
}
