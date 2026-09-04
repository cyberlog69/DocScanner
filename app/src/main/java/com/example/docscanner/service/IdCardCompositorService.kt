package com.example.docscanner.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.min

/**
 * Composites dual-sided ID cards (Front & Back) onto a single clean A4 document page.
 */
object IdCardCompositorService {

    private const val CANVAS_WIDTH = 1600
    private const val CANVAS_HEIGHT = 2262 // A4 ratio (~1.414)
    private const val CARD_CORNER_RADIUS = 24f

    /**
     * Composites front and back side bitmaps symmetrically onto an A4 page.
     */
    fun compositeIdCard(frontBitmap: Bitmap, backBitmap: Bitmap): Bitmap {
        val composite = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)

        // Draw crisp white background
        canvas.drawColor(Color.WHITE)

        val cardWidth = CANVAS_WIDTH * 0.85f
        val cardHeight = cardWidth / 1.586f // ID-1 standard aspect ratio (85.6mm x 53.98mm)
        val left = (CANVAS_WIDTH - cardWidth) / 2f

        // Top card (Front)
        val frontTop = CANVAS_HEIGHT * 0.12f
        val frontRect = RectF(left, frontTop, left + cardWidth, frontTop + cardHeight)
        drawCardWithLabel(canvas, frontBitmap, frontRect, "FRONT SIDE")

        // Bottom card (Back)
        val backTop = CANVAS_HEIGHT * 0.54f
        val backRect = RectF(left, backTop, left + cardWidth, backTop + cardHeight)
        drawCardWithLabel(canvas, backBitmap, backRect, "BACK SIDE")

        // Draw subtle footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(160, 160, 160)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DocScanner • Secure On-Device ID Scan", CANVAS_WIDTH / 2f, CANVAS_HEIGHT - 60f, footerPaint)

        return composite
    }

    private fun drawCardWithLabel(canvas: Canvas, cardBitmap: Bitmap, rect: RectF, label: String) {
        // Drop shadow / subtle border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 224, 230)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 249, 250)
            style = Paint.Style.FILL
        }

        // Card background & border
        canvas.drawRoundRect(rect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, bgPaint)
        canvas.drawRoundRect(rect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, borderPaint)

        // Label above card
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 110, 125)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        canvas.drawText(label, rect.left, rect.top - 16f, labelPaint)

        // Draw cropped/fitted card image with rounded corners
        val roundedBitmap = getRoundedCornerBitmap(cardBitmap, rect.width().toInt(), rect.height().toInt(), CARD_CORNER_RADIUS)
        canvas.drawBitmap(roundedBitmap, rect.left, rect.top, null)
        roundedBitmap.recycle()
    }

    private fun getRoundedCornerBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())

        // Calculate scaling to fill target
        val scaleX = targetWidth.toFloat() / src.width
        val scaleY = targetHeight.toFloat() / src.height
        val scale = min(scaleX, scaleY)

        val scaledW = (src.width * scale).toInt()
        val scaledH = (src.height * scale).toInt()
        val offsetX = (targetWidth - scaledW) / 2
        val offsetY = (targetHeight - scaledH) / 2

        canvas.drawRoundRect(rectF, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = Rect(0, 0, src.width, src.height)
        val destRect = Rect(offsetX, offsetY, offsetX + scaledW, offsetY + scaledH)
        canvas.drawBitmap(src, srcRect, destRect, paint)

        return output
    }
}
