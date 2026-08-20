package com.example.docscanner.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryClassifierTest {

    @Test
    fun classify_invoiceText_detectsInvoice() {
        val text = """
            TAX INVOICE
            GSTIN: 27ABCDE1234F1Z5
            Invoice No: INV-2024-001
            Invoice Date: 2024-01-15
            Subtotal: 1000.00
            CGST: 90.00
            SGST: 90.00
            Total Amount: 1180.00
        """.trimIndent()
        assertEquals(DocumentCategory.INVOICE, CategoryClassifier.classify(text))
    }

    @Test
    fun classify_receiptText_detectsReceipt() {
        val text = """
            CASH RECEIPT
            Store #4521
            Payment Method: CARD ENDING 4421
            Amount Paid: 540.00
            Cashier: John
            Change Due: 0.00
        """.trimIndent()
        assertEquals(DocumentCategory.RECEIPT, CategoryClassifier.classify(text))
    }

    @Test
    fun classify_idCardText_detectsIdCard() {
        val text = """
            Government of India
            UNIQUE IDENTIFICATION AUTHORITY OF INDIA
            AADHAAR
            Date of Birth: 01/01/1990
            Father's Name: Ramesh
        """.trimIndent()
        assertEquals(DocumentCategory.ID_CARD, CategoryClassifier.classify(text))
    }

    @Test
    fun classify_contractText_detectsContract() {
        val text = """
            EMPLOYMENT AGREEMENT
            This Agreement is entered into by the parties hereto...
            WHEREAS the employer wishes to employ the employee...
            IN WITNESS WHEREOF, the parties have signed and delivered.
        """.trimIndent()
        assertEquals(DocumentCategory.CONTRACT, CategoryClassifier.classify(text))
    }

    @Test
    fun classify_blankOrNoKeywords_returnsFallback() {
        assertEquals(DocumentCategory.OTHER, CategoryClassifier.classify(""))
        assertEquals(DocumentCategory.OTHER, CategoryClassifier.classify("random shopping list groceries milk bread"))
        assertEquals(DocumentCategory.NOTE, CategoryClassifier.classify("random", fallbackCategory = DocumentCategory.NOTE))
    }

    @Test
    fun classify_isCaseInsensitive() {
        val text = "TAX INVOICE GSTIN 27AAABC Invoice No 45".uppercase()
        assertEquals(DocumentCategory.INVOICE, CategoryClassifier.classify(text))
    }

    @Test
    fun classify_strongerMatchWins() {
        // Clear invoice indicators must win over the single low-weight "total" receipt hit.
        val text = "TAX INVOICE GSTIN 27AAABC Invoice No 45 Total 1000"
        assertEquals(DocumentCategory.INVOICE, CategoryClassifier.classify(text))
    }
}