# Census Tracker (Phase 1 — SMS)

A local-first Android app for census field collectors to manage respondent
contacts, send templated SMS/WhatsApp messages with auto-filled placeholders,
track delivery status, and recover from failures — all data stored on-device
in Room (SQLite) with JSON export/import backup.

## Features

### Respondent management
- Add/edit/delete respondents: owner name, 10-digit phone (with configurable
  country code, default `+91`), building ID, house/address, optional email
- **Auto-format contact name** checkbox: saved as `B<Building> H<House> <Owner>`
- Optional write-through to the device phone book (ContactsProvider)
- Contact list with search, saved/unsaved filters, sort by name or last activity,
  last-message preview, and unread badges for incoming replies

### Messaging
- Per-contact chat UI with sent/received bubbles, channel badges (SMS / WA)
- **SMS** via `SmsManager` with delivery + sent status callbacks
  (`PENDING → SENT → DELIVERED`, `FAILED`, `FAILED_NO_SERVICE`,
  `FAILED_AIRPLANE_MODE`, `FAILED_NO_PDU`)
- **WhatsApp** via official `wa.me` deep links — opens WhatsApp/WhatsApp
  Business with the message pre-filled (no API keys required; delivery
  receipts are not available without the Business Platform API)
- Placeholder engine: `{collector.*}` and `{respondent.*}` tokens inserted via
  UI chips, resolved per recipient
- Preset message templates (CRUD), default "Census Self-Enumeration" template
- **Broadcast SMS**: multi-select recipients, preset or custom body,
  per-recipient placeholder resolution, sequential send with progress,
  per-recipient failure isolation
- **Retry queue**: all `PENDING`/`FAILED*` messages across contacts with
  retry (channel-aware) and delete/cancel
- Incoming SMS auto-matched to contacts by phone number; unknown senders are
  auto-added as respondents — replies appear in-app with unread badges

### Reliability & diagnostics
- Persistent **Error Log** (Settings → Error Log): every failed action records
  timestamp, action, reason, and technical detail; filterable, copyable, clearable
- Global crash handler writes uncaught exceptions to the same log
- **Developer Options** kill switches: disable SMS or WhatsApp app-wide
  (blocks sends *and* incoming SMS capture; every blocked attempt is logged)
- Stale `PENDING` messages are failed on boot / app update (`BootReceiver`)
- Dark theme toggle; default toggles for save-to-phone and send-now

### Backup & restore
- Export/import full registry (profile, contacts, messages, presets) as JSON
  via the system file picker (SAF)
- App-specific format marker (`"format": "com.censustracker.backup"`,
  `formatVersion`) — foreign JSON files are rejected
- Idempotent import: contacts dedupe by normalized phone number, messages by
  content, presets by title; message `contactId`s are remapped safely

## Tech stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- Room (SQLite) + Kotlin coroutines `Flow` for reactive UI
- Hilt dependency injection
- `SmsManager` + SMS broadcast receivers, ContactsProvider
- `kotlinx.serialization` for JSON backup
- minSdk 24, targetSdk/compileSdk 34

## Project structure

```
app/src/main/java/com/censustracker/
├── di/AppModule.kt                  # Hilt app + DI modules + crash handler
├── ui/
│   ├── MainActivity.kt              # NavHost + routing + theme state
│   ├── theme/Theme.kt               # Material3 light/dark color scheme
│   ├── screen/                      # Compose screens (see AGENTS.md)
│   └── viewmodel/                   # Hilt ViewModels + UiState
├── domain/
│   ├── model/DomainModels.kt        # Pure Kotlin models
│   ├── repository/                  # Repository interfaces
│   └── usecase/                     # Use-case classes
├── data/
│   ├── local/room/                  # AppDatabase (v2), entities, DAOs
│   ├── repository/impl/             # Room repository implementations
│   └── json/JsonBackupService.kt    # Export/import with format validation
├── sms/                             # SmsSender, SmsReceiver, SmsDeliveryReceiver
├── whatsapp/WhatsAppSender.kt       # wa.me deep-link sender
├── contacts/                        # Device contact read/write
├── placeholder/PlaceholderResolver.kt
├── preferences/AppPreferences.kt    # SharedPreferences wrapper
├── logging/                         # ErrorLogger (JSONL) + ErrorLogEntry
├── util/PhoneNumberFormatter.kt     # +CC normalization, wa.me digits
└── receiver/BootReceiver.kt         # Fails stale PENDING messages on boot
```

## Requirements

- Android Studio (Hedgehog or newer) + JDK 17
- Android SDK Platform 34, build-tools
- A physical device or emulator for SMS/WhatsApp flows
  (emulator SMS can be tested via `adb` emulator console; WhatsApp needs the
  app installed)

## Build & test

```bash
./gradlew assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew assembleRelease        # release APK (unsigned; sign before distribution)
./gradlew testDebugUnitTest      # unit tests (Robolectric, 32 tests)
./gradlew lintDebug              # Android lint
```

## Permissions (AndroidManifest.xml)

`SEND_SMS`, `READ_SMS`, `RECEIVE_SMS`, `WRITE_CONTACTS`, `READ_CONTACTS`,
`RECEIVE_BOOT_COMPLETED` — all requested at runtime where required.
`android.hardware.telephony` is declared optional so the app still installs
on devices without cellular radio.

## WhatsApp integration notes

- Sending uses `https://wa.me/<digits>?text=<encoded>` — requires WhatsApp
  or WhatsApp Business installed; the user confirms inside WhatsApp.
- Replies are **not** captured (consumer WhatsApp exposes no API). For
  two-way WhatsApp, the WhatsApp Business Platform (Cloud API) is required:
  Meta Business account, dedicated business number, access token, and a
  backend — plus per-message fees for business-initiated templates.
- For reply capture today, ask respondents to reply by **SMS** — the app
  matches incoming SMS to contacts automatically.

## Backup file format

Exports are JSON with a format header:

```json
{
  "format": "com.censustracker.backup",
  "formatVersion": 1,
  "app": "Census Tracker",
  "exportedAt": 1730000000000,
  "collectorProfile": { ... },
  "contacts": [ ... ],
  "messages": [ ... ],
  "presets": [ ... ]
}
```

Import rejects any file missing the `format` marker or with a newer
`formatVersion` than the app supports.

## Known limitations (Phase 1)

- WhatsApp delivery/read receipts unavailable (deep-link only)
- SMS requires the device to have an active SIM / telephony radio
- Release build is unsigned and unminified — sign + enable R8 for distribution
- Instrumented on-device SMS tests are not automated; verify manually

## License

Internal project — all rights reserved.
