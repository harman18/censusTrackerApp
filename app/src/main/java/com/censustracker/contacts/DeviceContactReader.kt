package com.censustracker.contacts

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class DeviceContact(
    val name: String,
    val phoneNumber: String,
    val email: String? = null
)

class DeviceContactReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun readDeviceContacts(): List<DeviceContact> {
        val contactsList = mutableListOf<DeviceContact>()
        val seenNumbers = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
        )

        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use { cur ->
            val nameIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val idIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

            while (cur.moveToNext()) {
                val name = if (nameIndex != -1) cur.getString(nameIndex) ?: "" else ""
                val rawNumber = if (numberIndex != -1) cur.getString(numberIndex) ?: "" else ""
                val contactId = if (idIndex != -1) cur.getString(idIndex) else null

                val cleanedNumber = rawNumber.trim()
                val digitsOnly = cleanedNumber.filter { it.isDigit() }

                if (cleanedNumber.isNotBlank() && digitsOnly.length >= 7 && seenNumbers.add(digitsOnly)) {
                    val email = contactId?.let { getEmailForContactId(it) }
                    contactsList.add(
                        DeviceContact(
                            name = name.ifBlank { "Unknown Contact" },
                            phoneNumber = cleanedNumber,
                            email = email
                        )
                    )
                }
            }
        }
        return contactsList
    }

    private fun getEmailForContactId(contactId: String): String? {
        val emailCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        return emailCursor?.use { cur ->
            if (cur.moveToFirst()) {
                val idx = cur.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                if (idx != -1) cur.getString(idx) else null
            } else null
        }
    }
}
