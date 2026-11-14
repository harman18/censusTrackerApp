package com.censustracker.data.json

import android.content.Context
import android.net.Uri
import com.censustracker.data.local.room.AppDatabase
import com.censustracker.data.local.room.entity.CollectorProfile
import com.censustracker.data.local.room.entity.Contact
import com.censustracker.data.local.room.entity.Message
import com.censustracker.data.local.room.entity.PresetMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

data class ImportSummary(
    val contactsAdded: Int = 0,
    val contactsUpdated: Int = 0,
    val messagesImported: Int = 0,
    val messagesSkipped: Int = 0,
    val presetsImported: Int = 0
)

class JsonBackupService(
    private val context: Context,
    private val database: AppDatabase
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val backupDir: File by lazy {
        File(context.getExternalFilesDirs(null).firstOrNull() ?: context.filesDir, "census_backup")
    }

    suspend fun exportToJson(): File {
        return withContext(Dispatchers.IO) {
            backupDir.mkdirs()
            val file = File(backupDir, "census_export_${System.currentTimeMillis()}.json")
            file.writeText(buildBackupJson())
            file
        }
    }

    /** Export backup JSON directly to a user-selected URI (e.g. Downloads via SAF). */
    suspend fun exportToUri(uri: Uri) {
        withContext(Dispatchers.IO) {
            val payload = buildBackupJson()
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Could not open destination for backup file")
        }
    }

    private suspend fun buildBackupJson(): String {
        val contacts = database.contactDao().getAll()
        val messages = database.messageDao().getAll()
        val presets = database.presetMessageDao().getAll()
        val profile = database.collectorProfileDao().getProfile()

        val root = buildJsonObject {
            put("format", JsonPrimitive(BACKUP_FORMAT))
            put("formatVersion", JsonPrimitive(BACKUP_FORMAT_VERSION))
            put("app", JsonPrimitive("Census Tracker"))
            put("exportedAt", JsonPrimitive(System.currentTimeMillis()))
            put("collectorProfile", profile?.let { profileToJson(it) } ?: JsonNull)
            put("contacts", contactsToJson(contacts))
            put("messages", messagesToJson(messages))
            put("presets", presetsToJson(presets))
        }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    suspend fun importFromJson(file: File): ImportSummary {
        return withContext(Dispatchers.IO) {
            importFromJsonString(file.readText())
        }
    }

    /** Import backup JSON from a user-selected URI (e.g. picked via SAF file picker). */
    suspend fun importFromUri(uri: Uri): ImportSummary {
        return withContext(Dispatchers.IO) {
            val content = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw IllegalStateException("Could not read selected backup file")
            importFromJsonString(content)
        }
    }

    /**
     * Idempotent import: contacts are matched by phone number (updated in place when they
     * already exist), messages are de-duplicated by content, and presets are matched by title.
     * Imported rows always receive fresh auto-generated IDs so they never overwrite
     * unrelated local records; message contactIds are remapped through the contact map.
     */
    suspend fun importFromJsonString(content: String): ImportSummary {
        val root = json.decodeFromString<JsonObject>(content)

        // Only accept files produced by this app — reject arbitrary JSON.
        val format = root["format"]?.jsonPrimitive?.contentOrNull
        if (format != BACKUP_FORMAT) {
            throw IllegalArgumentException(
                "This file is not a Census Tracker backup. " +
                        "Please choose a JSON file exported from the app."
            )
        }
        val version = root["formatVersion"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        if (version > BACKUP_FORMAT_VERSION) {
            throw IllegalArgumentException(
                "This backup was created by a newer version of the app (format v$version). " +
                        "Please update the app before importing."
            )
        }

        val contactDao = database.contactDao()
        val messageDao = database.messageDao()
        val presetDao = database.presetMessageDao()

        // --- Collector profile (single row, safe to replace) ---
        root["collectorProfile"]?.takeIf { it !is JsonNull }?.let { obj ->
            database.collectorProfileDao().insert(collectorProfileFromJson(obj.jsonObject))
        }

        // --- Contacts: dedupe by phone number ---
        val workingContacts = contactDao.getAll().toMutableList()
        val contactIdMap = mutableMapOf<Long, Long>() // backup id -> local id
        var contactsAdded = 0
        var contactsUpdated = 0

        root["contacts"]?.let { arr ->
            if (arr is JsonArray) {
                arr.map { it.jsonObject }.forEach { obj ->
                    val parsed = contactFromJson(obj)
                    val backupId = parsed.id
                    val match = findContactByPhone(workingContacts, parsed.phoneNumber)
                    if (match != null) {
                        val updated = match.copy(
                            name = parsed.name.ifBlank { match.name },
                            houseNo = parsed.houseNo ?: match.houseNo,
                            location = parsed.location ?: match.location,
                            email = parsed.email ?: match.email,
                            savedToPhone = parsed.savedToPhone || match.savedToPhone,
                            channelPreference = parsed.channelPreference,
                            updatedAt = System.currentTimeMillis()
                        )
                        contactDao.update(updated)
                        workingContacts[workingContacts.indexOf(match)] = updated
                        contactIdMap[backupId] = match.id
                        contactsUpdated++
                    } else {
                        val insertedId = contactDao.insert(parsed.copy(id = 0))
                        workingContacts.add(parsed.copy(id = insertedId))
                        contactIdMap[backupId] = insertedId
                        contactsAdded++
                    }
                }
            }
        }

        // --- Messages: remap contactId, skip duplicates ---
        val allLocalContacts = workingContacts
        val seenMessageKeys = messageDao.getAll()
            .map { messageDedupKey(it.contactId, it.phoneNumber, it.direction, it.body, it.sentAt ?: it.receivedAt ?: it.createdAt) }
            .toMutableSet()
        var messagesImported = 0
        var messagesSkipped = 0

        root["messages"]?.let { arr ->
            if (arr is JsonArray) {
                arr.map { it.jsonObject }.forEach { obj ->
                    val parsed = messageFromJson(obj)
                    val resolvedContactId = contactIdMap[parsed.contactId]
                        ?: findContactByPhone(allLocalContacts, parsed.phoneNumber)?.id
                        ?: return@forEach

                    val key = messageDedupKey(
                        resolvedContactId, parsed.phoneNumber, parsed.direction, parsed.body,
                        parsed.sentAt ?: parsed.receivedAt ?: parsed.createdAt
                    )
                    if (!seenMessageKeys.add(key)) {
                        messagesSkipped++
                        return@forEach
                    }

                    messageDao.insert(
                        parsed.copy(id = 0, contactId = resolvedContactId)
                    )
                    messagesImported++
                }
            }
        }

        // --- Presets: dedupe by title ---
        val existingPresets = presetDao.getAll().associateBy { it.title }
        var presetsImported = 0

        root["presets"]?.let { arr ->
            if (arr is JsonArray) {
                arr.map { it.jsonObject }.forEach { obj ->
                    val parsed = presetFromJson(obj)
                    val existing = existingPresets[parsed.title]
                    if (existing != null) {
                        presetDao.update(
                            existing.copy(
                                bodyTemplate = parsed.bodyTemplate,
                                dynamicVariables = parsed.dynamicVariables,
                                createdAt = parsed.createdAt
                            )
                        )
                    } else {
                        presetDao.insert(parsed.copy(id = 0))
                    }
                    presetsImported++
                }
            }
        }

        return ImportSummary(
            contactsAdded = contactsAdded,
            contactsUpdated = contactsUpdated,
            messagesImported = messagesImported,
            messagesSkipped = messagesSkipped,
            presetsImported = presetsImported
        )
    }

    companion object {
        const val BACKUP_FORMAT = "com.censustracker.backup"
        const val BACKUP_FORMAT_VERSION = 1
    }

    private fun messageDedupKey(
        contactId: Long,
        phoneNumber: String,
        direction: String,
        body: String,
        timestamp: Long
    ): String = "$contactId|$phoneNumber|$direction|$body|$timestamp"

    private fun phoneDigits(number: String): String = number.filter { it.isDigit() }

    private fun findContactByPhone(contacts: List<Contact>, phoneNumber: String): Contact? {
        val digits = phoneDigits(phoneNumber)
        if (digits.length < 7) return null
        return contacts.find { c ->
            val cDigits = phoneDigits(c.phoneNumber)
            cDigits == digits ||
                    (cDigits.length >= 7 && digits.endsWith(cDigits)) ||
                    (digits.length >= 7 && cDigits.endsWith(digits))
        }
    }

    private fun profileToJson(p: CollectorProfile): JsonObject =
        buildJsonObject {
            put("id", JsonPrimitive(p.id))
            put("name", JsonPrimitive(p.name))
            put("phoneNumber", JsonPrimitive(p.phoneNumber))
            put("email", p.email?.let { JsonPrimitive(it) } ?: JsonNull)
            put("referenceLink", p.referenceLink?.let { JsonPrimitive(it) } ?: JsonNull)
            put("createdAt", JsonPrimitive(p.createdAt))
            put("updatedAt", JsonPrimitive(p.updatedAt))
        }

    private fun collectorProfileFromJson(obj: JsonObject): CollectorProfile =
        CollectorProfile(
            id = obj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
            name = obj["name"]?.jsonPrimitive?.content ?: "",
            phoneNumber = obj["phoneNumber"]?.jsonPrimitive?.content ?: "",
            email = obj["email"]?.jsonPrimitive?.contentOrNull,
            referenceLink = obj["referenceLink"]?.jsonPrimitive?.contentOrNull,
            createdAt = obj["createdAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = obj["updatedAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis()
        )

    private fun contactsToJson(contacts: List<Contact>): JsonArray =
        buildJsonArray { contacts.forEach { add(contactToJson(it)) } }

    private fun contactToJson(c: Contact): JsonObject =
        buildJsonObject {
            put("id", JsonPrimitive(c.id))
            put("name", JsonPrimitive(c.name))
            put("phoneNumber", JsonPrimitive(c.phoneNumber))
            put("houseNo", c.houseNo?.let { JsonPrimitive(it) } ?: JsonNull)
            put("location", c.location?.let { JsonPrimitive(it) } ?: JsonNull)
            put("email", c.email?.let { JsonPrimitive(it) } ?: JsonNull)
            put("savedToPhone", JsonPrimitive(c.savedToPhone))
            put("channelPreference", JsonPrimitive(c.channelPreference))
            put("createdAt", JsonPrimitive(c.createdAt))
            put("updatedAt", JsonPrimitive(c.updatedAt))
        }

    private fun contactFromJson(obj: JsonObject): Contact =
        Contact(
            id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0,
            name = obj["name"]?.jsonPrimitive?.content ?: "",
            phoneNumber = obj["phoneNumber"]?.jsonPrimitive?.content ?: "",
            houseNo = obj["houseNo"]?.jsonPrimitive?.contentOrNull,
            location = obj["location"]?.jsonPrimitive?.contentOrNull,
            email = obj["email"]?.jsonPrimitive?.contentOrNull,
            savedToPhone = obj["savedToPhone"]?.jsonPrimitive?.content?.toBoolean() ?: false,
            channelPreference = obj["channelPreference"]?.jsonPrimitive?.content ?: "SMS",
            createdAt = obj["createdAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = obj["updatedAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis()
        )

    private fun messagesToJson(messages: List<Message>): JsonArray =
        buildJsonArray { messages.forEach { add(messageToJson(it)) } }

    private fun messageToJson(m: Message): JsonObject =
        buildJsonObject {
            put("id", JsonPrimitive(m.id))
            put("contactId", JsonPrimitive(m.contactId))
            put("phoneNumber", JsonPrimitive(m.phoneNumber))
            put("direction", JsonPrimitive(m.direction))
            put("channel", JsonPrimitive(m.channel))
            put("body", JsonPrimitive(m.body))
            put("status", JsonPrimitive(m.status))
            put("sentAt", m.sentAt?.let { JsonPrimitive(it) } ?: JsonNull)
            put("receivedAt", m.receivedAt?.let { JsonPrimitive(it) } ?: JsonNull)
            put("smsMessageId", m.smsMessageId?.let { JsonPrimitive(it) } ?: JsonNull)
            put("isRead", JsonPrimitive(m.isRead))
            put("createdAt", JsonPrimitive(m.createdAt))
        }

    private fun messageFromJson(obj: JsonObject): Message =
        Message(
            id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0,
            contactId = obj["contactId"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0,
            phoneNumber = obj["phoneNumber"]?.jsonPrimitive?.content ?: "",
            direction = obj["direction"]?.jsonPrimitive?.content ?: "SENT",
            channel = obj["channel"]?.jsonPrimitive?.content ?: "SMS",
            body = obj["body"]?.jsonPrimitive?.content ?: "",
            status = obj["status"]?.jsonPrimitive?.content ?: "PENDING",
            sentAt = obj["sentAt"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
            receivedAt = obj["receivedAt"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
            smsMessageId = obj["smsMessageId"]?.jsonPrimitive?.contentOrNull,
            isRead = obj["isRead"]?.jsonPrimitive?.content?.toBoolean() ?: false,
            createdAt = obj["createdAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis()
        )

    private fun presetsToJson(presets: List<PresetMessage>): JsonArray =
        buildJsonArray { presets.forEach { add(presetToJson(it)) } }

    private fun presetToJson(p: PresetMessage): JsonObject =
        buildJsonObject {
            put("id", JsonPrimitive(p.id))
            put("title", JsonPrimitive(p.title))
            put("bodyTemplate", JsonPrimitive(p.bodyTemplate))
            put("dynamicVariables", JsonPrimitive(p.dynamicVariables))
            put("createdAt", JsonPrimitive(p.createdAt))
        }

    private fun presetFromJson(obj: JsonObject): PresetMessage =
        PresetMessage(
            id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0,
            title = obj["title"]?.jsonPrimitive?.content ?: "",
            bodyTemplate = obj["bodyTemplate"]?.jsonPrimitive?.content ?: "",
            dynamicVariables = obj["dynamicVariables"]?.jsonPrimitive?.content ?: "[]",
            createdAt = obj["createdAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis()
        )
}
