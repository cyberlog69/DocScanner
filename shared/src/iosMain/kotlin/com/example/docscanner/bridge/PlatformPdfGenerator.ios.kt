package com.example.docscanner.bridge

import com.example.docscanner.model.Page
import com.example.docscanner.model.PdfQuality
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginPDFContextToFile
import platform.UIKit.UIGraphicsBeginPDFPageWithInfo
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

import kotlinx.cinterop.useContents
import platform.UIKit.drawInRect

@OptIn(ExperimentalForeignApi::class)
actual class PlatformPdfGenerator {

    actual suspend fun generateSearchablePdf(
        title: String,
        pages: List<Page>,
        quality: PdfQuality,
        outputPath: String
    ): String = withContext(Dispatchers.Default) {
        val a4Width = 595.28
        val a4Height = 841.89
        val compressionRatio = quality.compressionQuality / 100.0

        val success = try {
            val zeroRect = CGRectMake(0.0, 0.0, 0.0, 0.0)
            val contextStarted = UIGraphicsBeginPDFContextToFile(outputPath, zeroRect, null)

            if (contextStarted) {
                for (page in pages) {
                    val uiImage = UIImage.imageWithContentsOfFile(page.imagePath) ?: continue
                    val compressedData = UIImageJPEGRepresentation(uiImage, compressionRatio)
                    val compressedImage = if (compressedData != null) UIImage.imageWithData(compressedData) ?: uiImage else uiImage

                    val (imgWidth, imgHeight) = compressedImage.size.useContents { width to height }
                    val pageHeight = if (imgWidth > 0) a4Width * (imgHeight / imgWidth) else a4Height
                    val pageRect = CGRectMake(0.0, 0.0, a4Width, pageHeight)

                    UIGraphicsBeginPDFPageWithInfo(pageRect, null)

                    // 1. Draw page image layer
                    compressedImage.drawInRect(pageRect)

                    // 2. Draw invisible text layer for OCR searchability
                    if (page.extractedText.isNotBlank()) {
                        val attributes = mapOf<Any?, Any>(
                            NSForegroundColorAttributeName to UIColor.clearColor,
                            NSFontAttributeName to UIFont.systemFontOfSize(12.0)
                        )
                        val lines = page.extractedText.split("\n").filter { it.isNotBlank() }
                        val lineSpacing = if (lines.isNotEmpty()) (pageHeight - 40.0) / lines.size else 16.0
                        for ((lineIdx, line) in lines.withIndex()) {
                            val yPos = 20.0 + (lineIdx * lineSpacing)
                            val lineRect = CGRectMake(20.0, yPos, a4Width - 40.0, lineSpacing.coerceAtLeast(14.0))
                            val nsLine = (line as Any) as NSString
                            nsLine.drawInRect(lineRect, withAttributes = attributes)
                        }
                    }
                }
                UIGraphicsEndPDFContext()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            try { UIGraphicsEndPDFContext() } catch (_: Exception) {}
            false
        }

        if (!success) {
            // Fallback to PDFKit standard export
            val pdfDocument = PDFDocument()
            for ((index, page) in pages.withIndex()) {
                val uiImage = UIImage.imageWithContentsOfFile(page.imagePath) ?: continue
                val compressedData = UIImageJPEGRepresentation(uiImage, compressionRatio)
                val compressedImage = if (compressedData != null) UIImage.imageWithData(compressedData) ?: uiImage else uiImage
                val pdfPage = PDFPage(image = compressedImage)
                pdfDocument.insertPage(pdfPage, atIndex = index.toULong())
            }
            val outputUrl = NSURL.fileURLWithPath(outputPath)
            pdfDocument.writeToURL(outputUrl)
        }

        outputPath
    }
}

