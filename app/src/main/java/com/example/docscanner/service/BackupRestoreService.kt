package com.example.docscanner.service

import android.content.Context
import android.util.Log
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Folder
import com.example.docscanner.data.model.Page
import com.example.docscanner.data.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
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
    val message: String = ""
)

/**
 * Handles 100% offline, local backup and restore using standard ZIP archives.
 */
class BackupRestoreService(
    private val context: Context,
    private val repository: DocumentRepository,
    private val fileStorageService: FileStorageService
) {

    companion object {
        private const val MANIFEST_FILE_NAME = "backup_manifest.json"
        private const val BACKUP_VERSION = 1
    }

    /**
     * Creates a single standalone ZIP backup containing the database dump and all page images + PDFs.
     */
    suspend fun createBackupZip(): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(context.cacheDir, "DocScanner_Backup_$timestamp.zip")

        val allFolders = repository.getAllFoldersSync()
        // Gather all documents (including vault documents in the backup)
        val allDocs = mutableListOf<Document>()
        // Read directly from DB through DAO/sync query
        val foldersJson = JSONArray()
        for (f in allFolders) {
            foldersJson.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("color", f.color)
                put("createdAt", f.createdAt)
            })
        }

        val docsJson = JSONArray()
        val pagesJson = JSONArray()
        val imagesToZip = mutableListOf<File>()
        val pdfsToZip = mutableListOf<File>()
        val thumbsToZip = mutableListOf<File>()

        // Collect documents from database
        val cursor = (context.applicationContext as? com.example.docscanner.DocScannerApp)
            ?.container?.database?.readableDatabase?.rawQuery("SELECT * FROM documents", null)

        cursor?.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val titleIdx = c.getColumnIndexOrThrow("title")
            val categoryIdx = c.getColumnIndexOrThrow("category")
            val createdAtIdx = c.getColumnIndexOrThrow("createdAt")
            val modifiedAtIdx = c.getColumnIndexOrThrow("modifiedAt")
            val pageCountIdx = c.getColumnIndexOrThrow("pageCount")
            val thumbnailPathIdx = c.getColumnIndexOrThrow("thumbnailPath")
            val pdfPathIdx = c.getColumnIndexOrThrow("pdfPath")
            val extractedTextIdx = c.getColumnIndexOrThrow("extractedText")
            val isPinnedIdx = c.getColumnIndex("isPinned")
            val tagsIdx = c.getColumnIndex("tags")
            val isVaultIdx = c.getColumnIndex("isVault")
            val folderIdIdx = c.getColumnIndex("folderId")

            while (c.moveToNext()) {
                val docId = c.getString(idIdx)
                val isPinned = if (isPinnedIdx >= 0) c.getInt(isPinnedIdx) == 1 else false
                val tags = if (tagsIdx >= 0) c.getString(tagsIdx) else ""
                val isVault = if (isVaultIdx >= 0) c.getInt(isVaultIdx) == 1 else false
                val folderId = if (folderIdIdx >= 0) c.getString(folderIdIdx) else null
                val thumbPath = c.getString(thumbnailPathIdx)
                val pdfPath = c.getString(pdfPathIdx)

                docsJson.put(JSONObject().apply {
                    put("id", docId)
                    put("title", c.getString(titleIdx))
                    put("category", c.getString(categoryIdx))
                    put("createdAt", c.getLong(createdAtIdx))
                    put("modifiedAt", c.getLong(modifiedAtIdx))
                    put("pageCount", c.getInt(pageCountIdx))
                    put("thumbnailFileName", File(thumbPath).name)
                    put("pdfFileName", File(pdfPath).name)
                    put("extractedText", c.getString(extractedTextIdx))
                    put("isPinned", isPinned)
                    put("tags", tags)
                    put("isVault", isVault)
                    put("folderId", folderId ?: "")
                })

                if (thumbPath.isNotBlank()) File(thumbPath).takeIf { it.exists() }?.let { thumbsToZip.add(it) }
                if (pdfPath.isNotBlank()) File(pdfPath).takeIf { it.exists() }?.let { pdfsToZip.add(it) }

                // Pages for this doc
                val pages = repository.getPagesForDocumentSync(docId)
                for (p in pages) {
                    pagesJson.put(JSONObject().apply {
                        put("id", p.id)
                        put("documentId", p.documentId)
                        put("pageIndex", p.pageIndex)
                        put("imageFileName", File(p.imagePath).name)
                        put("originalImageFileName", File(p.originalImagePath).name)
                        put("extractedText", p.extractedText)
                        put("createdAt", p.createdAt)
                    })
                    if (p.imagePath.isNotBlank()) File(p.imagePath).takeIf { it.exists() }?.let { imagesToZip.add(it) }
                    if (p.originalImagePath.isNotBlank() && p.originalImagePath != p.imagePath) {
                        File(p.originalImagePath).takeIf { it.exists() }?.let { imagesToZip.add(it) }
                    }
                }
            }
        }

        val manifestJson = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("timestamp", System.currentTimeMillis())
            put("documents", docsJson)
            put("pages", pagesJson)
            put("folders", foldersJson)
        }

        // Write ZIP archive
        ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zos ->
            // 1. Manifest
            val manifestBytes = manifestJson.toString(2).toByteArray(Charsets.UTF_8)
            zos.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
            zos.write(manifestBytes)
            zos.closeEntry()

            // 2. Images
            for (img in imagesToZip.distinctBy { it.name }) {
                zos.putNextEntry(ZipEntry("images/${img.name}"))
                img.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }

            // 3. Thumbnails
            for (thumb in thumbsToZip.distinctBy { it.name }) {
                zos.putNextEntry(ZipEntry("thumbnails/${thumb.name}"))
                thumb.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }

            // 4. PDFs
            for (pdf in pdfsToZip.distinctBy { it.name }) {
                zos.putNextEntry(ZipEntry("pdfs/${pdf.name}"))
                pdf.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }

        backupFile
    }

    /**
     * Restores a backup from an input stream (e.g. from file picker Uri).
     */
    suspend fun restoreBackup(inputStream: InputStream): RestoreResult = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "restore_temp_${UUID.randomUUID()}")
        tempDir.mkdirs()

        try {
            // Unpack ZIP
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val destFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Read manifest
            val manifestFile = File(tempDir, MANIFEST_FILE_NAME)
            if (!manifestFile.exists()) {
                return@withContext RestoreResult(false, 0, "Invalid backup: Missing manifest")
            }

            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            val foldersArray = manifest.optJSONArray("folders") ?: JSONArray()
            val docsArray = manifest.optJSONArray("documents") ?: JSONArray()
            val pagesArray = manifest.optJSONArray("pages") ?: JSONArray()

            // Restore folders
            for (i in 0 until foldersArray.length()) {
                val fObj = foldersArray.getJSONObject(i)
                val folder = Folder(
                    id = fObj.getString("id"),
                    name = fObj.getString("name"),
                    color = fObj.optInt("color", 0),
                    createdAt = fObj.optLong("createdAt", System.currentTimeMillis())
                )
                repository.insertFolder(folder)
            }

            // Target app storage directories
            val pagesDir = File(context.filesDir, "pages").apply { mkdirs() }
            val thumbsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val pdfsDir = File(context.filesDir, "pdfs").apply { mkdirs() }

            var restoredDocs = 0

            // Restore documents
            for (i in 0 until docsArray.length()) {
                val dObj = docsArray.getJSONObject(i)
                val docId = dObj.getString("id")
                val thumbFileName = dObj.optString("thumbnailFileName", "")
                val pdfFileName = dObj.optString("pdfFileName", "")

                val destThumbFile = File(thumbsDir, thumbFileName)
                val srcThumb = File(tempDir, "thumbnails/$thumbFileName")
                if (srcThumb.exists()) srcThumb.copyTo(destThumbFile, overwrite = true)

                val destPdfFile = File(pdfsDir, pdfFileName)
                val srcPdf = File(tempDir, "pdfs/$pdfFileName")
                if (srcPdf.exists()) srcPdf.copyTo(destPdfFile, overwrite = true)

                val doc = Document(
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
                    isVault = dObj.optBoolean("isVault", false),
                    folderId = dObj.optString("folderId", "").takeIf { it.isNotBlank() }
                )
                repository.saveDocument(doc)
                restoredDocs++
            }

            // Restore pages
            val pagesToSave = mutableListOf<Page>()
            for (i in 0 until pagesArray.length()) {
                val pObj = pagesArray.getJSONObject(i)
                val imgFileName = pObj.getString("imageFileName")
                val origFileName = pObj.optString("originalImageFileName", imgFileName)

                val destImg = File(pagesDir, imgFileName)
                val srcImg = File(tempDir, "images/$imgFileName")
                if (srcImg.exists()) srcImg.copyTo(destImg, overwrite = true)

                val destOrig = File(pagesDir, origFileName)
                val srcOrig = File(tempDir, "images/$origFileName")
                if (srcOrig.exists() && srcOrig != srcImg) srcOrig.copyTo(destOrig, overwrite = true)

                pagesToSave.add(
                    Page(
                        id = pObj.getString("id"),
                        documentId = pObj.getString("documentId"),
                        pageIndex = pObj.getInt("pageIndex"),
                        imagePath = destImg.absolutePath,
                        originalImagePath = destOrig.absolutePath,
                        extractedText = pObj.optString("extractedText", ""),
                        createdAt = pObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (pagesToSave.isNotEmpty()) {
                repository.savePages(pagesToSave)
            }

            RestoreResult(true, restoredDocs, "Successfully restored $restoredDocs documents!")
        } catch (e: Exception) {
            Log.e("BackupRestoreService", "Error during restore", e)
            RestoreResult(false, 0, "Restore failed: ${e.localizedMessage}")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
