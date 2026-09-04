package com.example.docscanner.service

data class ExtractedReceiptData(
    val merchantName: String? = null,
    val date: String? = null,
    val totalAmount: String? = null,
    val taxAmount: String? = null,
    val invoiceNumber: String? = null,
    val rawText: String = ""
) {
    val hasData: Boolean
        get() = merchantName != null || date != null || totalAmount != null || invoiceNumber != null
}

object ReceiptParser {

    private val DATE_PATTERNS = listOf(
        Regex("""\b(\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4})\b"""),
        Regex("""\b(\d{4}[/.-]\d{1,2}[/.-]\d{1,2})\b"""),
        Regex("""\b(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4})\b""", RegexOption.IGNORE_CASE),
        Regex("""\b((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2},?\s+\d{2,4})\b""", RegexOption.IGNORE_CASE)
    )

    private val TOTAL_KEYWORDS = listOf(
        "grand total", "total amount", "net amount", "total due", "amount due", "balance due", "total payable", "total"
    )

    private val TAX_KEYWORDS = listOf(
        "gstin", "gst amount", "tax amount", "cgst", "sgst", "vat amount", "sales tax", "tax"
    )

    private val INVOICE_PATTERNS = listOf(
        Regex("""(?:invoice|inv|bill|receipt|order)\s*(?:no|number|#)?[:.\s]*([a-zA-Z0-9\-_/]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:inv|bill|rec)[#:\s]+([a-zA-Z0-9\-_/]+)""", RegexOption.IGNORE_CASE)
    )

    private val AMOUNT_REGEX = Regex("""([$₹€£]?\s*\d{1,3}(?:[,\s]\d{3})*(?:\.\d{2})|\d+(?:\.\d{2}))""")

    /**
     * Parses the given OCR text and extracts key financial and metadata fields.
     */
    fun parse(ocrText: String): ExtractedReceiptData {
        if (ocrText.isBlank()) return ExtractedReceiptData(rawText = ocrText)

        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ExtractedReceiptData(rawText = ocrText)

        val merchant = extractMerchant(lines)
        val date = extractDate(lines)
        val total = extractTotal(lines)
        val tax = extractTax(lines)
        val invoiceNo = extractInvoiceNumber(lines)

        return ExtractedReceiptData(
            merchantName = merchant,
            date = date,
            totalAmount = total,
            taxAmount = tax,
            invoiceNumber = invoiceNo,
            rawText = ocrText
        )
    }

    private fun extractMerchant(lines: List<String>): String? {
        val noiseWords = setOf(
            "tax invoice", "invoice", "receipt", "cash memo", "bill of supply", "customer copy",
            "original", "duplicate", "welcome", "tel", "phone", "phone:", "ph:", "email",
            "gstin", "pan", "date", "time", "order", "table", "cashier"
        )

        for (line in lines.take(5)) {
            val lower = line.lowercase().trim()
            if (noiseWords.any { lower.startsWith(it) || lower == it }) continue
            if (line.length in 3..45 && !line.contains(Regex("""\d{5,}"""))) {
                return line.trim()
            }
        }
        return null
    }

    private fun extractDate(lines: List<String>): String? {
        for (line in lines) {
            for (pattern in DATE_PATTERNS) {
                val match = pattern.find(line)
                if (match != null) {
                    return match.groupValues[1].trim()
                }
            }
        }
        return null
    }

    private fun extractTotal(lines: List<String>): String? {
        // Look for explicit total keywords (iterating backwards since totals are near bottom)
        for (line in lines.asReversed()) {
            val lower = line.lowercase()
            for (keyword in TOTAL_KEYWORDS) {
                if (lower.contains(keyword)) {
                    val match = AMOUNT_REGEX.find(line)
                    if (match != null) {
                        return match.value.trim()
                    }
                }
            }
        }
        return null
    }

    private fun extractTax(lines: List<String>): String? {
        for (line in lines) {
            val lower = line.lowercase()
            for (keyword in TAX_KEYWORDS) {
                if (lower.contains(keyword)) {
                    // Try to find a currency/number amount first
                    val amountMatch = AMOUNT_REGEX.find(line)
                    if (amountMatch != null) {
                        return amountMatch.value.trim()
                    }
                    // Or match GSTIN identification pattern (15 alphanumerics in India)
                    val gstinMatch = Regex("""\b\d{2}[A-Z]{5}\d{4}[A-Z]{1}[A-Z\d]{1}[Z]{1}[A-Z\d]{1}\b""").find(line)
                    if (gstinMatch != null) {
                        return gstinMatch.value
                    }
                }
            }
        }
        return null
    }

    private fun extractInvoiceNumber(lines: List<String>): String? {
        for (line in lines) {
            for (pattern in INVOICE_PATTERNS) {
                val match = pattern.find(line)
                if (match != null) {
                    val candidate = match.groupValues[1].trim()
                    if (candidate.length in 3..25) {
                        return candidate
                    }
                }
            }
        }
        return null
    }
}
