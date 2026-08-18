package com.example.docscanner.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CameraQuality(val displayName: String, val description: String, val badge: String) {
    STANDARD("Standard (1080p)", "Fast scan & compact file size (~1-2 MB/page)", "1080p"),
    HIGH("High (2K)", "Enhanced sharpness for text & forms (~3-5 MB/page)", "2K"),
    UHD_4K("Ultra HD (4K Native)", "Maximum sensor resolution & micro-detail (~8-15 MB/page)", "4K UHD")
}

enum class PdfQuality(val displayName: String, val dpi: Int, val compressionQuality: Int, val description: String, val badge: String) {
    STANDARD("Standard (150 DPI)", 150, 80, "Smallest size, perfect for email & messaging", "150 DPI"),
    HIGH("High (300 DPI)", 300, 92, "Print-ready crisp text & clean graphics", "300 DPI"),
    UHD_4K("Ultra HD (600 DPI)", 600, 100, "Archival quality with zero compression artifacts", "600 DPI UHD")
}

enum class ThemeMode(val displayName: String, val description: String) {
    SYSTEM("System Default", "Follow device theme"),
    LIGHT("Light", "Always use light theme"),
    DARK("Dark", "Always use dark theme")
}

data class ScannerSettingsState(
    val cameraQuality: CameraQuality = CameraQuality.HIGH,
    val pdfQuality: PdfQuality = PdfQuality.HIGH,
    val autoOcr: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class ScannerPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("docscanner_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CAMERA_QUALITY = "key_camera_quality"
        private const val KEY_PDF_QUALITY = "key_pdf_quality"
        private const val KEY_AUTO_OCR = "key_auto_ocr"
        private const val KEY_THEME_MODE = "key_theme_mode"
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ScannerSettingsState> = _settings.asStateFlow()

    private fun loadSettings(): ScannerSettingsState {
        val camOrdinal = prefs.getInt(KEY_CAMERA_QUALITY, CameraQuality.HIGH.ordinal)
        val pdfOrdinal = prefs.getInt(KEY_PDF_QUALITY, PdfQuality.HIGH.ordinal)
        val autoOcr = prefs.getBoolean(KEY_AUTO_OCR, true)
        val themeOrdinal = prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)

        return ScannerSettingsState(
            cameraQuality = CameraQuality.entries.getOrNull(camOrdinal) ?: CameraQuality.HIGH,
            pdfQuality = PdfQuality.entries.getOrNull(pdfOrdinal) ?: PdfQuality.HIGH,
            autoOcr = autoOcr,
            themeMode = ThemeMode.entries.getOrNull(themeOrdinal) ?: ThemeMode.SYSTEM
        )
    }

    fun setCameraQuality(quality: CameraQuality) {
        prefs.edit().putInt(KEY_CAMERA_QUALITY, quality.ordinal).apply()
        _settings.value = _settings.value.copy(cameraQuality = quality)
    }

    fun setPdfQuality(quality: PdfQuality) {
        prefs.edit().putInt(KEY_PDF_QUALITY, quality.ordinal).apply()
        _settings.value = _settings.value.copy(pdfQuality = quality)
    }

    fun setAutoOcr(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_OCR, enabled).apply()
        _settings.value = _settings.value.copy(autoOcr = enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }
}
