package com.example.docscanner.model

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
        /** Primary lookup — uses enum name (stable across reordering). */
        fun fromName(name: String): DocumentCategory =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHER

        /** Legacy lookup by ordinal — only used during DB migration from v1→v2. */
        fun fromOrdinal(ordinal: Int): DocumentCategory =
            entries.getOrNull(ordinal) ?: OTHER

        /** Alias for fromName for cross-platform convenience. */
        fun fromString(value: String): DocumentCategory = fromName(value)
    }
}

