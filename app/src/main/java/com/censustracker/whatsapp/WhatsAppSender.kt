package com.censustracker.whatsapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.censustracker.util.PhoneNumberFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface WhatsAppSender {
    /**
     * Opens WhatsApp (or WhatsApp Business) with a pre-filled message via the
     * official wa.me deep link. Returns success once the intent is dispatched;
     * delivery cannot be confirmed without the WhatsApp Business Platform API.
     */
    suspend fun send(phoneNumber: String, message: String): WhatsAppResult
}

data class WhatsAppResult(
    val success: Boolean,
    val error: String? = null
)

class AndroidWhatsAppSender @Inject constructor(
    @ApplicationContext private val context: Context
) : WhatsAppSender {

    override suspend fun send(phoneNumber: String, message: String): WhatsAppResult {
        return withContext(Dispatchers.Main) {
            val number = PhoneNumberFormatter.toWhatsAppNumber(phoneNumber)
            if (number.length < 7) {
                return@withContext WhatsAppResult(success = false, error = "Invalid phone number for WhatsApp")
            }
            val url = "https://wa.me/$number?text=" + URLEncoder.encode(message, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                WhatsAppResult(success = true)
            } catch (_: ActivityNotFoundException) {
                // Fall back to any handler of wa.me links
                try {
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                    WhatsAppResult(success = true)
                } catch (e: Exception) {
                    WhatsAppResult(success = false, error = "WhatsApp is not installed on this device")
                }
            } catch (e: Exception) {
                WhatsAppResult(success = false, error = e.message ?: "Failed to open WhatsApp")
            }
        }
    }
}
