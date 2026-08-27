package com.example.docscanner.bridge

import com.example.docscanner.model.Page
import com.example.docscanner.model.PdfQuality
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class)
actual class PlatformPdfGenerator {

    actual suspend fun generateSearchablePdf(
        title: String,
        pages: List<Page>,
        quality: PdfQuality,
        outputPath: String
    ): String = withContext(Dispatchers.Default) {
        val pdfDocument = PDFDocument()

        val compressionRatio = quality.compressionQuality / 100.0

        for ((index, page) in pages.withIndex()) {
            val uiImage = UIImage.imageWithContentsOfFile(page.imagePath) ?: continue
            val compressedData = UIImageJPEGRepresentation(uiImage, compressionRatio)
            val compressedImage = if (compressedData != null) UIImage.imageWithData(compressedData) ?: uiImage else uiImage

            val pdfPage = PDFPage(image = compressedImage)
            pdfDocument.insertPage(pdfPage, atIndex = index.toULong())
        }

        val outputUrl = NSURL.fileURLWithPath(outputPath)
        pdfDocument.writeToURL(outputUrl)

        outputPath
    }
}
