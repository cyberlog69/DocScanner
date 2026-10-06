package com.example.docscanner.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CropGeometryTest {

    @Test
    fun testFullImageQuadrilateral() {
        val quad = Quadrilateral.fullImage()
        assertEquals(0f, quad.topLeft.x)
        assertEquals(0f, quad.topLeft.y)
        assertEquals(1f, quad.topRight.x)
        assertEquals(0f, quad.topRight.y)
        assertEquals(1f, quad.bottomRight.x)
        assertEquals(1f, quad.bottomRight.y)
        assertEquals(0f, quad.bottomLeft.x)
        assertEquals(1f, quad.bottomLeft.y)
        assertTrue(quad.isValidConvex())
    }

    @Test
    fun testInsetQuadrilateral() {
        val quad = Quadrilateral.inset(0.1f)
        assertEquals(0.1f, quad.topLeft.x)
        assertEquals(0.1f, quad.topLeft.y)
        assertEquals(0.9f, quad.topRight.x)
        assertEquals(0.1f, quad.topRight.y)
        assertEquals(0.9f, quad.bottomRight.x)
        assertEquals(0.9f, quad.bottomRight.y)
        assertEquals(0.1f, quad.bottomLeft.x)
        assertEquals(0.9f, quad.bottomLeft.y)
        assertTrue(quad.isValidConvex())
    }

    @Test
    fun testInvalidNonConvexQuadrilateral() {
        // Crossed diagonals / bowtie quadrilateral
        val invalidQuad = Quadrilateral(
            topLeft = PointF(0f, 0f),
            topRight = PointF(1f, 1f),
            bottomRight = PointF(1f, 0f),
            bottomLeft = PointF(0f, 1f)
        )
        assertFalse(invalidQuad.isValidConvex())
    }

    @Test
    fun testBoundingBoxCalculation() {
        val quad = Quadrilateral(
            topLeft = PointF(0.1f, 0.2f),
            topRight = PointF(0.85f, 0.15f),
            bottomRight = PointF(0.9f, 0.95f),
            bottomLeft = PointF(0.05f, 0.8f)
        )
        val bbox = quad.boundingBox()
        assertEquals(0.05f, bbox[0])
        assertEquals(0.15f, bbox[1])
        assertEquals(0.9f, bbox[2])
        assertEquals(0.95f, bbox[3])
        assertTrue((bbox[2] - bbox[0]) > 0.8f)
        assertTrue((bbox[3] - bbox[1]) > 0.7f)
    }

    @Test
    fun testPointFDistance() {
        val p1 = PointF(0f, 0f)
        val p2 = PointF(3f, 4f)
        assertEquals(5f, p1.distanceTo(p2))
    }
}
