package com.censustracker.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface SmsSender {
    suspend fun send(phoneNumber: String, message: String, databaseMessageId: Long = 0L): SmsResult
}

data class SmsResult(
    val success: Boolean,
    val messageId: String? = null,
    val error: String? = null
)

class AndroidSmsSender(private val context: Context) : SmsSender {

    override suspend fun send(phoneNumber: String, message: String, databaseMessageId: Long): SmsResult {
        return withContext(Dispatchers.IO) {
            // First check if device has telephony capability / SIM active
            val smsManager = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            } catch (_: Exception) {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Always write message to the Android system Telephony Provider (content://sms/sent or content://sms/outbox)
            // so that user can view the sent/attempted SMS in their default Messaging App
            val systemSmsUri = writeToSystemSmsStore(phoneNumber, message)

            try {
                val requestCode = (System.currentTimeMillis() % 100000).toInt()

                val sentIntent = Intent("com.censustracker.SMS_SENT").apply {
                    setPackage(context.packageName)
                    putExtra("message_id", databaseMessageId)
                    putExtra("phone_number", phoneNumber)
                    putExtra("system_sms_uri", systemSmsUri?.toString())
                }
                val deliveredIntent = Intent("com.censustracker.SMS_DELIVERED").apply {
                    setPackage(context.packageName)
                    putExtra("message_id", databaseMessageId)
                    putExtra("phone_number", phoneNumber)
                }

                val pendingSent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    sentIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val pendingDelivered = PendingIntent.getBroadcast(
                    context,
                    requestCode + 1,
                    deliveredIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    val sentIntents = ArrayList<PendingIntent>()
                    val deliveredIntents = ArrayList<PendingIntent>()
                    for (i in parts.indices) {
                        sentIntents.add(pendingSent)
                        deliveredIntents.add(pendingDelivered)
                    }
                    smsManager.sendMultipartTextMessage(
                        phoneNumber,
                        null,
                        parts,
                        sentIntents,
                        deliveredIntents
                    )
                } else {
                    smsManager.sendTextMessage(
                        phoneNumber,
                        null,
                        message,
                        pendingSent,
                        pendingDelivered
                    )
                }

                val uuid = java.util.UUID.randomUUID().toString()
                SmsResult(success = true, messageId = uuid)
            } catch (e: Exception) {
                // If carrier dispatch failed (e.g. no SIM card, airplane mode, or network error),
                // it is recorded as a failure, but it is already preserved in the system SMS app inbox/outbox.
                SmsResult(success = false, error = e.message ?: "Failed to dispatch SMS over cellular network")
            }
        }
    }

    private fun writeToSystemSmsStore(phoneNumber: String, message: String): Uri? {
        return try {
            val values = ContentValues().apply {
                put("address", phoneNumber)
                put("body", message)
                put("date", System.currentTimeMillis())
                put("read", 1)
                put("type", 2) // MESSAGE_TYPE_SENT = 2
            }
            context.contentResolver.insert(Uri.parse("content://sms/sent"), values)
        } catch (_: Exception) {
            // Some devices / permissions might restrict writing directly to system provider if not default SMS app
            null
        }
    }
}
