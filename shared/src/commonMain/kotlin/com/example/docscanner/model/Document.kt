package com.example.docscanner.model

/**
 * Represents a scanned document with one or more pages.
 */
data class Document(
    val id: String,
    val title: String,
    val category: DocumentCategory = DocumentCategory.OTHER,
    val createdAt: Long = 0L,
    val modifiedAt: Long = 0L,
    val pageCount: Int = 0,
    val thumbnailPath: String = "",
    val pdfPath: String = "",
    val extractedText: String = "",
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList()
) {
    /** Formats tags as comma-separated string for SQLite / storage. */
    fun tagsToDbString(): String = tags.joinToString(",") { it.trim() }

    companion object {
        fun tagsToDbString(tags: List<String>): String = tags.joinToString(",") { it.trim() }

        /** Parses comma-separated tags string. */
        fun parseTagsString(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}

/**
 * Represents a single scanned page within a document.
 */
data class Page(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val imagePath: String,
    val originalImagePath: String = imagePath,
    val extractedText: String = "",
    val createdAt: Long = 0L,
    val width: Int = 0,
    val height: Int = 0
)

