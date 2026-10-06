package com.example.docscanner.service

import com.example.docscanner.model.Document
import com.example.docscanner.model.DocumentCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartMergeDetectorTest {

    private fun createDoc(
        id: String,
        title: String,
        createdAt: Long,
        category: DocumentCategory = DocumentCategory.RECEIPT,
        pageCount: Int = 1,
        isVault: Boolean = false
    ): Document {
        return Document(
            id = id,
            title = title,
            createdAt = createdAt,
            modifiedAt = createdAt,
            pageCount = pageCount,
            thumbnailPath = "",
            pdfPath = "",
            extractedText = "",
            category = category,
            isVault = isVault
        )
    }

    @Test
    fun testDetectsNearbyScansWithMatchingCategory() {
        val baseTime = 1770000000000L
        val doc1 = createDoc("1", "Invoice 101", baseTime, DocumentCategory.INVOICE)
        val doc2 = createDoc("2", "Invoice 102", baseTime + 3 * 60 * 1000L, DocumentCategory.INVOICE)

        val suggestions = SmartMergeDetector.findMergeSuggestions(listOf(doc1, doc2))
        assertEquals(1, suggestions.size)
        val suggestion = suggestions.first()
        assertEquals("1", suggestion.primaryDocument.id)
        assertEquals(1, suggestion.candidateDocuments.size)
        assertEquals("2", suggestion.candidateDocuments.first().id)
        assertEquals(2, suggestion.allDocuments.size)
        assertTrue(suggestion.confidence >= 0.85f)
        assertTrue(suggestion.reason.contains("Invoice", ignoreCase = true))
    }

    @Test
    fun testIgnoresScansOutsideSessionWindow() {
        val baseTime = 1770000000000L
        val doc1 = createDoc("1", "Scan 1", baseTime)
        // 40 minutes later (default window is 25 minutes)
        val doc2 = createDoc("2", "Scan 2", baseTime + 40 * 60 * 1000L)

        val suggestions = SmartMergeDetector.findMergeSuggestions(listOf(doc1, doc2))
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun testExcludesVaultedDocuments() {
        val baseTime = 1770000000000L
        val doc1 = createDoc("1", "Secret 1", baseTime, isVault = true)
        val doc2 = createDoc("2", "Secret 2", baseTime + 2 * 60 * 1000L, isVault = true)

        val suggestions = SmartMergeDetector.findMergeSuggestions(listOf(doc1, doc2))
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun testSingleDocumentReturnsEmpty() {
        val doc1 = createDoc("1", "Solo Scan", 1770000000000L)
        val suggestions = SmartMergeDetector.findMergeSuggestions(listOf(doc1))
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun testDetectsSimilarTitlesAcrossSession() {
        val baseTime = 1770000000000L
        val doc1 = createDoc("1", "Scan 1", baseTime, DocumentCategory.OTHER)
        val doc2 = createDoc("2", "Scan 2", baseTime + 1 * 60 * 1000L, DocumentCategory.OTHER)

        val suggestions = SmartMergeDetector.findMergeSuggestions(listOf(doc1, doc2))
        assertEquals(1, suggestions.size)
        assertEquals(2, suggestions.first().allDocuments.size)
    }
}
