package com.example.docscanner.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import com.example.docscanner.bridge.OcrRecognitionMode
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
import kotlin.math.abs

/**
 * Offline OCR service using ML Kit Text Recognition v2 (bundled multi-language models).
 * Supports Latin (English/European), Devanagari (Hindi/Marathi), Chinese, Japanese, and Korean.
 * Includes handwriting detection and contrast-enhancement pre-processing.
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
     * Enhances contrast and stroke definition to optimize detection of cursive and faint handwriting.
     */
    private fun preprocessForHandwriting(source: Bitmap): Bitmap {
        val enhanced = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhanced)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Increase contrast (+35%) and slightly deepen shadows to sharpen handwriting strokes
        val colorMatrix = ColorMatrix(floatArrayOf(
            1.35f, 0f, 0f, 0f, -15f,
            0f, 1.35f, 0f, 0f, -15f,
            0f, 0f, 1.35f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return enhanced
    }

    /**
     * Runs OCR on a [Bitmap] with the specified [language] and [mode], returning extracted text.
     */
    suspend fun recognizeText(
        bitmap: Bitmap,
        language: OcrLanguage = OcrLanguage.LATIN,
        mode: OcrRecognitionMode = OcrRecognitionMode.BALANCED_AUTO
    ): OcrResult =
        suspendCancellableCoroutine { cont ->
            val client = getRecognizer(language)
            val processedBitmap = when (mode) {
                OcrRecognitionMode.HANDWRITING_OPTIMIZED -> preprocessForHandwriting(bitmap)
                else -> bitmap
            }
            val image = InputImage.fromBitmap(processedBitmap, 0)
            client.process(image)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.map { block ->
                        OcrBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            lines = block.lines.map { line ->
                                val isLikelyHandwritten = mode == OcrRecognitionMode.HANDWRITING_OPTIMIZED ||
                                    (line.confidence in 0.35f..0.82f && abs(line.angle) > 1.2f)
                                OcrLine(
                                    text = line.text,
                                    confidence = line.confidence,
                                    boundingBox = line.boundingBox,
                                    angle = line.angle,
                                    isLikelyHandwritten = isLikelyHandwritten
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
     * Detects text baseline orientation and computes recommended auto-rotation correction.
     */
    suspend fun detectPageOrientation(bitmap: Bitmap, language: OcrLanguage = OcrLanguage.LATIN): AutoRotateResult {
        val ocr = recognizeText(bitmap, language)
        val samples = ocr.blocks.flatMap { block ->
            block.lines.map { line ->
                TextOrientationSample(
                    angleDegrees = line.angle,
                    confidence = line.confidence,
                    width = line.boundingBox?.width()?.toFloat() ?: 0f,
                    height = line.boundingBox?.height()?.toFloat() ?: 0f
                )
            }
        }
        return AutoRotateDetector.detectCorrection(samples)
    }

    /**
     * Runs OCR on an image file with the specified [language] and [mode].
     */
    suspend fun recognizeTextFromFile(
        file: File,
        language: OcrLanguage = OcrLanguage.LATIN,
        mode: OcrRecognitionMode = OcrRecognitionMode.BALANCED_AUTO
    ): OcrResult {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot decode image: ${file.path}")
        return recognizeText(bitmap, language, mode)
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
    val boundingBox: Rect? = null,
    val lines: List<OcrLine> = emptyList()
)

data class OcrLine(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect? = null,
    val angle: Float = 0f,
    val isLikelyHandwritten: Boolean = false
)
