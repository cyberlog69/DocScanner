package com.example.docscanner.model

/**
 * Fast, offline keyword-heuristic document classifier.
 * Scans OCR text and suggests the most accurate DocumentCategory.
 * 100% pure Kotlin — identical behavior across Android and iOS.
 */
object CategoryClassifier {

    private val INVOICE_KEYWORDS = listOf(
        "invoice", "tax invoice", "bill to", "invoice date", "due date",
        "total amount", "gst", "vat", "subtotal", "balance due", "itemized",
        "remit to", "hsn", "sac code", "cgst", "sgst", "igst"
    )

    private val RECEIPT_KEYWORDS = listOf(
        "receipt", "cash receipt", "payment receipt", "change due", "cashier",
        "terminal", "card ending", "pos", "store #", "merchant #",
        "thank you for shopping", "sale transaction", "items sold", "qty"
    )

    private val ID_CARD_KEYWORDS = listOf(
        "identity", "identity card", "national id", "passport", "driving licence",
        "driver license", "driver's license", "date of birth", "dob", "sex",
        "nationality", "blood group", "citizen", "expiry date", "valid until",
        "aadhaar", "pan card", "voter id", "ssn", "social security"
    )

    private val CONTRACT_KEYWORDS = listOf(
        "agreement", "contract", "terms and conditions", "party of the first part",
        "hereby agree", "in witness whereof", "confidentiality", "non-disclosure",
        "nda", "lease agreement", "tenancy", "employment agreement", "indemnification",
        "jurisdiction", "signatures", "signed by", "witnesseth"
    )

    private val NOTES_KEYWORDS = listOf(
        "notes", "meeting notes", "minutes of meeting", "agenda", "action items",
        "todo", "to-do", "brainstorming", "key takeaways", "discussion",
        "summary", "objectives", "follow up", "memo"
    )

    private val BOOK_KEYWORDS = listOf(
        "chapter", "prologue", "epilogue", "contents", "table of contents",
        "author", "published by", "isbn", "copyright", "all rights reserved",
        "preface", "acknowledgments", "bibliography", "index", "volume", "edition"
    )

    fun classify(extractedText: String): DocumentCategory {
        if (extractedText.isBlank()) return DocumentCategory.OTHER

        val lower = extractedText.lowercase()

        val invoiceScore = countMatches(lower, INVOICE_KEYWORDS)
        val receiptScore = countMatches(lower, RECEIPT_KEYWORDS)
        val idScore = countMatches(lower, ID_CARD_KEYWORDS)
        val contractScore = countMatches(lower, CONTRACT_KEYWORDS)
        val notesScore = countMatches(lower, NOTES_KEYWORDS)
        val bookScore = countMatches(lower, BOOK_KEYWORDS)

        val scores = listOf(
            DocumentCategory.INVOICE to invoiceScore,
            DocumentCategory.RECEIPT to receiptScore,
            DocumentCategory.ID_CARD to idScore,
            DocumentCategory.CONTRACT to contractScore,
            DocumentCategory.NOTES to notesScore,
            DocumentCategory.BOOK to bookScore
        )

        val best = scores.maxByOrNull { it.second } ?: return DocumentCategory.OTHER

        return if (best.second > 0) best.first else DocumentCategory.OTHER
    }

    private fun countMatches(text: String, keywords: List<String>): Int {
        var count = 0
        for (kw in keywords) {
            if (text.contains(kw)) {
                count += if (kw.contains(" ")) 2 else 1
            }
        }
        return count
    }
}
