package com.example.docscanner.data.model

/**
 * Re-exported from KMP :shared module (com.example.docscanner.model)
 * Single source of truth for SortOrder across Android & iOS.
 */
typealias SortOrder = com.example.docscanner.model.SortOrder

fun List<Document>.applySort(sortOrder: SortOrder): List<Document> =
    com.example.docscanner.model.applySort(sortOrder)


