package com.example.docscanner.service

data class ExtractedContactData(
    val fullName: String? = null,
    val jobTitle: String? = null,
    val company: String? = null,
    val phone: String? = null,
    val alternatePhone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val address: String? = null,
    val rawText: String = ""
) {
    val hasData: Boolean
        get() = fullName != null || phone != null || email != null || company != null

    /**
     * Builds standard RFC 2426 vCard 3.0 format text.
     */
    fun toVCardString(): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:3.0")
        if (!fullName.isNullOrBlank()) {
            sb.appendLine("FN:$fullName")
            val parts = fullName.trim().split(Regex("""\s+"""))
            if (parts.size >= 2) {
                sb.appendLine("N:${parts.last()};${parts.dropLast(1).joinToString(" ")};;;")
            } else {
                sb.appendLine("N:$fullName;;;;")
            }
        }
        if (!company.isNullOrBlank()) {
            sb.appendLine("ORG:$company")
        }
        if (!jobTitle.isNullOrBlank()) {
            sb.appendLine("TITLE:$jobTitle")
        }
        if (!phone.isNullOrBlank()) {
            sb.appendLine("TEL;TYPE=CELL,VOICE:$phone")
        }
        if (!alternatePhone.isNullOrBlank()) {
            sb.appendLine("TEL;TYPE=WORK,VOICE:$alternatePhone")
        }
        if (!email.isNullOrBlank()) {
            sb.appendLine("EMAIL;TYPE=INTERNET,PREF:$email")
        }
        if (!website.isNullOrBlank()) {
            val url = if (website.startsWith("http://") || website.startsWith("https://")) website else "https://$website"
            sb.appendLine("URL:$url")
        }
        if (!address.isNullOrBlank()) {
            sb.appendLine("ADR;TYPE=WORK:;;$address;;;;")
        }
        sb.appendLine("END:VCARD")
        return sb.toString()
    }
}

/**
 * 100% offline, on-device business card parser extracting contacts into structured data and vCards.
 * Multiplatform: Shared between Android and iOS.
 */
object BusinessCardParser {

    private val EMAIL_REGEX = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")

    private val URL_REGEX = Regex(
        """\b(?:https?://)?(?:www\.)?([a-zA-Z0-9][-a-zA-Z0-9]*\.(?:com|org|net|io|in|co|ai|dev|app|biz|info|me|edu|gov|tech|xyz)(?:/[^\s]*)?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val PHONE_CANDIDATE_REGEX = Regex(
        """(?:(?:\+|00)\d{1,3}[-.\s]?)?(?:\(?\d{2,5}\)?[-.\s]?)?\d{3,5}[-.\s]?\d{3,5}"""
    )

    private val PHONE_PREFIXES = listOf("mobile", "cell", "tel", "phone", "ph", "m:", "t:", "p:", "call", "whatsapp", "fax")

    private val JOB_TITLE_KEYWORDS = listOf(
        "chief executive officer", "ceo", "chief technology officer", "cto",
        "chief financial officer", "cfo", "chief operating officer", "coo",
        "founder", "co-founder", "president", "vice president", "vp",
        "managing director", "executive director", "director", "general manager",
        "senior manager", "product manager", "project manager", "sales manager", "marketing manager",
        "operations manager", "account manager", "branch manager", "manager",
        "software engineer", "senior software engineer", "lead engineer", "system architect", "solution architect",
        "software developer", "developer", "engineer", "architect",
        "ui/ux designer", "product designer", "graphic designer", "designer",
        "senior consultant", "principal consultant", "consultant",
        "specialist", "analyst", "data scientist", "lead", "head of",
        "officer", "doctor", "dr.", "physician", "surgeon", "advocate",
        "attorney", "lawyer", "solicitor", "chartered accountant", "cpa", "accountant",
        "professor", "researcher", "scientist", "partner", "principal", "associate"
    )

    private val COMPANY_SUFFIXES = listOf(
        "pvt ltd", "private limited", "ltd", "limited", "inc", "incorporated",
        "llc", "corp", "corporation", "co.", "technologies", "technology",
        "solutions", "services", "enterprises", "group", "holdings", "ventures",
        "studios", "studio", "labs", "systems", "agency", "associates",
        "industries", "consulting", "foundation", "institute", "bank", "capital"
    )

    private val ADDRESS_KEYWORDS = listOf(
        "street", "st.", "road", "rd.", "avenue", "ave.", "boulevard", "blvd.",
        "suite", "ste", "floor", "building", "bldg", "block", "sector", "phase",
        "park", "plaza", "lane", "drive", "dr.", "cross", "nagar", "marg"
    )

    /**
     * Parses OCR text from a scanned business card into structured contact data.
     */
    fun parse(ocrText: String): ExtractedContactData {
        if (ocrText.isBlank()) return ExtractedContactData(rawText = ocrText)

        val rawLines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (rawLines.isEmpty()) return ExtractedContactData(rawText = ocrText)

        val email = extractEmail(rawLines)
        val website = extractWebsite(rawLines)
        val phones = extractPhones(rawLines)
        val primaryPhone = phones.getOrNull(0)
        val secondaryPhone = phones.getOrNull(1)
        val jobTitle = extractJobTitle(rawLines)
        val company = extractCompany(rawLines)
        val address = extractAddress(rawLines)
        val fullName = extractName(rawLines, setOfNotNull(jobTitle, company, email, website, primaryPhone, secondaryPhone, address))

        return ExtractedContactData(
            fullName = fullName,
            jobTitle = jobTitle,
            company = company,
            phone = primaryPhone,
            alternatePhone = secondaryPhone,
            email = email,
            website = website,
            address = address,
            rawText = ocrText
        )
    }

    private fun extractEmail(lines: List<String>): String? {
        for (line in lines) {
            val match = EMAIL_REGEX.find(line)
            if (match != null) {
                return match.value.lowercase().trim()
            }
        }
        return null
    }

    private fun extractWebsite(lines: List<String>): String? {
        for (line in lines) {
            if (line.contains("@")) continue
            val match = URL_REGEX.find(line)
            if (match != null) {
                val candidate = match.value.trim().removeSuffix("/").removeSuffix(".")
                if (!candidate.contains("@")) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun extractPhones(lines: List<String>): List<String> {
        val found = mutableListOf<String>()
        for (line in lines) {
            val lower = line.lowercase()
            val isExplicitPhone = PHONE_PREFIXES.any { lower.contains(it) }

            for (match in PHONE_CANDIDATE_REGEX.findAll(line)) {
                val digitsOnly = match.value.filter { it.isDigit() }
                if (digitsOnly.length in 8..15) {
                    val formatted = match.value.trim().removePrefix(":")
                    if (!found.contains(formatted)) {
                        if (isExplicitPhone) {
                            found.add(0, formatted)
                        } else {
                            found.add(formatted)
                        }
                    }
                }
            }
        }
        return found
    }

    private fun extractJobTitle(lines: List<String>): String? {
        for (line in lines) {
            val lower = line.lowercase().trim()
            for (title in JOB_TITLE_KEYWORDS) {
                if (lower == title || lower.startsWith("$title ") || lower.endsWith(" $title") || lower.contains(" $title ")) {
                    return line.trim()
                }
            }
        }
        return null
    }

    private fun extractCompany(lines: List<String>): String? {
        for (line in lines) {
            val lower = line.lowercase().trim()
            for (suffix in COMPANY_SUFFIXES) {
                if (lower.contains(suffix)) {
                    return line.trim()
                }
            }
        }
        return null
    }

    private fun extractAddress(lines: List<String>): String? {
        val addressParts = mutableListOf<String>()
        for (line in lines) {
            val lower = line.lowercase()
            if (ADDRESS_KEYWORDS.any { lower.contains(it) } || line.contains(Regex("""\b\d{5,6}\b"""))) {
                if (!line.contains("@") && !URL_REGEX.containsMatchIn(line)) {
                    addressParts.add(line.trim())
                }
            }
        }
        return if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else null
    }

    private fun extractName(lines: List<String>, excludedFields: Set<String>): String? {
        for (line in lines.take(6)) {
            val trimmed = line.trim()
            if (trimmed.length in 3..40 && !trimmed.contains("@") && !trimmed.contains("http")) {
                if (trimmed.any { it.isDigit() }) continue
                if (excludedFields.contains(trimmed)) continue

                val words = trimmed.split(Regex("""\s+"""))
                if (words.size in 2..4) {
                    val isCapitalized = words.all { it.firstOrNull()?.isUpperCase() == true }
                    val lower = trimmed.lowercase()
                    val isJobOrCompany = JOB_TITLE_KEYWORDS.any { lower.contains(it) } || COMPANY_SUFFIXES.any { lower.contains(it) }
                    if (isCapitalized && !isJobOrCompany) {
                        return trimmed
                    }
                }
            }
        }
        return null
    }
}
