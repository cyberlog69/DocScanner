package com.example.docscanner.service

import android.content.Context
import android.util.Log
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Folder
import com.example.docscanner.data.model.Page
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.model.ScannerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class RestoreResult(
    val success: Boolean,
    val restoredDocumentsCount: Int = 0,
    val skippedVaultCount: Int = 0,
    val message: String = ""
)

data class BackupResult(
    val file: File,
    val documentCount: Int,
    val excludedVaultCount: Int
)

/**
 * Handles 100% offline, local backup and restore using standard ZIP archives.
 *
 * Vault documents are intentionally excluded: their content is encrypted with a
 * device-bound Android Keystore key, so it could never be decrypted after a restore
 * on another device.
 */
class BackupRestoreService(
    private val context: Context,
    private val repository: DocumentRepository,
    private val fileStorageService: FileStorageService
) {

    companion object {
        private const val MANIFEST_FILE_NAME = "backup_manifest.json"
        private const val BACKUP_VERSION = 1

        /** Guards against decompression bombs from a crafted archive. */
        private const val MAX_BACKUP_ENTRIES = 20_000
        private const val MAX_BACKUP_UNCOMPRESSED_BYTES = 2L * 1024 * 1024 * 1024
    }

    /**
     * Creates a single standalone ZIP backup containing the database dump and all page images + PDFs.
     */
    suspend fun createBackupZip(): BackupResult = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(context.cacheDir, "DocScanner_Backup_$timestamp.zip")

        val allFolders = repository.getAllFoldersSync()
        val allDocuments = repository.getAllDocumentsSync()
        val vaultDocuments = allDocuments.filter { it.isVault }
        val backupDocuments = allDocuments.filterNot { it.isVault }

        val foldersJson = JSONArray()
        for (f in allFolders) {
            foldersJson.put(
                JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("color", f.color)
                    put("createdAt", f.createdAt)
                }
            )
        }

        val docsJson = JSONArray()
        val pagesJson = JSONArray()
        val imagesToZip = mutableListOf<File>()
        val pdfsToZip = mutableListOf<File>()
        val thumbsToZip = mutableListOf<File>()

        for (doc in backupDocuments) {
            docsJson.put(
                JSONObject().apply {
                    put("id", doc.id)
                    put("title", doc.title)
                    put("category", doc.category.name)
                    put("createdAt", doc.createdAt)
                    put("modifiedAt", doc.modifiedAt)
                    put("pageCount", doc.pageCount)
                    put("thumbnailFileName", File(doc.thumbnailPath).name)
                    put("pdfFileName", File(doc.pdfPath).name)
                    put("extractedText", doc.extractedText)
                    put("isPinned", doc.isPinned)
                    put("tags", doc.tagsToDbString())
                    put("isVault", false)
                    put("folderId", doc.folderId ?: "")
                }
            )

            if (doc.thumbnailPath.isNotBlank()) {
                File(doc.thumbnailPath).takeIf { it.exists() }?.let { thumbsToZip.add(it) }
            }
            if (doc.pdfPath.isNotBlank()) {
                File(doc.pdfPath).takeIf { it.exists() }?.let { pdfsToZip.add(it) }
            }

            for (p in repository.getPagesForDocumentSync(doc.id)) {
                pagesJson.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("documentId", p.documentId)
                        put("pageIndex", p.pageIndex)
                        put("imageFileName", File(p.imagePath).name)
                        put("originalImageFileName", File(p.originalImagePath).name)
                        put("extractedText", p.extractedText)
                        put("createdAt", p.createdAt)
                    }
                )
                if (p.imagePath.isNotBlank()) {
                    File(p.imagePath).takeIf { it.exists() }?.let { imagesToZip.add(it) }
                }
                if (p.originalImagePath.isNotBlank() && p.originalImagePath != p.imagePath) {
                    File(p.originalImagePath).takeIf { it.exists() }?.let { imagesToZip.add(it) }
                }
            }
        }

        val manifestJson = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("timestamp", System.currentTimeMillis())
            put("excludedVaultDocuments", vaultDocuments.size)
            put("documents", docsJson)
            put("pages", pagesJson)
            put("folders", foldersJson)
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zos ->
            val manifestBytes = manifestJson.toString(2).toByteArray(Charsets.UTF_8)
            zos.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
            zos.write(manifestBytes)
            zos.closeEntry()

            for (img in imagesToZip.distinctBy { it.name }) {
                zos.putNextEntry(ZipEntry("images/${img.name}"))
                img.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }

            for (thumb in thumbsToZip.distinctBy { it.name }) {
                zos.putNextEntry(ZipEntry("thumbnails/${thumb.name}"))
                thumb.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }

            for (pdf in pdfsToZip.distinctBy { it.name }) {
                zos.putNextEntry(ZipEntry("pdfs/${pdf.name}"))
                pdf.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }

        BackupResult(
            file = backupFile,
            documentCount = backupDocuments.size,
            excludedVaultCount = vaultDocuments.size
        )
    }

    /**
     * Restores a backup from an input stream (e.g. from file picker Uri).
     */
    suspend fun restoreBackup(inputStream: InputStream): RestoreResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "restore_temp_${UUID.randomUUID()}")
        tempDir.mkdirs()

        try {
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val canonicalTarget = tempDir.canonicalPath + File.separator
                var entryCount = 0
                var totalBytes = 0L
                val buffer = ByteArray(64 * 1024)

                while (entry != null) {
                    entryCount++
                    if (entryCount > MAX_BACKUP_ENTRIES) {
                        throw SecurityException("Backup archive contains too many entries")
                    }

                    val destFile = File(tempDir, entry.name)
                    if (!destFile.canonicalPath.startsWith(canonicalTarget)) {
                        throw SecurityException("Zip entry is outside of target directory: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            while (true) {
                                val read = zis.read(buffer)
                                if (read < 0) break
                                totalBytes += read
                                if (totalBytes > MAX_BACKUP_UNCOMPRESSED_BYTES) {
                                    throw SecurityException("Backup archive exceeds the size limit")
                                }
                                fos.write(buffer, 0, read)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val manifestFile = File(tempDir, MANIFEST_FILE_NAME)
            if (!manifestFile.exists()) {
                return@withContext RestoreResult(false, 0, 0, "Invalid backup: Missing manifest")
            }

            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            val foldersArray = manifest.optJSONArray("folders") ?: JSONArray()
            val docsArray = manifest.optJSONArray("documents") ?: JSONArray()
            val pagesArray = manifest.optJSONArray("pages") ?: JSONArray()

            for (i in 0 until foldersArray.length()) {
                val fObj = foldersArray.getJSONObject(i)
                val folderResult = repository.insertFolder(
                    Folder(
                        id = fObj.getString("id"),
                        name = fObj.getString("name"),
                        color = fObj.optInt("color", 0),
                        createdAt = fObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
                if (folderResult is ScannerResult.Failure) {
                    Log.w("BackupRestoreService", "Skipped folder ${fObj.optString("name")}: ${folderResult.message}")
                }
            }

            val documentsDir = File(context.filesDir, "documents").apply { mkdirs() }
            val thumbsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }

            var restoredDocs = 0
            var skippedVaultDocs = 0
            var failedDocs = 0
            val restoredDocIds = mutableSetOf<String>()

            for (i in 0 until docsArray.length()) {
                val dObj = docsArray.getJSONObject(i)
                val docId = dObj.getString("id")

                // Older backups may contain vault entries whose key no longer exists.
                if (dObj.optBoolean("isVault", false)) {
                    skippedVaultDocs++
                    continue
                }

                val thumbFileName = dObj.optString("thumbnailFileName", "")
                val pdfFileName = dObj.optString("pdfFileName", "")

                val destThumbFile = File(thumbsDir, thumbFileName)
                val srcThumb = File(tempDir, "thumbnails/$thumbFileName")
                if (srcThumb.exists()) srcThumb.copyTo(destThumbFile, overwrite = true)

                val docDir = File(documentsDir, docId).apply { mkdirs() }
                val destPdfFile = File(docDir, pdfFileName)
                val srcPdf = File(tempDir, "pdfs/$pdfFileName")
                if (srcPdf.exists()) srcPdf.copyTo(destPdfFile, overwrite = true)

                val saveResult = repository.saveDocument(
                    Document(
                        id = docId,
                        title = dObj.getString("title"),
                        category = DocumentCategory.fromName(dObj.getString("category")),
                        createdAt = dObj.getLong("createdAt"),
                        modifiedAt = dObj.optLong("modifiedAt", System.currentTimeMillis()),
                        pageCount = dObj.getInt("pageCount"),
                        thumbnailPath = destThumbFile.absolutePath,
                        pdfPath = destPdfFile.absolutePath,
                        extractedText = dObj.optString("extractedText", ""),
                        isPinned = dObj.optBoolean("isPinned", false),
                        tags = Document.parseTagsString(dObj.optString("tags", "")),
                        isVault = false,
                        folderId = dObj.optString("folderId", "").takeIf { it.isNotBlank() }
                    )
                )
                if (saveResult is ScannerResult.Failure) {
                    Log.e("BackupRestoreService", "Failed to restore document $docId: ${saveResult.message}")
                    failedDocs++
                    continue
                }
                restoredDocIds.add(docId)
                restoredDocs++
            }

            val pagesToSave = mutableListOf<Page>()
            for (i in 0 until pagesArray.length()) {
                val pObj = pagesArray.getJSONObject(i)
                val pageDocId = pObj.getString("documentId")
                if (pageDocId !in restoredDocIds) continue

                val imgFileName = pObj.getString("imageFileName")
                val origFileName = pObj.optString("originalImageFileName", imgFileName)
                val pageDir = File(documentsDir, pageDocId).apply { mkdirs() }

                val destImg = File(pageDir, imgFileName)
                val srcImg = File(tempDir, "images/$imgFileName")
                if (srcImg.exists()) srcImg.copyTo(destImg, overwrite = true)

                val destOrig = File(pageDir, origFileName)
                val srcOrig = File(tempDir, "images/$origFileName")
                if (srcOrig.exists() && srcOrig != srcImg) srcOrig.copyTo(destOrig, overwrite = true)

                pagesToSave.add(
                    Page(
                        id = pObj.getString("id"),
                        documentId = pageDocId,
                        pageIndex = pObj.getInt("pageIndex"),
                        imagePath = destImg.absolutePath,
                        originalImagePath = destOrig.absolutePath,
                        extractedText = pObj.optString("extractedText", ""),
                        createdAt = pObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            var failedPages = 0
            if (pagesToSave.isNotEmpty()) {
                val pagesResult = repository.savePages(pagesToSave)
                if (pagesResult is ScannerResult.Failure) {
                    Log.e("BackupRestoreService", "Failed to restore pages: ${pagesResult.message}")
                    failedPages = pagesToSave.size
                }
            }

            val vaultNote = if (skippedVaultDocs > 0) {
                " Skipped $skippedVaultDocs vault document(s) that cannot be decrypted on this device."
            } else {
                ""
            }
            val failureNote = when {
                failedDocs > 0 && failedPages > 0 ->
                    " $failedDocs document(s) and $failedPages page(s) could not be restored."
                failedDocs > 0 -> " $failedDocs document(s) could not be restored."
                failedPages > 0 -> " $failedPages page(s) could not be restored."
                else -> ""
            }
            RestoreResult(
                success = failedDocs == 0 && failedPages == 0,
                restoredDocumentsCount = restoredDocs,
                skippedVaultCount = skippedVaultDocs,
                message = "Restored $restoredDocs document(s).$vaultNote$failureNote"
            )
        } catch (e: Exception) {
            Log.e("BackupRestoreService", "Error during restore", e)
            RestoreResult(false, 0, 0, "Restore failed: ${e.localizedMessage}")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
