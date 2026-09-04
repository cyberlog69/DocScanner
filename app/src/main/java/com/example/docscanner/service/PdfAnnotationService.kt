package com.example.docscanner.service

import android.util.Log
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class StampPosition(val displayName: String) {
    CENTER_WATERMARK("Diagonal Watermark"),
    TOP_HEADER("Header Stamp"),
    BOTTOM_FOOTER("Footer Stamp")
}

data class StampConfig(
    val text: String,
    val colorHex: String = "#D32F2F", // Red default
    val position: StampPosition = StampPosition.CENTER_WATERMARK,
    val opacity: Float = 0.35f
)

object PdfAnnotationService {

    val PRESET_STAMPS = listOf(
        "APPROVED",
        "CONFIDENTIAL",
        "PAID",
        "COPY",
        "DRAFT",
        "URGENT",
        "VERIFIED"
    )

    /**
     * Stamps an annotation/watermark onto every page of the given PDF bytes.
     */
    fun stampPdf(pdfBytes: ByteArray, config: StampConfig): ByteArray {
        if (pdfBytes.isEmpty() || config.text.isBlank()) return pdfBytes

        val outputStream = ByteArrayOutputStream()
        try {
            val reader = PdfReader(ByteArrayInputStream(pdfBytes))
            val writer = PdfWriter(outputStream)
            val pdfDoc = PdfDocument(reader, writer)
            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

            val rgb = parseColorHex(config.colorHex)
            val itextColor = DeviceRgb(rgb[0], rgb[1], rgb[2])

            val numPages = pdfDoc.numberOfPages
            for (i in 1..numPages) {
                val page = pdfDoc.getPage(i)
                val pageSize = page.pageSize
                val canvas = PdfCanvas(page.newContentStreamAfter(), page.resources, pdfDoc)

                val extGState = PdfExtGState().apply {
                    fillOpacity = config.opacity
                    strokeOpacity = config.opacity
                }
                canvas.setExtGState(extGState)
                canvas.setColor(itextColor, true)
                canvas.setColor(itextColor, false)

                when (config.position) {
                    StampPosition.CENTER_WATERMARK -> {
                        val fontSize = pageSize.width / 8f
                        canvas.setFontAndSize(font, fontSize)
                        val textWidth = font.getWidth(config.text, fontSize)
                        val angle = 45.0
                        val rad = angle * PI / 180.0
                        val cosA = cos(rad).toFloat()
                        val sinA = sin(rad).toFloat()

                        val centerX = pageSize.width / 2f
                        val centerY = pageSize.height / 2f
                        val textOffsetX = textWidth / 2f

                        val startX = centerX - textOffsetX * cosA
                        val startY = centerY - textOffsetX * sinA

                        canvas.beginText()
                        canvas.setTextMatrix(cosA, sinA, -sinA, cosA, startX, startY)
                        canvas.showText(config.text)
                        canvas.endText()
                    }
                    StampPosition.TOP_HEADER -> {
                        val fontSize = 28f
                        canvas.setFontAndSize(font, fontSize)
                        val textWidth = font.getWidth(config.text, fontSize)
                        val x = (pageSize.width - textWidth) / 2f
                        val y = pageSize.height - 50f

                        // Draw subtle rounded border rectangle around header stamp
                        canvas.rectangle((x - 16f).toDouble(), (y - 8f).toDouble(), (textWidth + 32f).toDouble(), (fontSize + 16f).toDouble())
                        canvas.setLineWidth(2f)
                        canvas.stroke()

                        canvas.beginText()
                        canvas.moveText(x.toDouble(), y.toDouble())
                        canvas.showText(config.text)
                        canvas.endText()
                    }
                    StampPosition.BOTTOM_FOOTER -> {
                        val fontSize = 24f
                        canvas.setFontAndSize(font, fontSize)
                        val textWidth = font.getWidth(config.text, fontSize)
                        val x = (pageSize.width - textWidth) / 2f
                        val y = 40f

                        canvas.rectangle((x - 16f).toDouble(), (y - 6f).toDouble(), (textWidth + 32f).toDouble(), (fontSize + 12f).toDouble())
                        canvas.setLineWidth(2f)
                        canvas.stroke()

                        canvas.beginText()
                        canvas.moveText(x.toDouble(), y.toDouble())
                        canvas.showText(config.text)
                        canvas.endText()
                    }
                }
                canvas.release()
            }

            pdfDoc.close()
            return outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e("PdfAnnotationService", "Error stamping PDF", e)
            return pdfBytes
        }
    }

    /**
     * Stamps an existing PDF file on disk.
     */
    fun stampPdfFile(file: File, config: StampConfig): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val stampedBytes = stampPdf(file.readBytes(), config)
        return try {
            file.writeBytes(stampedBytes)
            true
        } catch (e: Exception) {
            Log.e("PdfAnnotationService", "Failed to write stamped bytes to ${file.name}", e)
            false
        }
    }

    private fun parseColorHex(hex: String): IntArray {
        val clean = hex.removePrefix("#")
        return try {
            val colorInt = clean.toLong(16).toInt()
            intArrayOf(
                (colorInt shr 16) and 0xFF,
                (colorInt shr 8) and 0xFF,
                colorInt and 0xFF
            )
        } catch (_: Exception) {
            intArrayOf(211, 47, 47) // Fallback Red
        }
    }
}
