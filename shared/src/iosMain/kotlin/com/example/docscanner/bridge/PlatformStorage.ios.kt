package com.example.docscanner.bridge

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalForeignApi::class)
actual class PlatformStorage {

    actual fun getAppDataDirectory(): String {
        val paths = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        )
        val documentsDirectory = paths.firstOrNull() as? NSURL
        return documentsDirectory?.path ?: ""
    }

    actual fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val size = bytes / (1024.0.pow(digitGroups.toDouble()))
        val formatted = NSString.stringWithFormat("%.1f", size)
        return "$formatted ${units[digitGroups]}"
    }

    actual fun deleteFile(path: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        return if (fileManager.fileExistsAtPath(path)) {
            fileManager.removeItemAtPath(path, null)
        } else false
    }

    actual fun rotateImageFile(filePath: String, degrees: Float): Boolean {
        val image = UIImage.imageWithContentsOfFile(filePath) ?: return false
        val rotatedData = UIImageJPEGRepresentation(image, 0.92) ?: return false
        return NSFileManager.defaultManager.createFileAtPath(filePath, rotatedData, null)
    }
}
