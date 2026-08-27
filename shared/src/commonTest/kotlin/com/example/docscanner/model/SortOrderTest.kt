package com.example.docscanner.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SortOrderTest {

    private val docA = Document(
        id = "1",
        title = "Alpha",
        category = DocumentCategory.NOTE,
        createdAt = 1000L,
        modifiedAt = 1000L,
        pageCount = 1,
        thumbnailPath = "",
        pdfPath = "",
        extractedText = "",
        isPinned = false
    )

    private val docB = Document(
        id = "2",
        title = "Beta",
        category = DocumentCategory.RECEIPT,
        createdAt = 2000L,
        modifiedAt = 2000L,
        pageCount = 5,
        thumbnailPath = "",
        pdfPath = "",
        extractedText = "",
        isPinned = true // Pinned!
    )

    private val docC = Document(
        id = "3",
        title = "Gamma",
        category = DocumentCategory.CONTRACT,
        createdAt = 3000L,
        modifiedAt = 3000L,
        pageCount = 3,
        thumbnailPath = "",
        pdfPath = "",
        extractedText = "",
        isPinned = false
    )

    @Test
    fun testPinnedPrecedence() {
        val list = listOf(docA, docB, docC)
        val sorted = list.applySort(SortOrder.DATE_NEWEST)
        assertEquals("Beta", sorted.first().title, "Pinned doc must always come first")
    }

    @Test
    fun testSortDateDescending() {
        val list = listOf(docA, docC)
        val sorted = list.applySort(SortOrder.DATE_NEWEST)
        assertEquals("Gamma", sorted[0].title)
        assertEquals("Alpha", sorted[1].title)
    }

    @Test
    fun testSortDateAscending() {
        val list = listOf(docC, docA)
        val sorted = list.applySort(SortOrder.DATE_OLDEST)
        assertEquals("Alpha", sorted[0].title)
        assertEquals("Gamma", sorted[1].title)
    }

    @Test
    fun testSortNameAscending() {
        val list = listOf(docC, docA)
        val sorted = list.applySort(SortOrder.TITLE_AZ)
        assertEquals("Alpha", sorted[0].title)
        assertEquals("Gamma", sorted[1].title)
    }

    @Test
    fun testSortPagesDescending() {
        val list = listOf(docA, docC)
        val sorted = list.applySort(SortOrder.PAGE_COUNT)
        assertEquals("Gamma", sorted[0].title)
        assertEquals("Alpha", sorted[1].title)
    }
}
