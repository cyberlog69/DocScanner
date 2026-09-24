package com.example.docscanner.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
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
     * Stamps an annotation/watermark onto every page of the given PDF bytes using Apache PDFBox (Apache 2.0).
     */
    fun stampPdf(pdfBytes: ByteArray, config: StampConfig): ByteArray {
        if (pdfBytes.isEmpty() || config.text.isBlank()) return pdfBytes

        val outputStream = ByteArrayOutputStream()
        try {
            val document = PDDocument.load(ByteArrayInputStream(pdfBytes))
            val font = PDType1Font.HELVETICA_BOLD

            val rgb = parseColorHex(config.colorHex)
            val r = rgb[0] / 255f
            val g = rgb[1] / 255f
            val b = rgb[2] / 255f

            val numPages = document.numberOfPages
            for (i in 0 until numPages) {
                val page = document.getPage(i)
                val mediaBox = page.mediaBox
                val contentStream = PDPageContentStream(document, page, AppendMode.APPEND, true, true)

                val extGState = PDExtendedGraphicsState().apply {
                    nonStrokingAlphaConstant = config.opacity
                    strokingAlphaConstant = config.opacity
                }
                contentStream.setGraphicsStateParameters(extGState)
                contentStream.setNonStrokingColor(r, g, b)
                contentStream.setStrokingColor(r, g, b)

                when (config.position) {
                    StampPosition.CENTER_WATERMARK -> {
                        val fontSize = mediaBox.width / 8f
                        val textWidth = font.getStringWidth(config.text) / 1000f * fontSize
                        val angle = 45.0
                        val rad = angle * PI / 180.0
                        val cosA = cos(rad).toFloat()
                        val sinA = sin(rad).toFloat()

                        val centerX = mediaBox.width / 2f
                        val centerY = mediaBox.height / 2f
                        val textOffsetX = textWidth / 2f

                        val startX = centerX - textOffsetX * cosA
                        val startY = centerY - textOffsetX * sinA

                        contentStream.beginText()
                        contentStream.setFont(font, fontSize)
                        val matrix = Matrix(cosA, sinA, -sinA, cosA, startX, startY)
                        contentStream.setTextMatrix(matrix)
                        contentStream.showText(config.text)
                        contentStream.endText()
                    }
                    StampPosition.TOP_HEADER -> {
                        val fontSize = 28f
                        val textWidth = font.getStringWidth(config.text) / 1000f * fontSize
                        val x = (mediaBox.width - textWidth) / 2f
                        val y = mediaBox.height - 50f

                        contentStream.addRect(x - 16f, y - 8f, textWidth + 32f, fontSize + 16f)
                        contentStream.setLineWidth(2f)
                        contentStream.stroke()

                        contentStream.beginText()
                        contentStream.setFont(font, fontSize)
                        contentStream.newLineAtOffset(x, y)
                        contentStream.showText(config.text)
                        contentStream.endText()
                    }
                    StampPosition.BOTTOM_FOOTER -> {
                        val fontSize = 24f
                        val textWidth = font.getStringWidth(config.text) / 1000f * fontSize
                        val x = (mediaBox.width - textWidth) / 2f
                        val y = 40f

                        contentStream.addRect(x - 16f, y - 6f, textWidth + 32f, fontSize + 12f)
                        contentStream.setLineWidth(2f)
                        contentStream.stroke()

                        contentStream.beginText()
                        contentStream.setFont(font, fontSize)
                        contentStream.newLineAtOffset(x, y)
                        contentStream.showText(config.text)
                        contentStream.endText()
                    }
                }

                contentStream.close()
            }

            document.save(outputStream)
            document.close()
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

    /**
     * Stamps an annotation / watermark directly onto a page Bitmap matching the visual appearance.
     */
    fun stampWatermarkOnBitmap(baseBitmap: Bitmap, config: StampConfig): Bitmap {
        if (config.text.isBlank()) return baseBitmap

        val resultBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val rgb = parseColorHex(config.colorHex)
        val alphaInt = ((config.opacity.coerceIn(0.1f, 1.0f)) * 255).toInt()
        val colorInt = Color.argb(alphaInt, rgb[0], rgb[1], rgb[2])

        when (config.position) {
            StampPosition.CENTER_WATERMARK -> {
                val fontSize = (resultBitmap.width / 8f).coerceAtLeast(36f)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorInt
                    textSize = fontSize
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }

                canvas.save()
                canvas.rotate(-45f, resultBitmap.width / 2f, resultBitmap.height / 2f)

                val fontMetrics = paint.fontMetrics
                val baselineOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f
                canvas.drawText(
                    config.text,
                    resultBitmap.width / 2f,
                    (resultBitmap.height / 2f) - baselineOffset,
                    paint
                )
                canvas.restore()
            }
            StampPosition.TOP_HEADER -> {
                val fontSize = (resultBitmap.width * 0.045f).coerceIn(28f, 96f)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorInt
                    textSize = fontSize
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val textWidth = textPaint.measureText(config.text)
                val fontMetrics = textPaint.fontMetrics
                val textHeight = fontMetrics.descent - fontMetrics.ascent

                val centerX = resultBitmap.width / 2f
                val topMargin = (resultBitmap.height * 0.04f).coerceIn(30f, 120f)
                val baselineY = topMargin + textHeight

                val padX = fontSize * 0.6f
                val padY = fontSize * 0.3f
                val borderRect = RectF(
                    centerX - textWidth / 2f - padX,
                    topMargin - padY,
                    centerX + textWidth / 2f + padX,
                    topMargin + textHeight + padY
                )

                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorInt
                    style = Paint.Style.STROKE
                    strokeWidth = (fontSize * 0.08f).coerceAtLeast(3f)
                }
                val cornerRadius = fontSize * 0.3f
                canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, strokePaint)

                canvas.drawText(config.text, centerX, baselineY, textPaint)
            }
            StampPosition.BOTTOM_FOOTER -> {
                val fontSize = (resultBitmap.width * 0.04f).coerceIn(24f, 84f)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorInt
                    textSize = fontSize
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val textWidth = textPaint.measureText(config.text)
                val fontMetrics = textPaint.fontMetrics
                val textHeight = fontMetrics.descent - fontMetrics.ascent

                val centerX = resultBitmap.width / 2f
                val bottomMargin = (resultBitmap.height * 0.04f).coerceIn(30f, 120f)
                val baselineY = resultBitmap.height - bottomMargin - fontMetrics.descent

                val padX = fontSize * 0.6f
                val padY = fontSize * 0.3f
                val borderRect = RectF(
                    centerX - textWidth / 2f - padX,
                    baselineY + fontMetrics.ascent - padY,
                    centerX + textWidth / 2f + padX,
                    baselineY + fontMetrics.descent + padY
                )

                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorInt
                    style = Paint.Style.STROKE
                    strokeWidth = (fontSize * 0.08f).coerceAtLeast(3f)
                }
                val cornerRadius = fontSize * 0.3f
                canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, strokePaint)

                canvas.drawText(config.text, centerX, baselineY, textPaint)
            }
        }

        return resultBitmap
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
