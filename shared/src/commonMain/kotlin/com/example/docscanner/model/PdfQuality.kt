package com.example.docscanner.model

enum class PdfQuality(
    val displayName: String,
    val dpi: Int,
    val compressionQuality: Int,
    val description: String,
    val badge: String
) {
    STANDARD(
        displayName = "Standard (150 DPI)",
        dpi = 150,
        compressionQuality = 80,
        description = "Smallest size, perfect for email & messaging",
        badge = "150 DPI"
    ),
    HIGH(
        displayName = "High (300 DPI)",
        dpi = 300,
        compressionQuality = 92,
        description = "Print-ready crisp text & clean graphics",
        badge = "300 DPI"
    ),
    UHD_4K(
        displayName = "Ultra HD (600 DPI)",
        dpi = 600,
        compressionQuality = 100,
        description = "Archival quality with zero compression artifacts",
        badge = "600 DPI UHD"
    );

    companion object {
        fun fromString(value: String): PdfQuality {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: HIGH
        }
    }
}

