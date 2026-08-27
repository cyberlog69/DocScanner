package com.example.docscanner.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryClassifierTest {

    @Test
    fun testClassifyReceipt() {
        val receiptText = """
            WALMART SUPERCENTER
            STORE #1234
            MILK 1GAL        $3.49
            BREAD WHOLE WHEAT $2.99
            SUBTOTAL         $6.48
            TAX (8.25%)      $0.53
            TOTAL            $7.01
            CASH TENDERED    $10.00
            CHANGE DUE       $2.99
            THANK YOU FOR SHOPPING!
        """.trimIndent()

        val category = CategoryClassifier.classify(receiptText)
        assertEquals(DocumentCategory.RECEIPT, category)
    }

    @Test
    fun testClassifyInvoice() {
        val invoiceText = """
            ACME CONSULTING LLC
            INVOICE NO: INV-2026-089
            INVOICE DATE: AUG 27, 2026
            DUE DATE: SEP 26, 2026
            BILL TO: TECH CORP
            SERVICES RENDERED: SOFTWARE ARCHITECTURE
            AMOUNT DUE: $4,500.00
            PAYMENT TERMS: NET 30 DAYS
        """.trimIndent()

        val category = CategoryClassifier.classify(invoiceText)
        assertEquals(DocumentCategory.INVOICE, category)
    }

    @Test
    fun testClassifyContract() {
        val contractText = """
            NON-DISCLOSURE AGREEMENT
            THIS AGREEMENT is entered into on this 27th day of August 2026,
            by and between Party A and Party B.
            TERMS AND CONDITIONS:
            1. Confidentiality obligations and liabilities.
            2. The parties hereby agree to the terms herein.
            IN WITNESS WHEREOF, the undersigned have executed this agreement.
            SIGNATURE: _______________
        """.trimIndent()

        val category = CategoryClassifier.classify(contractText)
        assertEquals(DocumentCategory.CONTRACT, category)
    }

    @Test
    fun testClassifyNotes() {
        val notesText = """
            Sprint Planning Meeting Notes
            Date: 2026-08-27
            Agenda:
            - Discuss architecture improvements
            - Todo: Migrate to Koin DI
            - Summary: Wrap all repository calls with Result
            - Action items for team
        """.trimIndent()

        val category = CategoryClassifier.classify(notesText)
        assertEquals(DocumentCategory.NOTE, category)
    }

    @Test
    fun testClassifyIdCard() {
        val idText = """
            REPUBLIC OF CYBERLOG
            DRIVER LICENSE
            IDENTIFICATION CARD
            DOB: 1995-05-15
            NATIONALITY: GLOBAL
            PASSPORT NO: P8912384
            SEX: M
        """.trimIndent()

        val category = CategoryClassifier.classify(idText)
        assertEquals(DocumentCategory.ID_CARD, category)
    }

    @Test
    fun testClassifyBook() {
        val bookText = """
            CHAPTER 4: DESIGN PATTERNS IN KOTLIN
            PREFACE BY THE AUTHORS
            PAGE 142
            BIBLIOGRAPHY AND INDEX
            PUBLISHED BY TECH PRESS 2026
        """.trimIndent()

        val category = CategoryClassifier.classify(bookText)
        assertEquals(DocumentCategory.BOOK, category)
    }

    @Test
    fun testClassifyBlankReturnsOther() {
        val category = CategoryClassifier.classify("")
        assertEquals(DocumentCategory.OTHER, category)
    }
}
