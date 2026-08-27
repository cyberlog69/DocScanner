package com.example.docscanner.bridge

expect class PlatformStorage {
    fun getAppDataDirectory(): String
    fun formatFileSize(bytes: Long): String
    fun deleteFile(path: String): Boolean
    fun rotateImageFile(filePath: String, degrees: Float): Boolean
}
