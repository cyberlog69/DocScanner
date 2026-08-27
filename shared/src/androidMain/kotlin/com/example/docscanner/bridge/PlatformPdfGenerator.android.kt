package com.example.docscanner.bridge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.docscanner.model.Page
import com.example.docscanner.model.PdfQuality
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

actual class PlatformPdfGenerator {

    actual suspend fun generateSearchablePdf(
        title: String,
        pages: List<Page>,
        quality: PdfQuality,
        outputPath: String
    ): String = withContext(Dispatchers.IO) {
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val writer = PdfWriter(outputFile)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)
        document.setMargins(0f, 0f, 0f, 0f)

        try {
            for ((index, page) in pages.withIndex()) {
                val imageFile = File(page.imagePath)
                if (!imageFile.exists()) continue

                val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: continue
                val processedBytes = processBitmapForQuality(originalBitmap, quality)
                originalBitmap.recycle()

                val imageData = ImageDataFactory.create(processedBytes)
                val imgWidth = imageData.width
                val imgHeight = imageData.height

                val pageSize = PageSize(imgWidth, imgHeight)
                if (index > 0) {
                    pdfDoc.addNewPage(pageSize)
                }

                val image = Image(imageData).apply {
                    setFixedPosition(index + 1, 0f, 0f, imgWidth)
                    scaleToFit(imgWidth, imgHeight)
                }
                document.add(image)

                // Add invisible OCR text overlay
                if (page.extractedText.isNotBlank()) {
                    addInvisibleTextLayer(pdfDoc, index + 1, page.extractedText, imgWidth, imgHeight)
                }
            }
        } finally {
            document.close()
        }

        outputFile.absolutePath
    }

    private fun processBitmapForQuality(original: Bitmap, quality: PdfQuality): ByteArray {
        val stream = ByteArrayOutputStream()
        original.compress(Bitmap.CompressFormat.JPEG, quality.compressionQuality, stream)
        return stream.toByteArray()
    }

    private fun addInvisibleTextLayer(
        pdfDoc: PdfDocument,
        pageNumber: Int,
        text: String,
        pageWidth: Float,
        pageHeight: Float
    ) {
        try {
            val pdfPage = pdfDoc.getPage(pageNumber)
            val canvas = PdfCanvas(pdfPage.newContentStreamBefore(), pdfPage.resources, pdfDoc)

            canvas.saveState()
            val font = PdfFontFactory.createFont()
            canvas.setFontAndSize(font, 10f)
            canvas.setFillColor(ColorConstants.WHITE)

            // Text rendering mode 3 = invisible text
            canvas.setTextRenderingMode(3)
            canvas.beginText()

            val lines = text.split("\n").filter { it.isNotBlank() }
            val lineSpacing = if (lines.isNotEmpty()) (pageHeight - 40f) / (lines.size + 1) else 14f

            for ((i, line) in lines.withIndex()) {
                val yPosition = pageHeight - 30f - (i * lineSpacing)
                if (yPosition > 10f) {
                    canvas.moveText(20.0, yPosition.toDouble())
                    canvas.showText(line)
                    canvas.moveText(-20.0, -yPosition.toDouble())
                }
            }

            canvas.endText()
            canvas.restoreState()
        } catch (_: Exception) {
            // Soft failure for text layer; image remains preserved
        }
    }
}
