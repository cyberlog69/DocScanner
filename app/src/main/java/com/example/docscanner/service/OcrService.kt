package com.example.docscanner.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Offline OCR service using ML Kit Text Recognition v2 (bundled model).
 * No network request is ever made — the model runs 100% on-device.
 */
class OcrService(
    private val context: Context
) {
    // Bundled recognizer — fully offline, no GMS required
    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    /**
     * Runs OCR on a [Bitmap] and returns the full extracted text.
     */
    suspend fun recognizeText(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.map { block ->
                    OcrBlock(
                        text = block.text,
                        lines = block.lines.map { line ->
                            OcrLine(
                                text = line.text,
                                confidence = line.confidence
                            )
                        }
                    )
                }
                cont.resume(OcrResult(fullText = visionText.text, blocks = blocks))
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
        cont.invokeOnCancellation { recognizer.close() }
    }

    /**
     * Runs OCR on an image file.
     */
    suspend fun recognizeTextFromFile(file: File): OcrResult {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot decode image: ${file.path}")
        return recognizeText(bitmap)
    }

    fun close() = recognizer.close()
}

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock> = emptyList()
)

data class OcrBlock(
    val text: String,
    val lines: List<OcrLine> = emptyList()
)

data class OcrLine(
    val text: String,
    val confidence: Float
)
