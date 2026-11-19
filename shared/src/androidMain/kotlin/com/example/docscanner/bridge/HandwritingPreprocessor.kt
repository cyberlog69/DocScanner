package com.example.docscanner.bridge

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Single implementation of the handwriting contrast pass, shared by the Android app
 * and the KMP OCR bridge so the two cannot drift apart.
 *
 * ML Kit and Apple Vision do not expose a print/handwriting classifier, so this only
 * improves stroke contrast for faint ink; it does not detect handwriting.
 */
fun preprocessForHandwriting(source: Bitmap): Bitmap {
    val enhanced = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(enhanced)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val colorMatrix = ColorMatrix(
        floatArrayOf(
            1.35f, 0f, 0f, 0f, -15f,
            0f, 1.35f, 0f, 0f, -15f,
            0f, 0f, 1.35f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return enhanced
}
