package com.example.docscanner.bridge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

actual class PlatformStorage(private val context: Context) {

    actual fun getAppDataDirectory(): String {
        return context.filesDir.absolutePath
    }

    actual fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    actual fun deleteFile(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) file.delete() else false
    }

    actual fun rotateImageFile(filePath: String, degrees: Float): Boolean {
        val file = File(filePath)
        if (!file.exists()) return false

        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false
        val matrix = Matrix().apply { postRotate(degrees) }

        val rotatedBitmap = Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )

        return try {
            FileOutputStream(file).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            originalBitmap.recycle()
            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle()
            }
        }
    }
}
