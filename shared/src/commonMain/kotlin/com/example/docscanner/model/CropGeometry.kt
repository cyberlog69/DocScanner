package com.example.docscanner.model

import kotlin.math.max
import kotlin.math.min

/**
 * Normalized 2D point where (0.0, 0.0) is the top-left corner
 * and (1.0, 1.0) is the bottom-right corner of an image.
 */
data class PointF(
    val x: Float,
    val y: Float
) {
    fun clamp(): PointF = PointF(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f)
    )

    fun toPixel(width: Int, height: Int): Pair<Float, Float> =
        Pair(x * width, y * height)

    fun distanceTo(other: PointF): Float =
        kotlin.math.hypot(x - other.x, y - other.y)
}

/**
 * A quadrilateral defined by 4 corner points in normalized [0..1] coordinates:
 * [topLeft], [topRight], [bottomRight], [bottomLeft].
 */
data class Quadrilateral(
    val topLeft: PointF = PointF(0f, 0f),
    val topRight: PointF = PointF(1f, 0f),
    val bottomRight: PointF = PointF(1f, 1f),
    val bottomLeft: PointF = PointF(0f, 1f)
) {
    /** Clamps all 4 points to within [0..1] */
    fun clamp(): Quadrilateral = Quadrilateral(
        topLeft = topLeft.clamp(),
        topRight = topRight.clamp(),
        bottomRight = bottomRight.clamp(),
        bottomLeft = bottomLeft.clamp()
    )

    /**
     * Checks if the quadrilateral is a valid convex polygon (cross products of consecutive edges
     * all share the same sign). Prevents self-intersecting / bow-tie distortion.
     */
    fun isValidConvex(): Boolean {
        val points = listOf(topLeft, topRight, bottomRight, bottomLeft)
        var sign = 0
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % 4]
            val p3 = points[(i + 2) % 4]

            val crossProduct = (p2.x - p1.x) * (p3.y - p2.y) - (p2.y - p1.y) * (p3.x - p2.x)
            if (crossProduct != 0f) {
                val currentSign = if (crossProduct > 0f) 1 else -1
                if (sign == 0) {
                    sign = currentSign
                } else if (sign != currentSign) {
                    return false
                }
            }
        }
        return sign != 0
    }

    /**
     * Returns the bounding box of the 4 corners as normalized (minX, minY, maxX, maxY).
     */
    fun boundingBox(): FloatArray {
        val minX = min(min(topLeft.x, topRight.x), min(bottomRight.x, bottomLeft.x)).coerceIn(0f, 1f)
        val minY = min(min(topLeft.y, topRight.y), min(bottomRight.y, bottomLeft.y)).coerceIn(0f, 1f)
        val maxX = max(max(topLeft.x, topRight.x), max(bottomRight.x, bottomLeft.x)).coerceIn(0f, 1f)
        val maxY = max(max(topLeft.y, topRight.y), max(bottomRight.y, bottomLeft.y)).coerceIn(0f, 1f)
        return floatArrayOf(minX, minY, maxX, maxY)
    }

    companion object {
        /** Creates a full uncropped boundary spanning the entire image. */
        fun fullImage(): Quadrilateral = Quadrilateral(
            topLeft = PointF(0f, 0f),
            topRight = PointF(1f, 0f),
            bottomRight = PointF(1f, 1f),
            bottomLeft = PointF(0f, 1f)
        )

        /** Creates an inset crop rectangle (e.g., 5% margin inside image bounds). */
        fun inset(marginRatio: Float = 0.05f): Quadrilateral {
            val m = marginRatio.coerceIn(0f, 0.45f)
            return Quadrilateral(
                topLeft = PointF(m, m),
                topRight = PointF(1f - m, m),
                bottomRight = PointF(1f - m, 1f - m),
                bottomLeft = PointF(m, 1f - m)
            )
        }
    }
}
