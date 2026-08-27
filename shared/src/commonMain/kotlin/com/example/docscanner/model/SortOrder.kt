package com.example.docscanner.model

enum class SortOrder(val displayName: String) {
    DATE_NEWEST("Date (Newest first)"),
    DATE_OLDEST("Date (Oldest first)"),
    TITLE_AZ("Title (A to Z)"),
    TITLE_ZA("Title (Z to A)"),
    PAGE_COUNT("Page count (High to Low)");

    companion object {
        val DEFAULT = DATE_NEWEST

        fun fromString(value: String): SortOrder {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
        }
    }
}

/**
 * Sorts a list of documents according to [sortOrder], maintaining pinned documents at the top.
 */
fun List<Document>.applySort(sortOrder: SortOrder): List<Document> {
    val (pinned, unpinned) = this.partition { it.isPinned }
    val comparator = when (sortOrder) {
        SortOrder.DATE_NEWEST -> compareByDescending<Document> { it.createdAt }
        SortOrder.DATE_OLDEST -> compareBy<Document> { it.createdAt }
        SortOrder.TITLE_AZ -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        SortOrder.TITLE_ZA -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }
        SortOrder.PAGE_COUNT -> compareByDescending<Document> { it.pageCount }
    }
    return pinned.sortedWith(comparator) + unpinned.sortedWith(comparator)
}

