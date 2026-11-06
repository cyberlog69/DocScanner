package com.example.docscanner.bridge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import com.example.docscanner.model.OcrLanguage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class PlatformOcrEngine(private val context: Context? = null) {

    private val recognizerCache = mutableMapOf<OcrLanguage, TextRecognizer>()

    private fun getRecognizer(language: OcrLanguage): TextRecognizer {
        return recognizerCache.getOrPut(language) {
            when (language) {
                OcrLanguage.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                OcrLanguage.DEVANAGARI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                OcrLanguage.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                OcrLanguage.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                OcrLanguage.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            }
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

    actual suspend fun recognizeText(imagePath: String, language: OcrLanguage): String =
        recognizeText(imagePath, language, OcrRecognitionMode.BALANCED_AUTO)

    actual suspend fun recognizeText(
        imagePath: String,
        language: OcrLanguage,
        mode: OcrRecognitionMode
    ): String = withContext(Dispatchers.IO) {
        val recognizer = getRecognizer(language)
        val rawBitmap = if (imagePath.startsWith("content://") && context != null) {
            context.contentResolver.openInputStream(Uri.parse(imagePath))?.use {
                BitmapFactory.decodeStream(it)
            } ?: BitmapFactory.decodeFile(imagePath)
        } else {
            BitmapFactory.decodeFile(imagePath)
        } ?: return@withContext ""

        val processedBitmap = when (mode) {
            OcrRecognitionMode.HANDWRITING_OPTIMIZED -> preprocessForHandwriting(rawBitmap)
            else -> rawBitmap
        }

        val inputImage = InputImage.fromBitmap(processedBitmap, 0)

        suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text.trim())
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
    }
}

