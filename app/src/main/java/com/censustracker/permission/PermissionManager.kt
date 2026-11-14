package com.censustracker.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class PermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean
)

object PermissionManager {

    val REQUIRED_PERMISSIONS = listOf(
        PermissionItem(
            permission = Manifest.permission.SEND_SMS,
            title = "Send SMS",
            description = "Allows the app to send official census messages and preset notices directly to respondents.",
            isGranted = false
        ),
        PermissionItem(
            permission = Manifest.permission.RECEIVE_SMS,
            title = "Receive SMS",
            description = "Allows the app to automatically receive and log respondent survey replies in the chat thread.",
            isGranted = false
        ),
        PermissionItem(
            permission = Manifest.permission.READ_SMS,
            title = "Read SMS",
            description = "Allows the app to verify delivered and received census message records.",
            isGranted = false
        ),
        PermissionItem(
            permission = Manifest.permission.WRITE_CONTACTS,
            title = "Write Contacts",
            description = "Allows saving newly surveyed respondents directly to your device contact book for offline reference.",
            isGranted = false
        ),
        PermissionItem(
            permission = Manifest.permission.READ_CONTACTS,
            title = "Read Contacts",
            description = "Allows cross-checking and syncing existing contacts with your census registry.",
            isGranted = false
        )
    )

    fun checkAllPermissions(context: Context): List<PermissionItem> {
        return REQUIRED_PERMISSIONS.map { item ->
            val granted = ContextCompat.checkSelfPermission(
                context,
                item.permission
            ) == PackageManager.PERMISSION_GRANTED
            item.copy(isGranted = granted)
        }
    }

    fun areAllGranted(context: Context): Boolean {
        return checkAllPermissions(context).all { it.isGranted }
    }
}
