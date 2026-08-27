package com.example.docscanner.data.model

import com.example.docscanner.model.applySort as kmpApplySort

/**
 * Re-exported from KMP :shared module (com.example.docscanner.model)
 * Single source of truth for SortOrder across Android & iOS.
 */
typealias SortOrder = com.example.docscanner.model.SortOrder

fun List<Document>.applySort(sortOrder: SortOrder): List<Document> =
    this.kmpApplySort(sortOrder)



