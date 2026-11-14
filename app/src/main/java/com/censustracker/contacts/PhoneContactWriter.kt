package com.censustracker.contacts

import android.app.Activity
import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayList
import javax.inject.Inject

class PhoneContactWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun saveContact(name: String, phoneNumber: String, email: String? = null): Boolean {
        return try {
            val ops = ArrayList<ContentProviderOperation>()

            val rawContactUri = ContactsContract.RawContacts.CONTENT_URI
            ops.add(
                ContentProviderOperation.newInsert(rawContactUri)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            email?.takeIf { it.isNotEmpty() }?.let { email ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build()
                )
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteContactByPhone(phoneNumber: String): Boolean {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                arrayOf(phoneNumber),
                null
            )
            cursor?.use { cur ->
                var deleted = false
                while (cur.moveToNext()) {
                    val rawContactId = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                    val deletedRows = context.contentResolver.delete(
                        ContactsContract.RawContacts.CONTENT_URI,
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(rawContactId)
                    )
                    deleted = deletedRows > 0
                }
                deleted
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
