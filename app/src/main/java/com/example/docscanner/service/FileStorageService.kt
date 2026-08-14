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

    /** Saves a bitmap as JPEG to the documents directory. Returns the file path. */
    fun savePageImage(bitmap: Bitmap, documentId: String, pageIndex: Int): String {
        val docDir = File(documentsDir, documentId).also { it.mkdirs() }
        val file = File(docDir, "page_${pageIndex}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
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
    fun savePdf(data: ByteArray, documentId: String): String {
        val docDir = File(documentsDir, documentId).also { it.mkdirs() }
        val file = File(docDir, "document.pdf")
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
}
