package com.example.docscanner.data.model

/**
 * Represents a scanned document with one or more pages.
 */
data class Document(
    val id: String,
    val title: String,
    val category: DocumentCategory,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 0,
    val thumbnailPath: String = "",
    val pdfPath: String = "",
    val extractedText: String = "" // Full OCR text for search fallback
)

enum class DocumentCategory(val displayName: String, val emoji: String) {
    ALL("All", "📄"),
    RECEIPT("Receipts", "🧾"),
    ID_CARD("ID Cards", "🪪"),
    NOTE("Notes", "📝"),
    CONTRACT("Contracts", "📋"),
    INVOICE("Invoices", "💼"),
    BOOK("Books", "📚"),
    OTHER("Other", "📎");

    companion object {
        fun fromOrdinal(ordinal: Int): DocumentCategory = entries.getOrNull(ordinal) ?: OTHER
    }
}
