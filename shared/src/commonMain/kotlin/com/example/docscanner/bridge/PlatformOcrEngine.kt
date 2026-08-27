package com.example.docscanner.bridge

import com.example.docscanner.model.OcrLanguage

expect class PlatformOcrEngine {
    suspend fun recognizeText(imagePath: String, language: OcrLanguage): String
}
