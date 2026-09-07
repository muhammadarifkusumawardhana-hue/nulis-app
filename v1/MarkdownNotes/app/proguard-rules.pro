# Keep Markwon
-keep class io.noties.markwon.** { *; }

# Keep Google Drive API
-keep class com.google.api.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep iText PDF
-keep class com.itextpdf.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *

# Keep Gson models
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
