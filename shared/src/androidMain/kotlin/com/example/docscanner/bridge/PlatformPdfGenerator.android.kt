package com.example.docscanner.bridge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.docscanner.model.Page
import com.example.docscanner.model.PdfQuality
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        val document = PDDocument()
        document.documentInformation.title = title

        try {
            for (page in pages) {
                val imageFile = File(page.imagePath)
                if (!imageFile.exists()) continue

                val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: continue
                val imgWidth = originalBitmap.width.toFloat()
                val imgHeight = originalBitmap.height.toFloat()

                val a4Width = PDRectangle.A4.width
                val pageHeight = if (imgWidth > 0f) a4Width * (imgHeight / imgWidth) else PDRectangle.A4.height
                val pdPage = PDPage(PDRectangle(a4Width, pageHeight))
                document.addPage(pdPage)

                val qualityRatio = (quality.compressionQuality / 100f).coerceIn(0.1f, 1.0f)
                val pdImage = JPEGFactory.createFromImage(document, originalBitmap, qualityRatio)
                originalBitmap.recycle()

                val contentStream = PDPageContentStream(document, pdPage)
                contentStream.drawImage(pdImage, 0f, 0f, a4Width, pageHeight)

                if (page.extractedText.isNotBlank()) {
                    addInvisibleTextLayer(contentStream, page.extractedText, a4Width, pageHeight)
                }

                contentStream.close()
            }

            document.save(outputFile)
        } finally {
            document.close()
        }

        outputFile.absolutePath
    }

    private fun addInvisibleTextLayer(
        contentStream: PDPageContentStream,
        text: String,
        pageWidth: Float,
        pageHeight: Float
    ) {
        try {
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 10f)
            contentStream.setRenderingMode(RenderingMode.NEITHER) // Invisible text mode

            val lines = text.split("\n").filter { it.isNotBlank() }
            val lineSpacing = if (lines.isNotEmpty()) (pageHeight - 40f) / (lines.size + 1) else 14f

            var lastY = 0f
            for ((i, line) in lines.withIndex()) {
                val yPosition = pageHeight - 30f - (i * lineSpacing)
                if (yPosition > 10f) {
                    val cleanLine = line.replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "")
                    if (i == 0) {
                        contentStream.newLineAtOffset(20f, yPosition)
                    } else {
                        contentStream.newLineAtOffset(0f, yPosition - lastY)
                    }
                    lastY = yPosition
                    try {
                        contentStream.showText(cleanLine)
                    } catch (_: Exception) {}
                }
            }

            contentStream.endText()
        } catch (_: Exception) {
            // Soft failure for text layer; image remains preserved
        }
    }
}
