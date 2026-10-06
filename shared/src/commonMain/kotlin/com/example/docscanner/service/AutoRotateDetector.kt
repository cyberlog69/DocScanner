package com.example.docscanner.service

import kotlin.math.abs

/**
 * Data sample representing the orientation of an OCR text element or line.
 *
 * @property angleDegrees Orientation angle of text line in degrees (-180..180 or 0..360).
 * @property confidence Recognition confidence (0.0 .. 1.0).
 * @property width Bounding box width in pixels.
 * @property height Bounding box height in pixels.
 */
data class TextOrientationSample(
    val angleDegrees: Float,
    val confidence: Float = 1.0f,
    val width: Float = 0f,
    val height: Float = 0f
)

/**
 * Result of auto-rotate orientation detection.
 *
 * @property detectedAngleDegrees The detected orientation of the text (0, 90, 180, or 270).
 * @property suggestedCorrectionDegrees Clockwise rotation needed to make text upright (0, 90, 180, or 270).
 * @property confidence Confidence score between 0.0 and 1.0.
 */
data class AutoRotateResult(
    val detectedAngleDegrees: Int,
    val suggestedCorrectionDegrees: Int,
    val confidence: Float
) {
    val needsRotation: Boolean get() = suggestedCorrectionDegrees != 0
}

/**
 * Pure Kotlin detector that analyzes OCR text baseline angles and bounding box geometries
 * to determine if a page was scanned in landscape or upside down, and computes the exact
 * clockwise rotation needed to make the page upright.
 */
object AutoRotateDetector {

    /**
     * Analyzes a collection of text orientation samples and returns the suggested clockwise rotation.
     *
     * @param samples List of OCR text lines with angle and dimensions.
     * @return [AutoRotateResult] with [suggestedCorrectionDegrees] (0, 90, 180, or 270).
     */
    fun detectCorrection(samples: List<TextOrientationSample>): AutoRotateResult {
        if (samples.isEmpty()) {
            return AutoRotateResult(0, 0, 0f)
        }

        // Weighted voting into 4 primary quadrants:
        // 0° (upright), 90° (rotated right), 180° (upside down), 270° (rotated left)
        var score0 = 0f
        var score90 = 0f
        var score180 = 0f
        var score270 = 0f

        for (sample in samples) {
            val weight = sample.confidence.coerceIn(0.1f, 1.0f)
            // Normalize angle to [0..360)
            var normalized = sample.angleDegrees % 360f
            if (normalized < 0f) normalized += 360f

            when {
                normalized in 315.0..360.0 || normalized in 0.0..45.0 -> {
                    // Upright baseline
                    score0 += weight
                }
                normalized in 45.0..135.0 -> {
                    // Sideways text pointing right (90° clockwise)
                    score90 += weight
                }
                normalized in 135.0..225.0 -> {
                    // Upside down (180°)
                    score180 += weight
                }
                normalized in 225.0..315.0 -> {
                    // Sideways text pointing left (270° clockwise)
                    score270 += weight
                }
            }

            // Secondary heuristic: if lines are distinctly vertical (height > width * 2) and angle is 0
            if (sample.height > sample.width * 2.2f && sample.width > 0f) {
                // Strong signal of 90° or 270° rotated document
                score90 += weight * 0.5f
            }
        }

        val totalVotes = score0 + score90 + score180 + score270
        if (totalVotes == 0f) return AutoRotateResult(0, 0, 0f)

        val scores = listOf(0 to score0, 90 to score90, 180 to score180, 270 to score270)
        val winner = scores.maxByOrNull { it.second } ?: (0 to score0)
        val detected = winner.first
        val confidence = winner.second / totalVotes

        // To make the document upright:
        // If detected is 0° -> already upright (correction 0°)
        // If detected is 90° -> rotated clockwise, need 270° clockwise (or 90° CCW) to fix
        // If detected is 180° -> upside down, need 180° to fix
        // If detected is 270° -> rotated counter-clockwise, need 90° clockwise to fix
        val correction = when (detected) {
            90 -> 270
            180 -> 180
            270 -> 90
            else -> 0
        }

        // Only recommend rotation if confidence is reasonably high (> 50%)
        return if (confidence >= 0.5f) {
            AutoRotateResult(detected, correction, confidence)
        } else {
            AutoRotateResult(0, 0, confidence)
        }
    }
}
