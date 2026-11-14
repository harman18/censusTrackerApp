# Census Tracker — Android App (Phase 1: SMS System)

## Project Structure
```
cenusTrackerAp/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
├── gradle/wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── src/main/AndroidManifest.xml
│   ├── src/main/res/
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── colors.xml
│   │   │   └── themes.xml
│   │   ├── drawable/ic_launcher_foreground.xml
│   │   └── mipmap-anydpi-v26/ic_launcher.xml, ic_launcher_round.xml
│   └── src/main/java/com/censustracker/
│       ├── di/AppModule.kt                  (Hilt Application + DI modules)
│       ├── ui/
│       │   ├── MainActivity.kt              (NavHost + screen routing)
│       │   ├── theme/Theme.kt               (Material3 theme)
│       │   ├── navigator/                   (Screen route constants)
│       │   ├── screen/
│       │   │   ├── CollectorSetupScreen.kt  (collector profile setup)
│       │   │   ├── ContactListScreen.kt     (contact list)
│       │   │   ├── AddContactScreen.kt      (add contact + send SMS)
│       │   │   ├── ChatScreen.kt            (per-contact chat)
│       │   │   └── PresetMessageScreen.kt   (preset message CRUD)
│       │   └── viewmodel/
│       │       ├── CollectorSetupViewModel.kt
│       │       ├── ContactListViewModel.kt
│       │       ├── AddContactViewModel.kt
│       │       ├── ChatViewModel.kt
│       │       └── PresetMessageViewModel.kt
│       ├── domain/
│       │   ├── model/DomainModels.kt        (pure Kotlin data classes)
│       │   ├── repository/                  (repository interfaces)
│       │   └── usecase/                     (use case classes)
│       ├── data/
│       │   ├── local/room/
│       │   │   ├── AppDatabase.kt
│       │   │   ├── entity/                  (Room entities)
│       │   │   ├── dao/                     (Room DAOs)
│       │   │   └── typeconverters/Converters.kt
│       │   ├── repository/impl/             (Room-based repository impls)
│       │   └── json/JsonBackupService.kt    (JSON export/import)
│       ├── sms/
│       │   ├── SmsSender.kt                 (interface + Android impl)
│       │   ├── SmsReceiver.kt               (incoming SMS broadcast)
│       │   └── SmsDeliveryReceiver.kt       (delivery reports)
│       ├── contacts/PhoneContactWriter.kt   (ContactsProvider write)
│       └── placeholder/PlaceholderResolver.kt (message placeholder resolution)
```

## Tech Stack (Phase 1)
- Kotlin + Jetpack Compose (Material3)
- Room (SQLite) + kotlinx.serialization JSON backup
- Hilt DI
- Navigation Compose
- Android SmsManager + ContactsProvider
- minSdk 24, targetSdk 34

## Permissions (AndroidManifest.xml)
SEND_SMS, READ_SMS, RECEIVE_SMS, WRITE_CONTACTS, READ_CONTACTS, RECEIVE_BOOT_COMPLETED

## Build Instructions
1. Install Android Studio + JDK 17 + Android SDK (compileSdk 34)
2. Open the project in Android Studio
3. Run `gradle wrapper` to generate `gradlew` if needed
4. Build: `./gradlew assembleDebug`
5. Install on emulator/device: `./gradlew installDebug`

## Architecture
- **Data Layer:** Room entities + DAOs + Repository implementations + JSON backup service
- **Domain Layer:** Pure Kotlin models + Repository interfaces + Use cases
- **UI Layer:** Compose screens + Hilt ViewModels + Navigation Compose

## Phases
- **Phase 1 (current):** SMS system, local Room DB, contact saving, placeholder messages, chat UI
- **Phase 2:** WhatsApp integration, dual-SIM selection, advanced features
- **Phase 3:** Login, remote DB, web application

## Plan Reference
Detailed plan: `/Users/harmanjotsingh/.local/share/kilo/plans/1794737530000-census-sms-tracker-phase1.md`
