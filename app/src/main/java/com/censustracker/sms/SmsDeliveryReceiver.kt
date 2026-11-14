package com.censustracker.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.censustracker.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsDeliveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra("message_id", 0L)
        val action = intent.action ?: return

        if (messageId > 0L) {
            val status = when (action) {
                "com.censustracker.SMS_SENT" -> {
                    when (resultCode) {
                        Activity.RESULT_OK -> "SENT"
                        SmsManager.RESULT_ERROR_NO_SERVICE -> "FAILED_NO_SERVICE"
                        SmsManager.RESULT_ERROR_RADIO_OFF -> "FAILED_AIRPLANE_MODE"
                        SmsManager.RESULT_ERROR_NULL_PDU -> "FAILED_NO_PDU"
                        else -> "FAILED"
                    }
                }
                "com.censustracker.SMS_DELIVERED" -> {
                    if (resultCode == Activity.RESULT_OK) "DELIVERED" else "FAILED"
                }
                else -> null
            }

            status?.let { newStatus ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AppModule.messageRepository.updateMessageStatus(messageId, newStatus)
                    } catch (e: Exception) {
                        AppModule.errorLogger.log(
                            action = "update_delivery_status",
                            message = "Failed to update delivery status for message $messageId",
                            detail = e.message
                        )
                    }
                }
            }
        }
    }
}
