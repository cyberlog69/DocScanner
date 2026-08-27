package com.example.docscanner.model

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Paper format standards and dimension analysis for scanned documents.
 */
enum class PaperFormat(
    val displayName: String,
    val badge: String,
    val physicalDimensions: String
) {
    A4("A4 Standard", "A4", "210 × 297 mm"),
    US_LETTER("US Letter", "Letter", "8.5 × 11 in"),
    US_LEGAL("US Legal", "Legal", "8.5 × 14 in"),
    ID_CARD("ID / Credit Card", "ID Card", "85.6 × 54 mm"),
    RECEIPT("Receipt Roll", "Receipt", "Variable Roll"),
    CUSTOM("Custom Document", "Custom", "Variable Dimensions")
}

data class PageMetrics(
    val widthPx: Int,
    val heightPx: Int,
    val megapixels: Double,
    val format: PaperFormat,
    val estimatedDpi: Int
) {
    val resolutionString: String get() = "$widthPx × $heightPx px"
    val megapixelsString: String get() = "${((megapixels * 10).roundToInt()) / 10.0} MP"
    val dpiBadgeString: String get() = "$estimatedDpi DPI"
}

object DocumentMetricsCalculator {

    fun calculate(widthPx: Int, heightPx: Int): PageMetrics {
        if (widthPx <= 0 || heightPx <= 0) {
            return PageMetrics(0, 0, 0.0, PaperFormat.CUSTOM, 72)
        }

        val minDim = minOf(widthPx, heightPx).toDouble()
        val maxDim = maxOf(widthPx, heightPx).toDouble()
        val aspectRatio = minDim / maxDim // value between 0.0 and 1.0

        val format = when {
            // Receipt / Roll: very tall/long aspect ratio
            aspectRatio < 0.45 -> PaperFormat.RECEIPT

            // US Legal: 8.5 / 14 = 0.6071
            abs(aspectRatio - (8.5 / 14.0)) < 0.015 -> PaperFormat.US_LEGAL

            // ID Card: 53.98 / 85.60 = 0.6306
            abs(aspectRatio - (53.98 / 85.60)) < 0.025 -> PaperFormat.ID_CARD

            // ISO 216 A4 (1 / sqrt(2) = 0.7071)
            abs(aspectRatio - 0.7071) < 0.035 -> PaperFormat.A4

            // US Letter: 8.5 / 11 = 0.7727
            abs(aspectRatio - (8.5 / 11.0)) < 0.035 -> PaperFormat.US_LETTER

            else -> PaperFormat.CUSTOM
        }

        val mp = (widthPx.toLong() * heightPx.toLong()) / 1_000_000.0

        // Estimated scan DPI based on standard A4 or Letter physical width (approx 8.27 - 8.5 inches)
        val estimatedDpi = when {
            maxDim >= 3000 -> 300
            maxDim >= 2000 -> 200
            maxDim >= 1400 -> 150
            else -> 72
        }

        return PageMetrics(
            widthPx = widthPx,
            heightPx = heightPx,
            megapixels = mp,
            format = format,
            estimatedDpi = estimatedDpi
        )
    }
}
