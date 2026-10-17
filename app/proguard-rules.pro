# Add project specific ProGuard rules here.
# Keep Retrofit & Gson DTOs and domain models serialized to/from JSON
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations
-keep class com.skech.moglog.data.remote.** { *; }
-keep class com.skech.moglog.domain.model.** { *; }
-keep class com.skech.moglog.data.security.BackupEnvelopePayload { *; }
-keep class com.skech.moglog.data.repository.CloudData { *; }

# Keep Room entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep SQLCipher native JNI hooks
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.**

# Keep Google Tink crypto primitives
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**

# Keep Kotlinx Serialization route classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.skech.moglog.**$$serializer { *; }
-keepclassmembers class com.skech.moglog.** {
    *** Companion;
}
-keepclasseswithmembers class com.skech.moglog.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Strip debug/verbose Log calls from release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}