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

import kotlinx.cinterop.useContents

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
        val normalizedDegrees = ((degrees % 360f) + 360f) % 360f
        val isSwapDimension = normalizedDegrees == 90f || normalizedDegrees == 270f
        val (originalWidth, originalHeight) = image.size.useContents { width to height }
        val targetWidth = if (isSwapDimension) originalHeight else originalWidth
        val targetHeight = if (isSwapDimension) originalWidth else originalHeight
        val targetSize = platform.CoreGraphics.CGSizeMake(targetWidth, targetHeight)

        platform.UIKit.UIGraphicsBeginImageContextWithOptions(targetSize, false, image.scale)
        val context = platform.UIKit.UIGraphicsGetCurrentContext() ?: run {
            platform.UIKit.UIGraphicsEndImageContext()
            return false
        }

        platform.CoreGraphics.CGContextTranslateCTM(context, targetWidth / 2.0, targetHeight / 2.0)
        val radians = normalizedDegrees.toDouble() * (kotlin.math.PI / 180.0)
        platform.CoreGraphics.CGContextRotateCTM(context, radians)
        image.drawInRect(platform.CoreGraphics.CGRectMake(-originalWidth / 2.0, -originalHeight / 2.0, originalWidth, originalHeight))

        val rotatedImage = platform.UIKit.UIGraphicsGetImageFromCurrentImageContext()
        platform.UIKit.UIGraphicsEndImageContext()

        if (rotatedImage == null) return false
        val rotatedData = UIImageJPEGRepresentation(rotatedImage, 0.92) ?: return false
        return NSFileManager.defaultManager.createFileAtPath(filePath, rotatedData, null)
    }
}


