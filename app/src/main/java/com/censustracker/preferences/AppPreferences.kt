package com.censustracker.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("census_tracker_prefs", Context.MODE_PRIVATE)

    var defaultSaveToPhone: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_SAVE_TO_PHONE, true)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_SAVE_TO_PHONE, value).apply()

    var defaultSendNow: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_SEND_NOW, false)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_SEND_NOW, value).apply()

    var lastSelectedPresetId: Long
        get() = prefs.getLong(KEY_LAST_PRESET_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_PRESET_ID, value).apply()

    var lastCustomBody: String
        get() = prefs.getString(KEY_LAST_CUSTOM_BODY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_CUSTOM_BODY, value).apply()

    var darkModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    /** Default dialing code prepended to respondent numbers, e.g. "+91". */
    var defaultCountryCode: String
        get() = prefs.getString(KEY_DEFAULT_COUNTRY_CODE, "+91") ?: "+91"
        set(value) = prefs.edit().putString(KEY_DEFAULT_COUNTRY_CODE, value).apply()

    /** Master kill-switch for all SMS send/receive paths. */
    var smsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SMS_ENABLED, value).apply()

    /** Master kill-switch for all WhatsApp send paths. */
    var whatsappEnabled: Boolean
        get() = prefs.getBoolean(KEY_WHATSAPP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WHATSAPP_ENABLED, value).apply()

    /** Last channel picked on the add-contact screen ("SMS" / "WHATSAPP"). */
    var lastChannel: String
        get() = prefs.getString(KEY_LAST_CHANNEL, "SMS") ?: "SMS"
        set(value) = prefs.edit().putString(KEY_LAST_CHANNEL, value).apply()

    /** Whether the add-contact form auto-formats the saved name as B·H·Name. */
    var autoFormatName: Boolean
        get() = prefs.getBoolean(KEY_AUTO_FORMAT_NAME, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_FORMAT_NAME, value).apply()

    /** Contact list sort order ("NAME" / "LAST_ACTIVITY"). */
    var contactSort: String
        get() = prefs.getString(KEY_CONTACT_SORT, "NAME") ?: "NAME"
        set(value) = prefs.edit().putString(KEY_CONTACT_SORT, value).apply()

    companion object {
        private const val KEY_DEFAULT_SAVE_TO_PHONE = "pref_save_to_phone"
        private const val KEY_DEFAULT_SEND_NOW = "pref_send_now"
        private const val KEY_LAST_PRESET_ID = "pref_last_preset_id"
        private const val KEY_LAST_CUSTOM_BODY = "pref_last_custom_body"
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_DEFAULT_COUNTRY_CODE = "pref_default_country_code"
        private const val KEY_SMS_ENABLED = "pref_sms_enabled"
        private const val KEY_WHATSAPP_ENABLED = "pref_whatsapp_enabled"
        private const val KEY_LAST_CHANNEL = "pref_last_channel"
        private const val KEY_AUTO_FORMAT_NAME = "pref_auto_format_name"
        private const val KEY_CONTACT_SORT = "pref_contact_sort"
    }
}
