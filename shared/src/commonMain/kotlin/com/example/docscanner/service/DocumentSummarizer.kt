package com.example.docscanner.service

data class DocumentSummaryResult(
    val executiveSummary: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val keyMetrics: List<String> = emptyList(),
    val wordCount: Int = 0,
    val readingTimeMinutes: Int = 1
) {
    val hasSummary: Boolean
        get() = executiveSummary.isNotBlank() || keyTakeaways.isNotEmpty()
}

/**
 * 100% offline, on-device extractive summarizer and key takeaway extractor.
 * Multiplatform: Shared between Android and iOS.
 */
object DocumentSummarizer {

    private val STOP_WORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
        "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
        "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
        "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
        "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here",
        "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd",
        "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's",
        "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
        "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other",
        "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shan't",
        "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some",
        "such", "than", "that", "that's", "the", "their", "theirs", "them",
        "themselves", "then", "there", "there's", "these", "they", "they'd",
        "they'll", "they're", "they've", "this", "those", "through", "to", "too",
        "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll",
        "we're", "we've", "were", "weren't", "what", "what's", "when", "when's",
        "where", "where's", "which", "while", "who", "who's", "whom", "why",
        "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll",
        "you're", "you've", "your", "yours", "yourself", "yourselves"
    )

    private val METRIC_REGEX = Regex(
        """([$₹€£]\s*\d{1,3}(?:[,\s]\d{3})*(?:\.\d{2})?|\b\d+(?:\.\d+)?%|\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b|\b\d+(?:\.\d+)?\s*(?:kg|g|lbs|km|miles|pcs|units|hrs|hours|days|months|years)\b)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * 100% offline extractive summarization and key insight extraction.
     */
    fun summarize(ocrText: String): DocumentSummaryResult {
        if (ocrText.isBlank()) return DocumentSummaryResult()

        val allWords = ocrText.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val wordCount = allWords.size
        val readingTime = (wordCount / 200).coerceAtLeast(1)

        val rawSentences = splitIntoSentences(ocrText)
        if (rawSentences.isEmpty()) {
            return DocumentSummaryResult(wordCount = wordCount, readingTimeMinutes = readingTime)
        }

        val wordFreq = mutableMapOf<String, Int>()
        for (word in allWords) {
            val clean = word.lowercase().trim { !it.isLetterOrDigit() }
            if (clean.length >= 3 && clean !in STOP_WORDS) {
                wordFreq[clean] = (wordFreq[clean] ?: 0) + 1
            }
        }

        val scoredSentences = rawSentences.mapIndexed { index, sentence ->
            val words = sentence.split(Regex("""\s+""")).map { it.lowercase().trim { c -> !c.isLetterOrDigit() } }
            var score = 0.0
            for (w in words) {
                score += wordFreq[w] ?: 0
            }

            val len = words.size.coerceAtLeast(1)
            score /= kotlin.math.sqrt(len.toDouble())

            if (index < 2) {
                score *= 1.35
            }

            if (METRIC_REGEX.containsMatchIn(sentence)) {
                score *= 1.25
            }

            ScoredSentence(index = index, text = sentence.trim(), score = score)
        }

        val topSummary = scoredSentences
            .sortedByDescending { it.score }
            .take(3)
            .sortedBy { it.index }
            .map { it.text }

        val topTakeaways = scoredSentences
            .sortedByDescending { it.score }
            .take(5)
            .map { cleanBullet(it.text) }
            .distinct()

        val metrics = METRIC_REGEX.findAll(ocrText)
            .map { it.value.trim() }
            .distinct()
            .take(8)
            .toList()

        val executiveSummaryText = topSummary.joinToString(" ")

        return DocumentSummaryResult(
            executiveSummary = executiveSummaryText,
            keyTakeaways = topTakeaways,
            keyMetrics = metrics,
            wordCount = wordCount,
            readingTimeMinutes = readingTime
        )
    }

    private fun splitIntoSentences(text: String): List<String> {
        val normalized = text.replace("\r\n", "\n")
        val lines = normalized.split("\n").map { it.trim() }

        val sentences = mutableListOf<String>()
        val current = StringBuilder()

        for (line in lines) {
            if (line.isBlank()) {
                if (current.isNotBlank()) {
                    sentences.add(current.toString().trim())
                    current.clear()
                }
                continue
            }

            val sentenceParts = line.split(Regex("""(?<=[.!?])\s+"""))
            for (part in sentenceParts) {
                val trimmed = part.trim()
                if (trimmed.length in 20..350) {
                    sentences.add(trimmed)
                } else if (trimmed.isNotBlank()) {
                    current.append(" ").append(trimmed)
                    if (current.length >= 30) {
                        sentences.add(current.toString().trim())
                        current.clear()
                    }
                }
            }
        }

        if (current.isNotBlank() && current.length >= 20) {
            sentences.add(current.toString().trim())
        }

        return sentences.filter { it.length >= 20 }
    }

    private fun cleanBullet(sentence: String): String {
        var clean = sentence.trim()
        clean = clean.removePrefix("-").removePrefix("*").removePrefix("•").trim()
        if (clean.isNotEmpty() && clean.first().isLowerCase()) {
            clean = clean.replaceFirstChar { it.uppercase() }
        }
        return clean
    }

    private data class ScoredSentence(
        val index: Int,
        val text: String,
        val score: Double
    )
}
