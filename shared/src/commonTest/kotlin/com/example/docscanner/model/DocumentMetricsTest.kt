package com.example.docscanner.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentMetricsTest {

    @Test
    fun testA4FormatDetection() {
        // Standard A4 at 300 DPI is 2480 x 3508
        val metrics = DocumentMetricsCalculator.calculate(2480, 3508)
        assertEquals(PaperFormat.A4, metrics.format)
        assertEquals("2480 × 3508 px", metrics.resolutionString)
        assertEquals(300, metrics.estimatedDpi)
    }

    @Test
    fun testUsLetterDetection() {
        // US Letter at 300 DPI is 2550 x 3300
        val metrics = DocumentMetricsCalculator.calculate(2550, 3300)
        assertEquals(PaperFormat.US_LETTER, metrics.format)
    }

    @Test
    fun testIdCardDetection() {
        // ID Card ratio (approx 85.6 x 54 mm) -> e.g. 1000 x 630
        val metrics = DocumentMetricsCalculator.calculate(630, 1000)
        assertEquals(PaperFormat.ID_CARD, metrics.format)
    }

    @Test
    fun testReceiptDetection() {
        // Long receipt ratio
        val metrics = DocumentMetricsCalculator.calculate(500, 2000)
        assertEquals(PaperFormat.RECEIPT, metrics.format)
    }
}
