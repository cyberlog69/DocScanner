package com.example.docscanner.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentModelTest {

    @Test
    fun testTagSerialization() {
        val doc = Document(
            id = "test-doc",
            title = "Receipt Tax 2026",
            category = DocumentCategory.RECEIPT,
            createdAt = 1000L,
            modifiedAt = 1000L,
            pageCount = 1,
            thumbnailPath = "",
            pdfPath = "",
            extractedText = "",
            tags = listOf("tax", "2026", "groceries")
        )

        val dbString = doc.tagsToDbString()
        assertEquals("tax,2026,groceries", dbString)

        val parsed = Document.parseTagsString(dbString)
        assertEquals(listOf("tax", "2026", "groceries"), parsed)
    }

    @Test
    fun testTagSerializationEmpty() {
        val parsed = Document.parseTagsString("")
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun testCategoryLookup() {
        assertEquals(DocumentCategory.RECEIPT, DocumentCategory.fromName("RECEIPT"))
        assertEquals(DocumentCategory.NOTE, DocumentCategory.fromName("NOTE"))
        assertEquals(DocumentCategory.OTHER, DocumentCategory.fromName("UNKNOWN_CATEGORY"))
    }
}
