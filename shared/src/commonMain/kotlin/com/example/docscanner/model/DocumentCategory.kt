package com.example.docscanner.model

enum class DocumentCategory(val displayName: String, val emoji: String) {
    ALL("All", "📁"),
    RECEIPT("Receipt", "🧾"),
    ID_CARD("ID Card", "🪪"),
    NOTES("Notes", "📝"),
    CONTRACT("Contract", "📜"),
    INVOICE("Invoice", "💼"),
    BOOK("Book", "📖"),
    OTHER("Other", "📄");

    companion object {
        fun fromString(value: String): DocumentCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
