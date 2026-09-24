package com.example.docscanner.service

import android.graphics.Bitmap
import android.util.Log
import com.example.docscanner.data.pref.PdfQuality
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import java.io.ByteArrayOutputStream

/**
 * Generates searchable PDFs from scanned page images + OCR text using Apache PDFBox (Apache 2.0).
 * Each page has a high-fidelity image layer (visible) + invisible text layer (searchable).
 */
class PdfGenerator {

    /**
     * Creates a searchable PDF from a list of [PageData] using the specified [PdfQuality].
     * Returns the PDF as a ByteArray.
     */
    fun generatePdf(
        pages: List<PageData>,
        title: String,
        quality: PdfQuality = PdfQuality.HIGH
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()

        try {
            val document = PDDocument()
            document.documentInformation.title = title

            val qualityRatio = when (quality) {
                PdfQuality.UHD_4K -> 1.0f
                PdfQuality.HIGH -> 0.92f
                PdfQuality.STANDARD -> 0.80f
            }

            for (pageData in pages) {
                addPageToPdf(document, pageData, qualityRatio)
            }

            document.save(outputStream)
            document.close()
        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error generating searchable PDF with PDFBox, falling back to basic image PDF", e)
            return generateBasicPdf(pages, title, quality)
        }

        return outputStream.toByteArray()
    }

    private fun addPageToPdf(
        document: PDDocument,
        pageData: PageData,
        qualityRatio: Float
    ) {
        val bitmap = pageData.bitmap
        val bitmapAspect = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height.toFloat() else 0.7f
        val pageWidth = PDRectangle.A4.width
        val pageHeight = if (bitmapAspect > 0f) pageWidth / bitmapAspect else PDRectangle.A4.height

        val pdPage = PDPage(PDRectangle(pageWidth, pageHeight))
        document.addPage(pdPage)

        val pdImage = JPEGFactory.createFromImage(document, bitmap, qualityRatio)
        val contentStream = PDPageContentStream(document, pdPage)
        contentStream.drawImage(pdImage, 0f, 0f, pageWidth, pageHeight)

        if (pageData.extractedText.isNotBlank()) {
            addInvisibleTextLayer(contentStream, pageData.extractedText, pageWidth, pageHeight)
        }

        contentStream.close()
    }

    private fun addInvisibleTextLayer(
        contentStream: PDPageContentStream,
        extractedText: String,
        pageWidth: Float,
        pageHeight: Float
    ) {
        try {
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 12f)
            contentStream.setRenderingMode(RenderingMode.NEITHER) // Invisible rendering mode

            val lines = extractedText.split("\n").filter { it.isNotBlank() }
            val lineHeight = (pageHeight - 40f) / lines.size.coerceAtLeast(1)

            var lastY = 0f
            for ((i, line) in lines.withIndex()) {
                val y = pageHeight - 20f - (i * lineHeight)
                val cleanLine = line.replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "")
                if (i == 0) {
                    contentStream.newLineAtOffset(20f, y)
                } else {
                    contentStream.newLineAtOffset(0f, y - lastY)
                }
                lastY = y
                try {
                    contentStream.showText(cleanLine)
                } catch (_: Exception) {}
            }

            contentStream.endText()
        } catch (e: Exception) {
            Log.w("PdfGenerator", "Failed to add invisible OCR text layer to PDF page", e)
        }
    }

    /**
     * Fallback: generates a simple PDF with page images.
     */
    private fun generateBasicPdf(
        pages: List<PageData>,
        title: String,
        quality: PdfQuality
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val document = PDDocument()
        document.documentInformation.title = title

        val qualityRatio = when (quality) {
            PdfQuality.UHD_4K -> 1.0f
            PdfQuality.HIGH -> 0.92f
            PdfQuality.STANDARD -> 0.80f
        }

        try {
            for (pageData in pages) {
                val bitmap = pageData.bitmap
                val bitmapAspect = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height.toFloat() else 0.7f
                val pageWidth = PDRectangle.A4.width
                val pageHeight = if (bitmapAspect > 0f) pageWidth / bitmapAspect else PDRectangle.A4.height

                val pdPage = PDPage(PDRectangle(pageWidth, pageHeight))
                document.addPage(pdPage)

                val pdImage = JPEGFactory.createFromImage(document, bitmap, qualityRatio)
                val contentStream = PDPageContentStream(document, pdPage)
                contentStream.drawImage(pdImage, 0f, 0f, pageWidth, pageHeight)
                contentStream.close()
            }

            document.save(outputStream)
        } finally {
            document.close()
        }

        return outputStream.toByteArray()
    }
}

data class PageData(
    val bitmap: Bitmap,
    val extractedText: String = ""
)
