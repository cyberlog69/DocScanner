package com.example.docscanner.bridge

import android.content.Context
import android.graphics.BitmapFactory
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

    actual suspend fun recognizeText(imagePath: String, language: OcrLanguage): String =
        withContext(Dispatchers.IO) {
            val recognizer = getRecognizer(language)
            val bitmap = if (imagePath.startsWith("content://") && context != null) {
                context.contentResolver.openInputStream(Uri.parse(imagePath))?.use {
                    BitmapFactory.decodeStream(it)
                } ?: BitmapFactory.decodeFile(imagePath)
            } else {
                BitmapFactory.decodeFile(imagePath)
            } ?: return@withContext ""

            val inputImage = InputImage.fromBitmap(bitmap, 0)

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
