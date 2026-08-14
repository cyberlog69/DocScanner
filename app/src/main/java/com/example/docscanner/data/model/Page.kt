package com.example.docscanner.data.model

/**
 * Represents a single scanned page within a document.
 */
data class Page(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val imagePath: String,       // Path to perspective-corrected image
    val originalImagePath: String, // Path to original captured image
    val extractedText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
