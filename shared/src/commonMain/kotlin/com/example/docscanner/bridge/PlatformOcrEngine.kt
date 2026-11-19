package com.example.docscanner.bridge

import com.example.docscanner.model.OcrLanguage

/**
 * Recognition profile for OCR processing.
 *
 * Neither ML Kit nor Apple Vision exposes a print/handwriting classifier, so these
 * values select an optimisation profile rather than performing detection.
 */
enum class OcrRecognitionMode(val displayName: String, val description: String) {
    /** Default profile for mixed printed and handwritten content */
    BALANCED_AUTO("Auto (Print & Handwriting)", "Default. Works well for most scans."),

    /** Optimised for high-throughput printed text and standard document scans */
    PRINTED_ONLY("Printed Text Only", "Best for books, contracts and printed forms."),

    /** Applies a contrast pass tuned for cursive, handwritten notes and form entries */
    HANDWRITING_OPTIMIZED("Handwriting Optimized", "Boosts contrast for faint or cursive ink.")
}

expect class PlatformOcrEngine {
    suspend fun recognizeText(imagePath: String, language: OcrLanguage): String
    suspend fun recognizeText(imagePath: String, language: OcrLanguage, mode: OcrRecognitionMode): String
}

