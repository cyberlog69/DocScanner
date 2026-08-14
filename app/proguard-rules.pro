# iText keep rules
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# ML Kit keep rules
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# CameraX keep rules
-keep class androidx.camera.** { *; }

# Room keep rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Hilt keep rules
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
