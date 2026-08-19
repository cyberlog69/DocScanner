package com.example.docscanner.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.docscanner.data.pref.OcrLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Offline OCR service using ML Kit Text Recognition v2 (bundled multi-language models).
 * Supports Latin (English/European), Devanagari (Hindi/Marathi), Chinese, Japanese, and Korean.
 * No network request is ever made — all models run 100% on-device.
 */
class OcrService(
    private val context: Context
) {
    private val recognizers = mutableMapOf<OcrLanguage, TextRecognizer>()

    private fun getRecognizer(language: OcrLanguage): TextRecognizer {
        return recognizers.getOrPut(language) {
            val options = when (language) {
                OcrLanguage.LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
                OcrLanguage.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
                OcrLanguage.CHINESE -> ChineseTextRecognizerOptions.Builder().build()
                OcrLanguage.JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
                OcrLanguage.KOREAN -> KoreanTextRecognizerOptions.Builder().build()
            }
            TextRecognition.getClient(options)
        }
    }

    /**
     * Runs OCR on a [Bitmap] with the specified [language] and returns extracted text.
     */
    suspend fun recognizeText(bitmap: Bitmap, language: OcrLanguage = OcrLanguage.LATIN): OcrResult =
        suspendCancellableCoroutine { cont ->
            val client = getRecognizer(language)
            val image = InputImage.fromBitmap(bitmap, 0)
            client.process(image)
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
        }

    /**
     * Runs OCR on an image file with the specified [language].
     */
    suspend fun recognizeTextFromFile(file: File, language: OcrLanguage = OcrLanguage.LATIN): OcrResult {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot decode image: ${file.path}")
        return recognizeText(bitmap, language)
    }

    fun close() {
        recognizers.values.forEach { it.close() }
        recognizers.clear()
    }
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
