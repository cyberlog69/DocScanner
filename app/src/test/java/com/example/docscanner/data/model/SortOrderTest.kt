package com.example.docscanner.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SortOrderTest {

    private fun doc(
        id: String,
        title: String,
        createdAt: Long,
        pageCount: Int,
        isPinned: Boolean = false
    ) = Document(
        id = id,
        title = title,
        category = DocumentCategory.OTHER,
        createdAt = createdAt,
        modifiedAt = createdAt,
        pageCount = pageCount,
        isPinned = isPinned
    )

    @Test
    fun pinnedDocumentsAlwaysStayOnTop() {
        val oldPinned = doc("1", "Old", createdAt = 100, pageCount = 1, isPinned = true)
        val newUnpinned = doc("2", "New", createdAt = 500, pageCount = 1)

        val sorted = listOf(newUnpinned, oldPinned).applySort(SortOrder.DATE_NEWEST)
        assertEquals(listOf(oldPinned, newUnpinned), sorted)
    }

    @Test
    fun sortByNewestFirst() {
        val older = doc("1", "A", createdAt = 100, pageCount = 1)
        val newer = doc("2", "B", createdAt = 200, pageCount = 1)

        assertEquals(listOf(newer, older), listOf(older, newer).applySort(SortOrder.DATE_NEWEST))
        assertEquals(listOf(older, newer), listOf(newer, older).applySort(SortOrder.DATE_OLDEST))
    }

    @Test
    fun sortByTitleCaseInsensitive() {
        val a = doc("1", "apple", createdAt = 1, pageCount = 1)
        val z = doc("2", "Zebra", createdAt = 2, pageCount = 1)

        assertEquals(listOf(a, z), listOf(z, a).applySort(SortOrder.TITLE_AZ))
        assertEquals(listOf(z, a), listOf(a, z).applySort(SortOrder.TITLE_ZA))
    }

    @Test
    fun sortByPageCountHighToLow() {
        val small = doc("1", "S", createdAt = 1, pageCount = 1)
        val large = doc("2", "L", createdAt = 2, pageCount = 20)

        assertEquals(listOf(large, small), listOf(small, large).applySort(SortOrder.PAGE_COUNT))
    }
}