package com.example.docscanner.bridge

import com.example.docscanner.model.OcrLanguage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.Vision.VNRequestTextRecognitionLevelFast
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class PlatformOcrEngine {

    actual suspend fun recognizeText(imagePath: String, language: OcrLanguage): String =
        recognizeText(imagePath, language, OcrRecognitionMode.BALANCED_AUTO)

    actual suspend fun recognizeText(
        imagePath: String,
        language: OcrLanguage,
        mode: OcrRecognitionMode
    ): String = withContext(Dispatchers.Default) {
        val fileUrl = NSURL.fileURLWithPath(imagePath)

        suspendCancellableCoroutine { continuation ->
            val request = VNRecognizeTextRequest { request, error ->
                if (error != null) {
                    continuation.resume("")
                    return@VNRecognizeTextRequest
                }

                val results = request?.results
                val textBuilder = StringBuilder()

                if (results != null) {
                    for (item in results) {
                        val observation = item as? VNRecognizedTextObservation ?: continue
                        val candidates = observation.topCandidates(1u)
                        val candidate = candidates.firstOrNull() as? VNRecognizedText
                        if (candidate != null) {
                            textBuilder.append(candidate.string()).append("\n")
                        }
                    }
                }

                continuation.resume(textBuilder.toString().trim())
            }

            request.recognitionLanguages = listOf(language.iosRecognitionCode)
            request.usesLanguageCorrection = true

            // Set recognition level based on mode: Accurate level engages Apple's neural handwriting model
            request.recognitionLevel = when (mode) {
                OcrRecognitionMode.PRINTED_ONLY -> VNRequestTextRecognitionLevelFast
                OcrRecognitionMode.BALANCED_AUTO,
                OcrRecognitionMode.HANDWRITING_OPTIMIZED -> VNRequestTextRecognitionLevelAccurate
            }

            val handler = VNImageRequestHandler(uRL = fileUrl, options = emptyMap<Any?, Any>())
            try {
                handler.performRequests(listOf(request), null)
            } catch (e: Exception) {
                continuation.resume("")
            }
        }
    }
}

