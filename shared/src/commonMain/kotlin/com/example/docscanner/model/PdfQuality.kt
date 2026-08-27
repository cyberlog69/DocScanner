package com.example.docscanner.model

enum class PdfQuality(
    val displayName: String,
    val badge: String,
    val description: String,
    val dpi: Int,
    val compressionQuality: Int
) {
    STANDARD(
        displayName = "Standard",
        badge = "150 DPI",
        description = "Small file size · Fast sharing",
        dpi = 150,
        compressionQuality = 75
    ),
    HIGH(
        displayName = "High Quality",
        badge = "300 DPI",
        description = "Crisp text · Recommended for prints & scans",
        dpi = 300,
        compressionQuality = 90
    ),
    UHD_4K(
        displayName = "Ultra HD / 4K",
        badge = "600 DPI",
        description = "Maximum archival clarity · Lossless OCR",
        dpi = 600,
        compressionQuality = 100
    );

    companion object {
        fun fromString(value: String): PdfQuality {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UHD_4K
        }
    }
}
