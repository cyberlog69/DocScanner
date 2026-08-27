package com.example.docscanner.model

/**
 * Smart, 100% offline rule-based category classifier using OCR extracted text.
 * Analyzes keywords and domain-specific terms with weighted scoring to suggest or auto-assign document categories.
 * 100% pure Kotlin — identical behavior across Android and iOS.
 */
object CategoryClassifier {

    private val invoiceKeywords = listOf(
        "tax invoice" to 5, "gstin" to 5, "invoice no" to 4, "invoice date" to 4,
        "bill to" to 3, "ship to" to 3, "subtotal" to 3, "taxable amount" to 3,
        "cgst" to 4, "sgst" to 4, "igst" to 4, "hsn/sac" to 4, "invoice" to 2,
        "total amount" to 2, "due date" to 2, "balance due" to 3, "po number" to 3
    )

    private val receiptKeywords = listOf(
        "cash receipt" to 5, "sales receipt" to 5, "payment receipt" to 4,
        "pos transaction" to 4, "change due" to 4, "amount paid" to 3,
        "payment method" to 3, "card ending" to 3, "merchant" to 3, "cashier" to 3,
        "tendered" to 3, "total" to 1, "paid" to 2, "receipt" to 2, "order total" to 2,
        "store #" to 3, "terminal #" to 3, "items sold" to 3
    )

    private val idCardKeywords = listOf(
        "aadhaar" to 5, "aadhar" to 5, "unique identification authority" to 6,
        "income tax department" to 5, "permanent account number" to 5, "pan card" to 5,
        "driving licence" to 5, "driving license" to 5, "motor vehicles department" to 5,
        "election commission of india" to 5, "voter identity" to 5, "elector's photo" to 5,
        "passport" to 5, "republic of india" to 4, "date of birth" to 3, "dob" to 3,
        "father's name" to 3, "gender" to 2, "identity card" to 4, "national id" to 4,
        "citizen" to 2, "valid until" to 2, "issue date" to 2
    )

    private val contractKeywords = listOf(
        "agreement" to 4, "memorandum of understanding" to 5, "mou" to 4,
        "terms and conditions" to 4, "in witness whereof" to 5, "whereas" to 4,
        "parties hereto" to 4, "party of the first part" to 5, "clause" to 3,
        "signed and delivered" to 4, "tenancy agreement" to 5, "lease deed" to 5,
        "employment agreement" to 5, "non-disclosure agreement" to 5, "nda" to 4,
        "jurisdiction" to 3, "arbitration" to 3, "indemnification" to 4, "termination" to 2
    )

    private val noteKeywords = listOf(
        "meeting notes" to 5, "minutes of meeting" to 5, "lecture notes" to 4,
        "action items" to 4, "todo" to 3, "to-do" to 3, "agenda" to 3,
        "summary" to 2, "memo" to 3, "brainstorming" to 3, "key points" to 3,
        "study notes" to 4, "handwritten" to 2, "discussion" to 2
    )

    private val bookKeywords = listOf(
        "table of contents" to 5, "chapter" to 3, "preface" to 4, "foreword" to 4,
        "published by" to 4, "all rights reserved" to 3, "isbn" to 5, "edition" to 3,
        "bibliography" to 4, "index" to 3, "volume" to 3, "author" to 2,
        "prologue" to 4, "epilogue" to 4
    )

    /**
     * Classifies the given [extractedText] and returns the most appropriate [DocumentCategory].
     * If no strong match is found, returns [fallbackCategory].
     */
    fun classify(extractedText: String, fallbackCategory: DocumentCategory = DocumentCategory.OTHER): DocumentCategory {
        if (extractedText.isBlank()) return fallbackCategory

        val lowerText = extractedText.lowercase()

        val scores = mutableMapOf<DocumentCategory, Int>()

        scores[DocumentCategory.INVOICE] = calculateScore(lowerText, invoiceKeywords)
        scores[DocumentCategory.RECEIPT] = calculateScore(lowerText, receiptKeywords)
        scores[DocumentCategory.ID_CARD] = calculateScore(lowerText, idCardKeywords)
        scores[DocumentCategory.CONTRACT] = calculateScore(lowerText, contractKeywords)
        scores[DocumentCategory.NOTE] = calculateScore(lowerText, noteKeywords)
        scores[DocumentCategory.BOOK] = calculateScore(lowerText, bookKeywords)

        val bestMatch = scores.maxByOrNull { it.value }

        // Require a minimum threshold score of 3 to avoid false positives
        return if (bestMatch != null && bestMatch.value >= 3) {
            bestMatch.key
        } else {
            fallbackCategory
        }
    }

    private fun calculateScore(text: String, keywords: List<Pair<String, Int>>): Int {
        var score = 0
        for ((keyword, weight) in keywords) {
            if (text.contains(keyword)) {
                score += weight
            }
        }
        return score
    }
}

