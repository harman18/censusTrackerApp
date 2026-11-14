package com.censustracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.censustracker.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recovers messages left in PENDING state when the app process was killed
 * between inserting the message and receiving the carrier send callback.
 * Marks them FAILED so they surface the retry action in chat instead of
 * staying silently stuck.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppModule.messageRepository.failPendingMessages()
            } catch (e: Exception) {
                AppModule.errorLogger.logAsync(
                    action = "boot_recovery",
                    message = "Failed to mark stale pending messages as failed",
                    detail = e.message
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
