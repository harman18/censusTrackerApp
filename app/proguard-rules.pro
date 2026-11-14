# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/harmanjotsingh/Desktop/Projects/Test_Projects/cenusTrackerAp/build.gradle.kts
# You can completely edit these flags to change how your code is obfuscated.

-keep class com.censustracker.domain.model.** { *; }
-keep class com.censustracker.data.local.room.entity.** { *; }
-keepnames class * extends android.app.Application
