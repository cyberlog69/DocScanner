# ── iText 7 ──────────────────────────────────────────────────────────────────
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
# BouncyCastle (bundled with iText for PDF encryption)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ── ML Kit ───────────────────────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── CameraX ──────────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── Kotlin Serialization ──────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$serializer {
    static **$serializer INSTANCE;
}

# ── Jetpack Compose ───────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Data classes / Models (prevent field stripping) ───────────────────────────
-keep class com.example.docscanner.data.model.** { *; }
-keep class com.example.docscanner.data.pref.** { *; }
