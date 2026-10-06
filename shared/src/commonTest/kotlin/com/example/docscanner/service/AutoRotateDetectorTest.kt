package com.example.docscanner.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoRotateDetectorTest {

    @Test
    fun testEmptySamplesReturnsNoRotation() {
        val result = AutoRotateDetector.detectCorrection(emptyList())
        assertEquals(0, result.detectedAngleDegrees)
        assertEquals(0, result.suggestedCorrectionDegrees)
        assertFalse(result.needsRotation)
    }

    @Test
    fun testUprightTextNeedsNoRotation() {
        val samples = listOf(
            TextOrientationSample(angleDegrees = 0f, confidence = 0.95f),
            TextOrientationSample(angleDegrees = 2f, confidence = 0.90f),
            TextOrientationSample(angleDegrees = -1f, confidence = 0.98f)
        )
        val result = AutoRotateDetector.detectCorrection(samples)
        assertEquals(0, result.detectedAngleDegrees)
        assertEquals(0, result.suggestedCorrectionDegrees)
        assertFalse(result.needsRotation)
        assertTrue(result.confidence > 0.8f)
    }

    @Test
    fun test90DegreesClockwiseTiltedTextNeeds270DegreesCorrection() {
        val samples = listOf(
            TextOrientationSample(angleDegrees = 90f, confidence = 0.95f),
            TextOrientationSample(angleDegrees = 88f, confidence = 0.92f),
            TextOrientationSample(angleDegrees = 92f, confidence = 0.89f)
        )
        val result = AutoRotateDetector.detectCorrection(samples)
        assertEquals(90, result.detectedAngleDegrees)
        assertEquals(270, result.suggestedCorrectionDegrees)
        assertTrue(result.needsRotation)
    }

    @Test
    fun test180DegreesUpsideDownTextNeeds180DegreesCorrection() {
        val samples = listOf(
            TextOrientationSample(angleDegrees = 180f, confidence = 0.95f),
            TextOrientationSample(angleDegrees = 178f, confidence = 0.90f),
            TextOrientationSample(angleDegrees = -180f, confidence = 0.93f)
        )
        val result = AutoRotateDetector.detectCorrection(samples)
        assertEquals(180, result.detectedAngleDegrees)
        assertEquals(180, result.suggestedCorrectionDegrees)
        assertTrue(result.needsRotation)
    }

    @Test
    fun test270DegreesCounterClockwiseTiltedTextNeeds90DegreesCorrection() {
        val samples = listOf(
            TextOrientationSample(angleDegrees = 270f, confidence = 0.95f),
            TextOrientationSample(angleDegrees = -90f, confidence = 0.90f),
            TextOrientationSample(angleDegrees = 268f, confidence = 0.92f)
        )
        val result = AutoRotateDetector.detectCorrection(samples)
        assertEquals(270, result.detectedAngleDegrees)
        assertEquals(90, result.suggestedCorrectionDegrees)
        assertTrue(result.needsRotation)
    }
}
