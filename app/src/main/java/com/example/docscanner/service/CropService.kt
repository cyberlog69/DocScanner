package com.example.docscanner.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.example.docscanner.model.PointF
import com.example.docscanner.model.Quadrilateral
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Service providing high-performance, offline image cropping and perspective correction.
 * Operates on [Bitmap] and [Quadrilateral] corner coordinates.
 */
object CropService {

    /**
     * Crops and rectifies a [bitmap] using the specified 4 normalized [corners].
     *
     * @param bitmap The source image to crop.
     * @param corners Normalized quadrilateral corners [0..1] representing the crop region.
     * @return A newly allocated, cropped [Bitmap]. The caller is responsible for recycling old bitmaps.
     */
    fun cropBitmap(bitmap: Bitmap, corners: Quadrilateral): Bitmap {
        val clamped = corners.clamp()
        val origW = bitmap.width.toFloat()
        val origH = bitmap.height.toFloat()

        // Source points in pixel coordinates
        val tl = clamped.topLeft.toPixel(bitmap.width, bitmap.height)
        val tr = clamped.topRight.toPixel(bitmap.width, bitmap.height)
        val br = clamped.bottomRight.toPixel(bitmap.width, bitmap.height)
        val bl = clamped.bottomLeft.toPixel(bitmap.width, bitmap.height)

        val src = floatArrayOf(
            tl.first, tl.second,
            tr.first, tr.second,
            br.first, br.second,
            bl.first, bl.second
        )

        // Compute destination dimensions based on edge lengths
        val topWidth = hypot(tr.first - tl.first, tr.second - tl.second)
        val bottomWidth = hypot(br.first - bl.first, br.second - bl.second)
        val leftHeight = hypot(bl.first - tl.first, bl.second - tl.second)
        val rightHeight = hypot(br.first - tr.first, br.second - tr.second)

        val destWidth = max(topWidth, bottomWidth).toInt().coerceIn(16, bitmap.width)
        val destHeight = max(leftHeight, rightHeight).toInt().coerceIn(16, bitmap.height)

        val dst = floatArrayOf(
            0f, 0f,
            destWidth.toFloat(), 0f,
            destWidth.toFloat(), destHeight.toFloat(),
            0f, destHeight.toFloat()
        )

        val matrix = Matrix()
        val polySuccess = matrix.setPolyToPoly(src, 0, dst, 0, 4)

        return if (polySuccess) {
            try {
                val output = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, matrix, paint)
                output
            } catch (_: OutOfMemoryError) {
                // Fallback to axis-aligned crop if memory is constrained
                fallbackBoundingBoxCrop(bitmap, clamped)
            } catch (_: Exception) {
                fallbackBoundingBoxCrop(bitmap, clamped)
            }
        } else {
            fallbackBoundingBoxCrop(bitmap, clamped)
        }
    }

    /**
     * Fallback crop using axis-aligned bounding box.
     */
    private fun fallbackBoundingBoxCrop(bitmap: Bitmap, corners: Quadrilateral): Bitmap {
        val bbox = corners.boundingBox()
        val startX = (bbox[0] * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val startY = (bbox[1] * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val width = ((bbox[2] - bbox[0]) * bitmap.width).toInt().coerceIn(1, bitmap.width - startX)
        val height = ((bbox[3] - bbox[1]) * bitmap.height).toInt().coerceIn(1, bitmap.height - startY)

        return Bitmap.createBitmap(bitmap, startX, startY, width, height)
    }
}
