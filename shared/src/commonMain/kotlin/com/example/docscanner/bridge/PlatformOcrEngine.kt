package com.example.docscanner.bridge

import com.example.docscanner.model.OcrLanguage

/**
 * Recognition mode for OCR processing.
 */
enum class OcrRecognitionMode(val displayName: String) {
    /** Balanced mode recognizing both printed and handwritten text */
    BALANCED_AUTO("Auto (Print & Handwriting)"),

    /** Optimized for high-throughput printed text and standard document scans */
    PRINTED_ONLY("Printed Text Only"),

    /** Optimized for cursive, handwritten notes, whiteboards, and form entries */
    HANDWRITING_OPTIMIZED("Handwriting Optimized")
}

expect class PlatformOcrEngine {
    suspend fun recognizeText(imagePath: String, language: OcrLanguage): String
    suspend fun recognizeText(imagePath: String, language: OcrLanguage, mode: OcrRecognitionMode): String
}

