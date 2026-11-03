# Proguard rules for Karaoke TV
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn io.ktor.**
-dontwarn okhttp3.**
