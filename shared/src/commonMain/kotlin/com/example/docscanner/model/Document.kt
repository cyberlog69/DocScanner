package com.example.docscanner.model

data class Document(
    val id: String,
    val title: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val pageCount: Int,
    val thumbnailPath: String,
    val pdfPath: String,
    val extractedText: String,
    val category: DocumentCategory = DocumentCategory.OTHER,
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList()
) {
    companion object {
        fun tagsToDbString(tags: List<String>): String = tags.joinToString(",")

        fun parseTagsString(raw: String): List<String> {
            if (raw.isBlank()) return emptyList()
            return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}

data class Page(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val imagePath: String,
    val extractedText: String = "",
    val width: Int = 0,
    val height: Int = 0
)
