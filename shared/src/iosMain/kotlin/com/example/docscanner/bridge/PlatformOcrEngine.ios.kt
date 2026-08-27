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
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class PlatformOcrEngine {

    actual suspend fun recognizeText(imagePath: String, language: OcrLanguage): String =
        withContext(Dispatchers.Default) {
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

                val handler = VNImageRequestHandler(uRL = fileUrl, options = emptyMap<Any?, Any>())
                try {
                    handler.performRequests(listOf(request), null)
                } catch (e: Exception) {
                    continuation.resume("")
                }
            }
        }
}
