# FamilyGuard ProGuard Rules

# Keep data classes for Gson
-keepclassmembers class com.familyguard.app.data.remote.api.** { *; }
-keepclassmembers class com.familyguard.app.domain.model.** { *; }
-keepclassmembers class com.familyguard.app.data.local.entity.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Don't warn about missing classes
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontwarn org.codehaus.mojo.animal_sniffer.**
