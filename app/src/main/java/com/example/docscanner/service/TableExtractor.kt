package com.example.docscanner.service

import android.content.Context
import java.io.File
import java.io.FileOutputStream

data class TableExtractionResult(
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val csvContent: String = "",
    val rowCount: Int = 0,
    val columnCount: Int = 0
) {
    val hasTable: Boolean
        get() = rowCount >= 2 && columnCount >= 2
}

object TableExtractor {

    /**
     * Extracts tabular data from OCR text.
     * Parses column alignments using delimiter patterns (pipes, tabs, multiple spaces).
     */
    fun extractTable(ocrText: String): TableExtractionResult {
        if (ocrText.isBlank()) return TableExtractionResult()

        val rawLines = ocrText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("---") && !it.startsWith("===") }

        if (rawLines.size < 2) return TableExtractionResult()

        // 1. Check for pipe-delimited table (| Col1 | Col2 | ...)
        val pipeRows = parsePipeTable(rawLines)
        if (pipeRows.size >= 2) {
            return buildResult(pipeRows)
        }

        // 2. Check for tab-delimited or multi-space aligned rows
        val spaceRows = parseSpaceAlignedTable(rawLines)
        if (spaceRows.size >= 2) {
            return buildResult(spaceRows)
        }

        // 3. Fallback: comma-separated or simple tabular splitting
        val commaRows = parseCommaSeparatedTable(rawLines)
        if (commaRows.size >= 2) {
            return buildResult(commaRows)
        }

        return TableExtractionResult()
    }

    /**
     * Extracts table from OCR blocks when spatial line coordinates are available.
     */
    fun extractTableFromOcrResult(ocrResult: OcrResult): TableExtractionResult {
        val allLines = ocrResult.blocks.flatMap { it.lines }
        val linesWithBoxes = allLines.filter { it.boundingBox != null }

        if (linesWithBoxes.size >= 4) {
            // Group lines into vertical rows based on Y-overlap
            val sorted = linesWithBoxes.sortedBy { it.boundingBox!!.top }
            val rows = mutableListOf<MutableList<OcrLine>>()

            for (line in sorted) {
                val box = line.boundingBox!!
                val midY = (box.top + box.bottom) / 2f
                val height = (box.bottom - box.top).coerceAtLeast(10)

                val matchingRow = rows.find { row ->
                    val rowMidY = row.mapNotNull { it.boundingBox }.map { (it.top + it.bottom) / 2f }.average()
                    kotlin.math.abs(midY - rowMidY) < height * 0.7f
                }

                if (matchingRow != null) {
                    matchingRow.add(line)
                } else {
                    rows.add(mutableListOf(line))
                }
            }

            // If we found at least 2 rows with 2+ columns each
            val candidateRows = rows.filter { it.size >= 2 }
            if (candidateRows.size >= 2) {
                val grid = candidateRows.map { row ->
                    row.sortedBy { it.boundingBox!!.left }.map { it.text.trim() }
                }
                return buildResult(grid)
            }
        }

        // Fallback to text parsing
        return extractTable(ocrResult.fullText)
    }

    private fun parsePipeTable(lines: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        for (line in lines) {
            if (line.contains("|")) {
                val cells = line.split("|")
                    .map { it.trim() }
                    .filterIndexed { index, _ ->
                        // Exclude leading/trailing empty elements from `| a | b |`
                        index != 0 || line.startsWith("|").not()
                    }
                    .filter { it.isNotEmpty() && !it.all { ch -> ch == '-' || ch == ':' } }

                if (cells.size >= 2) {
                    rows.add(cells)
                }
            }
        }
        return rows
    }

    private fun parseSpaceAlignedTable(lines: List<String>): List<List<String>> {
        // Look for lines that have 2 or more columns separated by at least 2 spaces or tabs
        val delimiterRegex = Regex("""\t+|\s{2,}""")
        val rows = mutableListOf<List<String>>()

        for (line in lines) {
            val tokens = line.split(delimiterRegex).map { it.trim() }.filter { it.isNotBlank() }
            if (tokens.size >= 2) {
                rows.add(tokens)
            }
        }

        if (rows.size < 2) return emptyList()

        // Find modal column count
        val colCounts = rows.map { it.size }
        val commonCount = colCounts.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0

        if (commonCount < 2) return emptyList()

        // Normalize rows to align with commonCount
        return rows.filter { kotlin.math.abs(it.size - commonCount) <= 1 }
    }

    private fun parseCommaSeparatedTable(lines: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        for (line in lines) {
            if (line.count { it == ',' } >= 2) {
                val cells = line.split(",").map { it.trim() }
                if (cells.size >= 2) {
                    rows.add(cells)
                }
            }
        }
        return rows
    }

    private fun buildResult(rawGrid: List<List<String>>): TableExtractionResult {
        if (rawGrid.isEmpty()) return TableExtractionResult()

        val maxCols = rawGrid.maxOf { it.size }
        // Pad shorter rows so table is rectangular
        val normalizedRows = rawGrid.map { row ->
            if (row.size < maxCols) row + List(maxCols - row.size) { "" } else row
        }

        val headers = normalizedRows.first()
        val dataRows = normalizedRows.drop(1)

        val csvBuilder = StringBuilder()
        for (row in normalizedRows) {
            val formattedLine = row.joinToString(",") { cell ->
                escapeCsvCell(cell)
            }
            csvBuilder.appendLine(formattedLine)
        }

        return TableExtractionResult(
            headers = headers,
            rows = dataRows,
            csvContent = csvBuilder.toString().trimEnd(),
            rowCount = normalizedRows.size,
            columnCount = maxCols
        )
    }

    private fun escapeCsvCell(cell: String): String {
        val clean = cell.trim()
        val containsSpecial = clean.contains(',') || clean.contains('"') || clean.contains('\n') || clean.contains('\r')
        return if (containsSpecial) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }

    /**
     * Saves CSV string to a file in cache directory for sharing.
     */
    fun writeCsvToFile(context: Context, csvContent: String, title: String): File {
        val safeTitle = title.replace(Regex("""[^a-zA-Z0-9]"""), "_").ifBlank { "table_export" }
        val file = File(context.cacheDir, "$safeTitle.csv")
        FileOutputStream(file).use { out ->
            out.write(csvContent.toByteArray(Charsets.UTF_8))
        }
        return file
    }
}
