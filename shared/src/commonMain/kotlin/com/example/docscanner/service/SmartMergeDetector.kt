package com.example.docscanner.service

import com.example.docscanner.model.Document
import com.example.docscanner.model.DocumentCategory
import kotlin.math.abs

/**
 * A suggested merge grouping detecting documents scanned in the same workflow session.
 *
 * @property id Unique ID identifying this suggestion.
 * @property primaryDocument The earlier or anchor document.
 * @property candidateDocuments Additional documents from the same scanning session to merge into [primaryDocument].
 * @property reason Human-readable explanation of why these documents were grouped.
 * @property timeDiffMinutes Elapsed time between the scans in minutes.
 * @property confidence Confidence score between 0.0 and 1.0.
 */
data class MergeSuggestion(
    val id: String,
    val primaryDocument: Document,
    val candidateDocuments: List<Document>,
    val reason: String,
    val timeDiffMinutes: Long,
    val confidence: Float
) {
    val allDocuments: List<Document> get() = listOf(primaryDocument) + candidateDocuments
    val totalPageCount: Int get() = allDocuments.sumOf { it.pageCount.coerceAtLeast(1) }
}

/**
 * Pure Kotlin detector that analyzes a user's document repository to detect
 * documents created in close temporal proximity (e.g. same scanning session)
 * with matching categories or naming patterns.
 */
object SmartMergeDetector {

    /** Maximum time difference (in milliseconds) to consider scans part of the same session. Default: 25 mins. */
    const val DEFAULT_SESSION_WINDOW_MS = 25 * 60 * 1000L

    /**
     * Evaluates a collection of [documents] and returns active merge suggestions.
     * Excludes vaulted documents and already-merged multi-page documents (pageCount >= 10).
     */
    fun findMergeSuggestions(
        documents: List<Document>,
        sessionWindowMs: Long = DEFAULT_SESSION_WINDOW_MS
    ): List<MergeSuggestion> {
        // Only evaluate non-vaulted documents
        val eligible = documents
            .filter { !it.isVault }
            .sortedBy { it.createdAt }

        if (eligible.size < 2) return emptyList()

        val suggestions = mutableListOf<MergeSuggestion>()
        val groupedDocIds = mutableSetOf<String>()

        for (i in eligible.indices) {
            val docA = eligible[i]
            if (docA.id in groupedDocIds) continue

            val group = mutableListOf<Document>()
            var latestCreatedAt = docA.createdAt

            for (j in (i + 1) until eligible.size) {
                val docB = eligible[j]
                if (docB.id in groupedDocIds) continue

                val timeDiff = abs(docB.createdAt - latestCreatedAt)
                if (timeDiff <= sessionWindowMs) {
                    // Check similarity signals:
                    // 1. Same category
                    // 2. Or default scan naming pattern (e.g., both contain "Scan" or "Doc")
                    // 3. Or both have 1-2 pages (standard single-page captures waiting to be collated)
                    val sameCategory = docA.category == docB.category && docA.category != DocumentCategory.OTHER
                    val singlePageScans = docA.pageCount <= 2 && docB.pageCount <= 2
                    val titlePatternMatch = isSimilarTitle(docA.title, docB.title)

                    if (sameCategory || singlePageScans || titlePatternMatch) {
                        group.add(docB)
                        latestCreatedAt = docB.createdAt
                    }
                } else {
                    // Since list is sorted by createdAt, further docs may exceed the window
                    // unless chaining, so we can break when gap is too large
                    break
                }
            }

            if (group.isNotEmpty()) {
                val totalTimeDiffMinutes = (group.last().createdAt - docA.createdAt) / 60000L
                val reason = when {
                    docA.category == group.first().category && docA.category != DocumentCategory.OTHER ->
                        "Scanned ${totalTimeDiffMinutes}m apart with matching category (${docA.category.displayName})"
                    docA.pageCount <= 1 && group.all { it.pageCount <= 1 } ->
                        "Consecutive single-page scans in the same session (${totalTimeDiffMinutes}m apart)"
                    else ->
                        "Similar documents scanned in the same session (${totalTimeDiffMinutes}m apart)"
                }

                val confidence = when {
                    docA.category == group.first().category && docA.category != DocumentCategory.OTHER -> 0.95f
                    docA.pageCount <= 1 && group.size == 1 -> 0.85f
                    else -> 0.75f
                }

                suggestions.add(
                    MergeSuggestion(
                        id = "merge_${docA.id}",
                        primaryDocument = docA,
                        candidateDocuments = group,
                        reason = reason,
                        timeDiffMinutes = totalTimeDiffMinutes.coerceAtLeast(1L),
                        confidence = confidence
                    )
                )

                groupedDocIds.add(docA.id)
                group.forEach { groupedDocIds.add(it.id) }
            }
        }

        return suggestions
    }

    private fun isSimilarTitle(titleA: String, titleB: String): Boolean {
        val cleanA = titleA.replace(Regex("\\d"), "").trim()
        val cleanB = titleB.replace(Regex("\\d"), "").trim()
        return cleanA.isNotBlank() && cleanA.equals(cleanB, ignoreCase = true)
    }
}
