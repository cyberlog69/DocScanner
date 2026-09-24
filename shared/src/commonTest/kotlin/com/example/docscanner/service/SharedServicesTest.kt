package com.example.docscanner.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SharedServicesTest {

    @Test
    fun testReceiptParserExtractsData() {
        val ocrSample = """
            Starbucks Coffee #1042
            123 Market Street, San Francisco, CA
            Date: 12/04/2025
            Invoice No: INV-98472
            1 Caffe Latte       $4.75
            1 Blueberry Muffin  $3.25
            Tax: $0.64
            Total Amount: $8.64
            Thank you for visiting!
        """.trimIndent()

        val parsed = ReceiptParser.parse(ocrSample)
        assertTrue(parsed.hasData)
        assertNotNull(parsed.merchantName)
        assertTrue(parsed.merchantName!!.contains("Starbucks", ignoreCase = true))
        assertEquals("12/04/2025", parsed.date)
        assertEquals("INV-98472", parsed.invoiceNumber)
        assertNotNull(parsed.totalAmount)
        assertTrue(parsed.totalAmount!!.contains("8.64"))
    }

    @Test
    fun testBusinessCardParserExtractsContactAndVCard() {
        val ocrSample = """
            Johnathan Doe
            Senior Software Architect
            Acme Technologies Inc
            Phone: +1 555-234-5678
            Email: john.doe@acme.com
            Website: https://acme.com
            100 Innovation Way, Austin, TX
        """.trimIndent()

        val contact = BusinessCardParser.parse(ocrSample)
        assertTrue(contact.hasData)
        assertEquals("Johnathan Doe", contact.fullName)
        assertEquals("Senior Software Architect", contact.jobTitle)
        assertEquals("Acme Technologies Inc", contact.company)
        assertEquals("john.doe@acme.com", contact.email)

        val vCard = contact.toVCardString()
        assertTrue(vCard.contains("BEGIN:VCARD"))
        assertTrue(vCard.contains("FN:Johnathan Doe"))
        assertTrue(vCard.contains("EMAIL;TYPE=INTERNET,PREF:john.doe@acme.com"))
        assertTrue(vCard.contains("END:VCARD"))
    }

    @Test
    fun testTableExtractorDetectsPipeTable() {
        val tableText = """
            | Item | Quantity | Price |
            | Apple | 10 | $15.00 |
            | Orange | 5 | $8.50 |
            | Banana | 12 | $6.00 |
        """.trimIndent()

        val table = TableExtractor.extractTable(tableText)
        assertTrue(table.hasTable)
        assertEquals(3, table.columnCount)
        assertEquals(3, table.rows.size)
        assertEquals(listOf("Item", "Quantity", "Price"), table.headers)
        assertTrue(table.csvContent.contains("Apple,10,$15.00"))
    }

    @Test
    fun testDocumentSummarizerProducesSummary() {
        val text = """
            Quarterly Business Review Q3 2026.
            DocScanner recorded massive growth across both Android and iOS platforms with over 150,000 active installations.
            The offline OCR engine processed 1,200,000 document pages with 99.4% accuracy across Latin and Devanagari scripts.
            Security enhancements included hardware-backed AES-256 GCM vault storage with biometric authentication.
            Looking forward into next quarter, our key priority is complete KMP parity and native Apache PDFBox integration.
            Total revenue growth reached 34% year-over-year with zero data egress costs.
        """.trimIndent()

        val summary = DocumentSummarizer.summarize(text)
        assertTrue(summary.hasSummary)
        assertTrue(summary.wordCount > 30)
        assertTrue(summary.executiveSummary.isNotBlank())
        assertTrue(summary.keyTakeaways.isNotEmpty())
    }
}
