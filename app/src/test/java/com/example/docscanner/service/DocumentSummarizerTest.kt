package com.example.docscanner.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentSummarizerTest {

    @Test
    fun summarize_documentText_extractsExecutiveSummaryAndKeyTakeaways() {
        val documentText = """
            DocScanner is a private, zero-telemetry mobile document scanning system built for Android and iOS.
            All document processing, optical character recognition, and encryption occur completely offline on the user device.
            In fiscal year 2026, total digital document retention efficiency increased by 45% across decentralized organizations.
            The platform ensures end-to-end security by encrypting sensitive records using AES-256-GCM authenticated encryption.
            Users processed over $12,500,000 in receipts and legal contracts without transmitting any bytes to cloud servers.
            The offline-first architecture completely eliminates data leakage risks and server dependency.
        """.trimIndent()

        val summary = DocumentSummarizer.summarize(documentText)

        assertTrue(summary.hasSummary)
        assertTrue(summary.wordCount > 50)
        assertTrue(summary.executiveSummary.isNotBlank())
        assertFalse(summary.keyTakeaways.isEmpty())
        assertTrue(summary.keyTakeaways.size in 1..5)

        // Verify key metrics extracted (e.g. 45%, $12,500,000)
        assertTrue(summary.keyMetrics.any { it.contains("45%") || it.contains("12,500,000") })
    }

    @Test
    fun summarize_blankText_returnsEmptySummary() {
        val summary = DocumentSummarizer.summarize("")
        assertFalse(summary.hasSummary)
    }
}
