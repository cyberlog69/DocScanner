package com.example.docscanner.bridge

import com.example.docscanner.model.Page
import com.example.docscanner.model.PdfQuality

expect class PlatformPdfGenerator {
    suspend fun generateSearchablePdf(
        title: String,
        pages: List<Page>,
        quality: PdfQuality,
        outputPath: String
    ): String
}
