package com.example.docscanner.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.kernel.geom.PageSize
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
/**
 * Generates searchable PDFs from scanned page images + OCR text.
 * Each page has an image layer (visible) + invisible text layer (searchable).
 */
class PdfGenerator {

    /**
     * Creates a searchable PDF from a list of [PageData].
     * Returns the PDF as a ByteArray.
     */
    fun generatePdf(pages: List<PageData>, title: String): ByteArray {
        val outputStream = ByteArrayOutputStream()

        try {
            val pdfWriter = PdfWriter(outputStream)
            val pdfDoc = PdfDocument(pdfWriter)
            pdfDoc.documentInfo.title = title

            pages.forEachIndexed { index, pageData ->
                addPageToPdf(pdfDoc, pageData, index)
            }

            pdfDoc.close()
        } catch (e: Exception) {
            // Fallback: create basic PDF with just images
            return generateBasicPdf(pages, title)
        }

        return outputStream.toByteArray()
    }

    private fun addPageToPdf(pdfDoc: PdfDocument, pageData: PageData, pageIndex: Int) {
        val bitmap = pageData.bitmap

        // Determine page size (A4 proportional)
        val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val pageWidth = PageSize.A4.width
        val pageHeight = pageWidth / bitmapAspect

        val pageSize = PageSize(pageWidth, pageHeight)
        val page = pdfDoc.addNewPage(pageSize)
        val canvas = PdfCanvas(page)

        // Add image layer
        val bitmapBytes = bitmapToJpegBytes(bitmap)
        val imageData = ImageDataFactory.create(bitmapBytes)
        val pdfImage = Image(imageData)
            .setFixedPosition(0f, 0f)
            .scaleToFit(pageWidth, pageHeight)
        val document = Document(pdfDoc)
        document.add(pdfImage)

        // Add invisible text layer for searchability
        if (pageData.extractedText.isNotBlank()) {
            addInvisibleTextLayer(canvas, pageData, pageWidth, pageHeight)
        }
    }

    private fun addInvisibleTextLayer(
        canvas: PdfCanvas,
        pageData: PageData,
        pageWidth: Float,
        pageHeight: Float
    ) {
        // Use rendering mode 3 (invisible) to overlay searchable text
        canvas.beginText()
        canvas.setTextRenderingMode(3) // Invisible
        canvas.setFontAndSize(
            com.itextpdf.kernel.font.PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA
            ),
            12f
        )

        // Place text lines at approximate positions
        val lines = pageData.extractedText.split("\n").filter { it.isNotBlank() }
        val lineHeight = pageHeight / (lines.size + 1).coerceAtLeast(1)
        lines.forEachIndexed { i, line ->
            val y = pageHeight - (i + 1) * lineHeight
            canvas.moveText(20.0, y.toDouble())
            canvas.showText(line)
            canvas.newlineText()
        }
        canvas.endText()
    }

    private fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

    /**
     * Fallback: generates a simple PDF with just page images.
     */
    private fun generateBasicPdf(pages: List<PageData>, title: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val pdfWriter = PdfWriter(outputStream)
        val pdfDoc = PdfDocument(pdfWriter)
        pdfDoc.documentInfo.title = title

        pages.forEach { pageData ->
            val bitmap = pageData.bitmap
            val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val pageWidth = PageSize.A4.width
            val pageHeight = pageWidth / bitmapAspect
            pdfDoc.addNewPage(PageSize(pageWidth, pageHeight))

            val bitmapBytes = bitmapToJpegBytes(bitmap)
            val imageData = ImageDataFactory.create(bitmapBytes)
            val pdfImage = Image(imageData)
                .setFixedPosition(0f, 0f)
                .scaleToFit(pageWidth, pageHeight)
            val document = Document(pdfDoc)
            document.add(pdfImage)
        }

        pdfDoc.close()
        return outputStream.toByteArray()
    }
}

data class PageData(
    val bitmap: Bitmap,
    val extractedText: String = ""
)
