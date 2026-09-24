package com.example.docscanner.service

import android.content.Context
import java.io.File
import java.io.FileOutputStream

fun TableExtractor.writeCsvToFile(context: Context, csvContent: String, title: String): File {
    val safeTitle = title.replace(Regex("""[^a-zA-Z0-9]"""), "_").ifBlank { "table_export" }
    val file = File(context.cacheDir, "$safeTitle.csv")
    FileOutputStream(file).use { out ->
        out.write(csvContent.toByteArray(Charsets.UTF_8))
    }
    return file
}

fun TableExtractor.extractTableFromOcrResult(ocrResult: OcrResult): TableExtractionResult {
    val allLines = ocrResult.blocks.flatMap { it.lines }
    val linesWithBoxes = allLines.filter { it.boundingBox != null }

    if (linesWithBoxes.size >= 4) {
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

        val candidateRows = rows.filter { it.size >= 2 }
        if (candidateRows.size >= 2) {
            val grid = candidateRows.map { row ->
                row.sortedBy { it.boundingBox!!.left }.map { it.text.trim() }
            }
            return buildResult(grid)
        }
    }

    return extractTable(ocrResult.fullText)
}
