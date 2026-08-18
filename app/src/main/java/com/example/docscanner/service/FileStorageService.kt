package com.example.docscanner.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Manages all file I/O for scanned documents.
 * All files are stored in the app's private storage — no cloud, no external access.
 */
class FileStorageService(
    private val context: Context
) {
    private val documentsDir: File
        get() = File(context.filesDir, "documents").also { it.mkdirs() }

    private val thumbnailsDir: File
        get() = File(context.filesDir, "thumbnails").also { it.mkdirs() }

    private val cacheDir: File
        get() = File(context.cacheDir, "scans").also { it.mkdirs() }

    /** Saves a bitmap as JPEG to the documents directory with custom compression quality. Returns the file path. */
    fun savePageImage(bitmap: Bitmap, documentId: String, pageIndex: Int, quality: Int = 92): String {
        val docDir = File(documentsDir, documentId).also { it.mkdirs() }
        val file = File(docDir, "page_${pageIndex}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(50, 100), out)
        }
        return file.absolutePath
    }

    /** Saves a small thumbnail for the document list. */
    fun saveThumbnail(bitmap: Bitmap, documentId: String): String {
        val thumbnail = Bitmap.createScaledBitmap(bitmap, 300, 400, true)
        val file = File(thumbnailsDir, "${documentId}.jpg")
        FileOutputStream(file).use { out ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        thumbnail.recycle()
        return file.absolutePath
    }

    /** Saves a PDF file for a document. Returns the file path. */
    fun savePdf(data: ByteArray, documentId: String, fileName: String = "document.pdf"): String {
        val docDir = File(documentsDir, documentId).also { it.mkdirs() }
        val file = File(docDir, fileName)
        file.writeBytes(data)
        return file.absolutePath
    }

    /** Gets the PDF file for a document if it exists. */
    fun getPdfFile(pdfPath: String): File? {
        val file = File(pdfPath)
        return if (file.exists()) file else null
    }

    /** Loads a bitmap from a file path. */
    fun loadBitmap(path: String): Bitmap? = BitmapFactory.decodeFile(path)

    /** Creates a temporary file in cache for camera captures. */
    fun createTempImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(cacheDir, "SCAN_${timestamp}_${UUID.randomUUID()}.jpg")
    }

    /** Deletes all files for a document (images + PDF). */
    fun deleteDocumentFiles(documentId: String) {
        File(documentsDir, documentId).deleteRecursively()
        File(thumbnailsDir, "${documentId}.jpg").delete()
    }

    /** Deletes a single page image file. */
    fun deletePageFile(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }

    /** Rotates an image file on disk by [degrees] clockwise and saves it back. */
    fun rotateImageFile(imagePath: String, degrees: Float = 90f): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath) ?: return false
            val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            FileOutputStream(imagePath).use { out ->
                rotated.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (rotated !== bitmap) rotated.recycle()
            bitmap.recycle()
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Gets the total storage size in bytes occupied by a specific document's files (images + PDF + thumbnail). */
    fun getDocumentStorageBytes(documentId: String): Long {
        val docDir = File(documentsDir, documentId)
        val thumb = File(thumbnailsDir, "${documentId}.jpg")
        val docBytes = if (docDir.exists()) docDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() } else 0L
        val thumbBytes = if (thumb.exists()) thumb.length() else 0L
        return docBytes + thumbBytes
    }

    /** Gets a content URI via FileProvider for sharing. */
    fun getShareUri(context: Context, file: File): Uri {
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /** Returns the total storage used by the app in bytes. */
    fun getTotalStorageUsed(): Long = documentsDir.walkBottomUp()
        .filter { it.isFile }
        .sumOf { it.length() }

    companion object {
        /** Formats byte count into a human-readable string like "1.2 MB" or "450 KB". */
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
    }
}
