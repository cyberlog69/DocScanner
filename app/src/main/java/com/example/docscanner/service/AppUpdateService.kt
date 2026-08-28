package com.example.docscanner.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.docscanner.model.AppUpdateInfo
import com.example.docscanner.model.ScannerResult
import com.example.docscanner.model.VersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service responsible for checking GitHub Releases for newer APK versions,
 * downloading update binaries with progress updates, and launching the native package installer.
 */
class AppUpdateService(private val context: Context) {

    companion object {
        private const val GITHUB_REPO = "cyberlog69/DocScanner"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        private const val USER_AGENT = "DocScanner-Android"
    }

    /**
     * Checks if a newer release is published on GitHub.
     */
    suspend fun checkForUpdates(currentVersionName: String): ScannerResult<AppUpdateInfo> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(LATEST_RELEASE_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext ScannerResult.Failure.GeneralError(
                    "Server returned HTTP $responseCode while checking for updates"
                )
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")
            val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases")
            val publishedAt = json.optString("published_at", "")

            var downloadUrl: String? = null
            var apkSizeBytes = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url")
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val hasUpdate = VersionComparator.isNewerVersion(currentVersionName, tagName)

            val updateInfo = AppUpdateInfo(
                hasUpdate = hasUpdate,
                currentVersion = currentVersionName,
                latestVersion = tagName.removePrefix("v").removePrefix("V"),
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                releasePageUrl = htmlUrl,
                publishedAt = publishedAt,
                apkSizeBytes = apkSizeBytes
            )

            ScannerResult.Success(updateInfo)
        } catch (e: Exception) {
            ScannerResult.Failure.GeneralError("Update check failed: ${e.localizedMessage ?: e.message}", e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Downloads the APK binary from GitHub asset URL into cache, reporting progress in real-time.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): ScannerResult<File> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(downloadUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", USER_AGENT)
                instanceFollowRedirects = true
            }

            val totalBytes = connection.contentLength.toLong()
            val updateDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            val apkFile = File(updateDir, "DocScanner-update.apk")
            if (apkFile.exists()) apkFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloadedBytes = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
                        onProgress(progress.coerceIn(0f, 1f), downloadedBytes, totalBytes)
                    }
                    output.flush()
                }
            }

            ScannerResult.Success(apkFile)
        } catch (e: Exception) {
            ScannerResult.Failure.StorageError("Failed to download APK update: ${e.localizedMessage ?: e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Prompts the native Android package installer to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(installIntent)
    }
}
