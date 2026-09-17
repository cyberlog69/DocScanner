package com.example.docscanner.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableExtractorTest {

    @Test
    fun extractTable_pipeTable_parsesRowsAndCsv() {
        val pipeTable = """
            | Item Description | Quantity | Unit Price | Total Price |
            | Mechanical Keyboard | 2 | 89.99 | 179.98 |
            | Wireless Mouse | 1 | 49.50 | 49.50 |
            | USB-C Cable (2m) | 3 | 12.00 | 36.00 |
        """.trimIndent()

        val result = TableExtractor.extractTable(pipeTable)

        assertTrue(result.hasTable)
        assertEquals(4, result.rowCount)
        assertEquals(4, result.columnCount)
        assertEquals("Item Description", result.headers[0])
        assertEquals("Quantity", result.headers[1])
        assertEquals("Mechanical Keyboard", result.rows[0][0])
        assertEquals("Wireless Mouse", result.rows[1][0])

        // Verify CSV content
        assertTrue(result.csvContent.contains("Item Description,Quantity,Unit Price,Total Price"))
        assertTrue(result.csvContent.contains("Mechanical Keyboard,2,89.99,179.98"))
    }

    @Test
    fun extractTable_spaceAlignedTable_detectsColumns() {
        val spaceTable = """
            Product Code    Description          Qty    Amount
            PRD-001         Industrial Widget    10     $1,250.00
            PRD-002         Mounting Bracket     25     $450.00
            PRD-003         Safety Sensor        5      $750.00
        """.trimIndent()

        val result = TableExtractor.extractTable(spaceTable)

        assertTrue(result.hasTable)
        assertTrue(result.rowCount >= 4)
        assertTrue(result.columnCount >= 4)
        assertEquals("Product Code", result.headers[0])
        assertTrue(result.csvContent.contains("Product Code"))
        // Check quotes for cell with comma ($1,250.00)
        assertTrue(result.csvContent.contains("\"$1,250.00\""))
    }
}
