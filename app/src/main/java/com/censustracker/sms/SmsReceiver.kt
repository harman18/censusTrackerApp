package com.censustracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.censustracker.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // System-level kill switch: when SMS is disabled, ignore incoming messages entirely.
        if (!AppModule.appPreferences.smsEnabled) {
            AppModule.errorLogger.logAsync(
                action = "receive_sms_blocked",
                message = "Incoming SMS ignored: SMS messaging is disabled in settings",
                detail = null
            )
            return
        }

        val bundle = intent.extras ?: return
        @Suppress("DEPRECATION")
        val pdus = bundle["pdus"] as? Array<*> ?: return
        val format = bundle.getString("format")

        val messages = pdus.mapNotNull { pdu ->
            val bytes = pdu as? ByteArray ?: return@mapNotNull null
            try {
                if (format != null) {
                    SmsMessage.createFromPdu(bytes, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(bytes)
                }
            } catch (_: Exception) {
                null
            }
        }

        if (messages.isEmpty()) return

        // Group fragments by sender
        val groupedBySender = messages.groupBy { it.originatingAddress ?: "Unknown" }

        groupedBySender.forEach { (sender, fragments) ->
            val fullBody = fragments.joinToString("") { it.messageBody ?: "" }
            val timestamp = fragments.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Normalize phone number to match contact (check exact or last 10 digits)
                    val allContacts = AppModule.contactRepository.getAllContacts()
                    val senderDigits = sender.filter { it.isDigit() }

                    val matchedContact = allContacts.find { c ->
                        val cDigits = c.phoneNumber.filter { it.isDigit() }
                        c.phoneNumber == sender ||
                                (cDigits.length >= 7 && senderDigits.endsWith(cDigits)) ||
                                (senderDigits.length >= 7 && cDigits.endsWith(senderDigits))
                    }

                    val contactId = matchedContact?.id ?: 0L

                    // If contact exists, or create as a respondent automatically so user never misses incoming replies
                    val finalContactId = if (contactId > 0L) {
                        contactId
                    } else {
                        // Automatically record unknown respondent in Census contacts
                        AppModule.contactRepository.addContact(
                            com.censustracker.domain.model.Contact(
                                name = "Respondent ($sender)",
                                phoneNumber = sender,
                                savedToPhone = false,
                                channelPreference = "SMS"
                            )
                        )
                    }

                    AppModule.messageRepository.addMessage(
                        com.censustracker.domain.model.Message(
                            contactId = finalContactId,
                            phoneNumber = sender,
                            direction = "RECEIVED",
                            channel = "SMS",
                            body = fullBody,
                            status = "DELIVERED",
                            receivedAt = timestamp,
                            createdAt = timestamp
                        )
                    )
                } catch (e: Exception) {
                    AppModule.errorLogger.log(
                        action = "receive_sms",
                        message = "Failed to record incoming SMS from $sender",
                        detail = e.message
                    )
                }
            }
        }
    }
}
