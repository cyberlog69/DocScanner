package com.example.docscanner.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

enum class SignaturePlacement(val displayName: String) {
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_CENTER("Bottom Center"),
    CENTER("Center Page"),
    TOP_RIGHT("Top Right")
}

enum class TargetPageOption(val displayName: String) {
    CURRENT_PAGE("Current Page"),
    LAST_PAGE("Last Page"),
    FIRST_PAGE("First Page"),
    ALL_PAGES("All Pages")
}

object SignatureService {

    private const val TAG = "SignatureService"
    private const val SIGNATURES_DIR = "signatures"
    private const val DEFAULT_SIGNATURE_NAME = "saved_signature.png"

    /**
     * Stamps an electronic signature (PNG bytes with transparent background) onto a PDF.
     *
     * @param pdfBytes Original PDF content.
     * @param signaturePngBytes Signature PNG image bytes.
     * @param targetPages 1-based list of page numbers to stamp.
     * @param placement Where on the page to stamp the signature.
     * @param signatureWidthScale Fraction of page width for signature (e.g. 0.28f).
     * @return New PDF bytes with signature stamped.
     */
    fun stampSignature(
        pdfBytes: ByteArray,
        signaturePngBytes: ByteArray,
        targetPages: List<Int>,
        placement: SignaturePlacement = SignaturePlacement.BOTTOM_RIGHT,
        signatureWidthScale: Float = 0.28f
    ): ByteArray {
        if (pdfBytes.isEmpty() || signaturePngBytes.isEmpty() || targetPages.isEmpty()) {
            return pdfBytes
        }

        val outputStream = ByteArrayOutputStream()
        try {
            val reader = PdfReader(ByteArrayInputStream(pdfBytes))
            val writer = PdfWriter(outputStream)
            val pdfDoc = PdfDocument(reader, writer)
            val numPages = pdfDoc.numberOfPages

            val imageData = ImageDataFactory.create(signaturePngBytes)
            val imgWidth = imageData.width
            val imgHeight = imageData.height
            val aspect = if (imgWidth > 0f) imgHeight / imgWidth else 0.5f

            for (pageNum in targetPages) {
                if (pageNum < 1 || pageNum > numPages) continue

                val page = pdfDoc.getPage(pageNum)
                val pageSize = page.pageSize
                val canvas = PdfCanvas(page.newContentStreamAfter(), page.resources, pdfDoc)

                val sigWidth = (pageSize.width * signatureWidthScale).coerceIn(100f, 320f)
                val sigHeight = sigWidth * aspect
                val margin = 36f

                val (x, y) = when (placement) {
                    SignaturePlacement.BOTTOM_RIGHT -> Pair(
                        pageSize.width - sigWidth - margin,
                        margin
                    )
                    SignaturePlacement.BOTTOM_LEFT -> Pair(
                        margin,
                        margin
                    )
                    SignaturePlacement.BOTTOM_CENTER -> Pair(
                        (pageSize.width - sigWidth) / 2f,
                        margin
                    )
                    SignaturePlacement.CENTER -> Pair(
                        (pageSize.width - sigWidth) / 2f,
                        (pageSize.height - sigHeight) / 2f
                    )
                    SignaturePlacement.TOP_RIGHT -> Pair(
                        pageSize.width - sigWidth - margin,
                        pageSize.height - sigHeight - margin
                    )
                }

                val rect = Rectangle(x, y, sigWidth, sigHeight)
                canvas.addImageFittedIntoRectangle(imageData, rect, false)
                canvas.release()
            }

            pdfDoc.close()
            return outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stamp signature onto PDF", e)
            return pdfBytes
        }
    }

    /**
     * Stamps an electronic signature directly onto an existing PDF file on disk.
     */
    fun stampSignatureOnFile(
        pdfFile: File,
        signaturePngBytes: ByteArray,
        targetPages: List<Int>,
        placement: SignaturePlacement = SignaturePlacement.BOTTOM_RIGHT
    ): Boolean {
        if (!pdfFile.exists() || pdfFile.length() == 0L) return false
        val originalBytes = pdfFile.readBytes()
        val stampedBytes = stampSignature(originalBytes, signaturePngBytes, targetPages, placement)
        if (stampedBytes.isEmpty() || stampedBytes.contentEquals(originalBytes)) {
            return false
        }
        return try {
            pdfFile.writeBytes(stampedBytes)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write stamped PDF file", e)
            false
        }
    }

    /**
     * Stamps an electronic signature onto a page Bitmap, respecting placement, aspect ratio, and scaling.
     */
    fun stampSignatureOnBitmap(
        baseBitmap: Bitmap,
        signatureBitmap: Bitmap,
        placement: SignaturePlacement = SignaturePlacement.BOTTOM_RIGHT,
        signatureWidthScale: Float = 0.28f
    ): Bitmap {
        val resultBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val aspect = if (signatureBitmap.width > 0) {
            signatureBitmap.height.toFloat() / signatureBitmap.width.toFloat()
        } else 0.5f

        val sigWidth = (resultBitmap.width * signatureWidthScale).coerceIn(120f, resultBitmap.width * 0.7f)
        val sigHeight = sigWidth * aspect
        val margin = (resultBitmap.width * 0.05f).coerceIn(24f, 120f)

        val (x, y) = when (placement) {
            SignaturePlacement.BOTTOM_RIGHT -> Pair(resultBitmap.width - sigWidth - margin, resultBitmap.height - sigHeight - margin)
            SignaturePlacement.BOTTOM_LEFT -> Pair(margin, resultBitmap.height - sigHeight - margin)
            SignaturePlacement.BOTTOM_CENTER -> Pair((resultBitmap.width - sigWidth) / 2f, resultBitmap.height - sigHeight - margin)
            SignaturePlacement.CENTER -> Pair((resultBitmap.width - sigWidth) / 2f, (resultBitmap.height - sigHeight) / 2f)
            SignaturePlacement.TOP_RIGHT -> Pair(resultBitmap.width - sigWidth - margin, margin)
        }

        val destRect = RectF(x, y, x + sigWidth, y + sigHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(signatureBitmap, null, destRect, paint)
        return resultBitmap
    }

    /**
     * Stamps an electronic signature (from PNG byte array) onto a page Bitmap.
     */
    fun stampSignatureOnBitmap(
        baseBitmap: Bitmap,
        signaturePngBytes: ByteArray,
        placement: SignaturePlacement = SignaturePlacement.BOTTOM_RIGHT,
        signatureWidthScale: Float = 0.28f
    ): Bitmap {
        val sigBitmap = BitmapFactory.decodeByteArray(signaturePngBytes, 0, signaturePngBytes.size) ?: return baseBitmap
        val result = stampSignatureOnBitmap(baseBitmap, sigBitmap, placement, signatureWidthScale)
        sigBitmap.recycle()
        return result
    }

    /**
     * Saves a user signature to internal storage for future 1-tap reuse.
     */
    fun saveSignatureToDisk(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val dir = File(context.filesDir, SIGNATURES_DIR).apply { mkdirs() }
            val file = File(dir, DEFAULT_SIGNATURE_NAME)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving signature to disk", e)
            false
        }
    }

    /**
     * Retrieves the saved signature if present.
     */
    fun getSavedSignatureFile(context: Context): File? {
        val file = File(File(context.filesDir, SIGNATURES_DIR), DEFAULT_SIGNATURE_NAME)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun loadSavedSignatureBitmap(context: Context): Bitmap? {
        val file = getSavedSignatureFile(context) ?: return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding saved signature bitmap", e)
            null
        }
    }

    fun deleteSavedSignature(context: Context): Boolean {
        val file = getSavedSignatureFile(context) ?: return false
        return file.delete()
    }
}
