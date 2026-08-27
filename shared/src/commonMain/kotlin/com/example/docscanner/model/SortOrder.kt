package com.example.docscanner.model

enum class SortOrder(val displayName: String) {
    DATE_DESC("Date (Newest first)"),
    DATE_ASC("Date (Oldest first)"),
    TITLE_ASC("Title (A to Z)"),
    TITLE_DESC("Title (Z to A)"),
    PAGE_COUNT_DESC("Pages (High to Low)");

    companion object {
        fun fromString(value: String): SortOrder {
            return entries.firstOrNull { it.name == value } ?: DATE_DESC
        }
    }
}
