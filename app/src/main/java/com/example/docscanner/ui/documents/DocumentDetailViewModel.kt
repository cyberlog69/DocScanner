package com.example.docscanner.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Folder
import com.example.docscanner.data.model.Page
import com.example.docscanner.data.pref.PdfQuality
import com.example.docscanner.data.pref.ScannerPreferences
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.service.ExtractedReceiptData
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.service.OcrService
import com.example.docscanner.service.PageData
import com.example.docscanner.service.PdfAnnotationService
import com.example.docscanner.service.PdfGenerator
import com.example.docscanner.service.ReceiptParser
import com.example.docscanner.service.StampConfig
import com.example.docscanner.service.VaultEncryptionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DocumentDetailViewModel(
    private val repository: DocumentRepository,
    private val fileStorageService: FileStorageService,
    private val pdfGenerator: PdfGenerator,
    private val ocrService: OcrService,
    private val preferences: ScannerPreferences,
    private val documentId: String
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentDetailState())
    val state: StateFlow<DocumentDetailState> = _state.asStateFlow()

    init {
        loadDocument()
    }

    fun reload() {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            val doc = repository.getDocumentById(documentId)
            val pages = repository.getPagesForDocumentSync(documentId)
            val bytes = withContext(Dispatchers.IO) {
                fileStorageService.getDocumentStorageBytes(documentId)
            }
            val folders = repository.getAllFoldersSync()
            val parsedReceipt = if (doc != null && doc.extractedText.isNotBlank()) {
                ReceiptParser.parse(doc.extractedText)
            } else null

            _state.update {
                it.copy(
                    document = doc,
                    pages = pages,
                    storageBytes = bytes,
                    availableFolders = folders,
                    extractedReceiptData = parsedReceipt
                )
            }
        }
    }

    fun renameDocument(newTitle: String) {
        viewModelScope.launch {
            repository.renameDocument(documentId, newTitle)
            loadDocument()
        }
    }

    fun updateCategory(category: DocumentCategory) {
        viewModelScope.launch {
            repository.updateCategory(documentId, category)
            loadDocument()
        }
    }

    fun togglePin() {
        val currentDoc = _state.value.document ?: return
        viewModelScope.launch {
            repository.togglePin(documentId, !currentDoc.isPinned)
            loadDocument()
        }
    }

    fun addTag(rawTag: String) {
        val currentDoc = _state.value.document ?: return
        val cleanTag = rawTag.trim().removePrefix("#").trim()
        if (cleanTag.isEmpty() || currentDoc.tags.contains(cleanTag)) return
        val updatedTags = currentDoc.tags + cleanTag
        viewModelScope.launch {
            repository.updateTags(documentId, updatedTags)
            loadDocument()
        }
    }

    fun removeTag(tag: String) {
        val currentDoc = _state.value.document ?: return
        val updatedTags = currentDoc.tags.filterNot { it.equals(tag, ignoreCase = true) }
        viewModelScope.launch {
            repository.updateTags(documentId, updatedTags)
            loadDocument()
        }
    }

    fun deleteDocument(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value.document?.let { doc ->
                fileStorageService.deleteDocumentFiles(doc.id)
                repository.deleteDocument(doc)
            }
            onComplete()
        }
    }

    /**
     * Rotates a page on disk by [degrees] clockwise and refreshes thumbnail if first page.
     */
    fun rotatePage(page: Page, degrees: Float = 90f) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = fileStorageService.rotateImageFile(page.imagePath, degrees)
            if (success) {
                if (page.originalImagePath.isNotBlank() && page.originalImagePath != page.imagePath) {
                    fileStorageService.rotateImageFile(page.originalImagePath, degrees)
                }
                // If it's page 0, update document thumbnail
                if (page.pageIndex == 0) {
                    val newBitmap = fileStorageService.loadBitmap(page.imagePath)
                    if (newBitmap != null) {
                        fileStorageService.saveThumbnail(newBitmap, page.documentId)
                        newBitmap.recycle()
                    }
                }
                // Re-fetch document & pages
                loadDocument()
            }
        }
    }

    /**
     * Deletes a page. If it's the only page left in the document, deletes the document entirely.
     */
    fun deletePage(page: Page, onDocumentDeleted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPages = _state.value.pages
            if (currentPages.size <= 1) {
                // Delete whole document
                _state.value.document?.let { doc ->
                    fileStorageService.deleteDocumentFiles(doc.id)
                    repository.deleteDocument(doc)
                }
                withContext(Dispatchers.Main) {
                    onDocumentDeleted()
                }
            } else {
                // Delete single page file and db record
                fileStorageService.deletePageFile(page.imagePath)
                if (page.originalImagePath.isNotBlank() && page.originalImagePath != page.imagePath) {
                    fileStorageService.deletePageFile(page.originalImagePath)
                }
                repository.deletePage(page)

                // Re-index remaining pages
                val remaining = repository.getPagesForDocumentSync(documentId)
                val reindexed = remaining.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
                repository.savePages(reindexed)

                // Update thumbnail from new first page
                reindexed.firstOrNull()?.let { firstPage ->
                    val firstBitmap = fileStorageService.loadBitmap(firstPage.imagePath)
                    if (firstBitmap != null) {
                        fileStorageService.saveThumbnail(firstBitmap, documentId)
                        firstBitmap.recycle()
                    }
                }

                // Update document page count & metadata
                val doc = repository.getDocumentById(documentId)
                if (doc != null) {
                    val combinedText = reindexed.joinToString("\n") { it.extractedText }
                    repository.updateDocumentMeta(
                        id = doc.id,
                        pageCount = reindexed.size,
                        thumbnailPath = doc.thumbnailPath,
                        pdfPath = doc.pdfPath,
                        extractedText = combinedText
                    )
                }

                loadDocument()
            }
        }
    }

    /**
     * Re-runs OCR on a single page using the configured language and updates the document text index.
     */
    fun rerunOcrOnPage(page: Page) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isOcrRunning = true, ocrProgressText = "Running OCR on page ${page.pageIndex + 1}...") }
            try {
                val result = ocrService.recognizeTextFromFile(
                    File(page.imagePath),
                    preferences.settings.value.ocrLanguage
                )
                val updatedPage = page.copy(extractedText = result.fullText)
                repository.savePage(updatedPage)
                refreshDocumentText()
            } catch (e: Exception) {
                _state.update { it.copy(ocrError = e.message ?: "OCR failed") }
            } finally {
                _state.update { it.copy(isOcrRunning = false, ocrProgressText = "") }
            }
        }
    }

    /**
     * Re-runs OCR on every page and rebuilds the document-level text + FTS index.
     */
    fun rerunOcrAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isOcrRunning = true, ocrProgressText = "Running OCR on ${_state.value.pages.size} pages...") }
            try {
                val language = preferences.settings.value.ocrLanguage
                val updatedPages = _state.value.pages.map { page ->
                    val result = ocrService.recognizeTextFromFile(File(page.imagePath), language)
                    page.copy(extractedText = result.fullText)
                }
                repository.savePages(updatedPages)
                refreshDocumentText()
            } catch (e: Exception) {
                _state.update { it.copy(ocrError = e.message ?: "OCR failed") }
            } finally {
                _state.update { it.copy(isOcrRunning = false, ocrProgressText = "") }
            }
        }
    }

    private suspend fun refreshDocumentText() {
        val doc = _state.value.document ?: return
        val pages = repository.getPagesForDocumentSync(documentId)
        val combinedText = pages.joinToString("\n\n--- Page Break ---\n\n") { it.extractedText }
        repository.updateDocumentMeta(
            id = documentId,
            pageCount = pages.size,
            thumbnailPath = doc.thumbnailPath,
            pdfPath = doc.pdfPath,
            extractedText = combinedText
        )
        loadDocument()
    }

    /**
     * Splits the document into two at [splitIndex] (the page that becomes the first
     * page of the new second document). The original document keeps the first part;
     * a new document is created for the second part and returned via [onComplete].
     */
    fun splitDocument(splitIndex: Int, onComplete: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = _state.value.document ?: return@launch
            val pages = _state.value.pages
            if (pages.size < 2 || splitIndex <= 0 || splitIndex >= pages.size) return@launch

            val firstPart = pages.subList(0, splitIndex)
            val secondPart = pages.subList(splitIndex, pages.size)
            val now = System.currentTimeMillis()
            val newId = UUID.randomUUID().toString()

            try {
                // Second part → new document with images copied & re-indexed from 0.
                val secondPages = secondPart.mapIndexedNotNull { index, page ->
                    fileStorageService.copyPageToDocument(page.imagePath, newId, index)?.let { newPath ->
                        page.copy(
                            id = UUID.randomUUID().toString(),
                            documentId = newId,
                            pageIndex = index,
                            imagePath = newPath,
                            originalImagePath = newPath
                        )
                    }
                }
                if (secondPages.isEmpty()) {
                    withContext(Dispatchers.Main) { onComplete(null) }
                    return@launch
                }

                // First part keeps the original document id and files.
                val firstText = firstPart.joinToString("\n\n--- Page Break ---\n\n") { it.extractedText }
                val firstThumb = fileStorageService.loadBitmap(firstPart.first().imagePath)?.let { bmp ->
                    fileStorageService.saveThumbnail(bmp, doc.id).also { bmp.recycle() }
                } ?: doc.thumbnailPath

                val secondText = secondPages.joinToString("\n\n--- Page Break ---\n\n") { it.extractedText }
                val secondThumb = fileStorageService.loadBitmap(secondPages.first().imagePath)?.let { bmp ->
                    fileStorageService.saveThumbnail(bmp, newId).also { bmp.recycle() }
                } ?: ""

                // Regenerate searchable PDFs for both parts.
                val pdfQuality = preferences.settings.value.pdfQuality

                val firstPageData = firstPart.mapNotNull { p ->
                    fileStorageService.loadBitmap(p.imagePath)?.let { PageData(it, p.extractedText) }
                }
                val firstPdf = pdfGenerator.generatePdf(firstPageData, doc.title, pdfQuality)
                val firstPdfPath = fileStorageService.savePdf(firstPdf, doc.id, fileName = "doc_${doc.id}_split_part1.pdf")
                firstPageData.forEach { it.bitmap.recycle() }

                val secondPageData = secondPages.mapNotNull { p ->
                    fileStorageService.loadBitmap(p.imagePath)?.let { PageData(it, p.extractedText) }
                }
                val secondPdf = pdfGenerator.generatePdf(secondPageData, "${doc.title} (2)", pdfQuality)
                val secondPdfPath = fileStorageService.savePdf(secondPdf, newId)
                secondPageData.forEach { it.bitmap.recycle() }

                // Persist the trimmed original document.
                repository.updateDocumentMeta(
                    id = doc.id,
                    pageCount = firstPart.size,
                    thumbnailPath = firstThumb,
                    pdfPath = firstPdfPath,
                    extractedText = firstText
                )

                // Persist the new document + its pages.
                val newDoc = Document(
                    id = newId,
                    title = "${doc.title} (2)",
                    category = doc.category,
                    createdAt = now,
                    modifiedAt = now,
                    pageCount = secondPages.size,
                    thumbnailPath = secondThumb,
                    pdfPath = secondPdfPath,
                    extractedText = secondText
                )
                repository.saveDocument(newDoc)
                repository.savePages(secondPages)

                // Remove the moved pages from the original document's record.
                secondPart.forEach { repository.deletePage(it) }

                loadDocument()
                withContext(Dispatchers.Main) { onComplete(newId) }
            } catch (e: Exception) {
                fileStorageService.deleteDocumentFiles(newId)
                withContext(Dispatchers.Main) { onComplete(null) }
            }
        }
    }

    /**
     * Generates or re-exports the document PDF with the selected [PdfQuality].
     * Invokes [onReady] with the generated PDF [File].
     */
    fun exportPdfWithQuality(quality: PdfQuality, onReady: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = _state.value.document ?: return@launch
            val pages = _state.value.pages

            _state.update { it.copy(isExporting = true, exportProgressText = "Rendering ${quality.badge} PDF...") }

            try {
                val pageDataList = pages.mapNotNull { page ->
                    val bitmap = fileStorageService.loadBitmap(page.imagePath)
                    bitmap?.let { PageData(bitmap = it, extractedText = page.extractedText) }
                }

                if (pageDataList.isNotEmpty()) {
                    val pdfBytes = pdfGenerator.generatePdf(
                        pages = pageDataList,
                        title = doc.title,
                        quality = quality
                    )
                    val fileName = "doc_${doc.id}_${quality.name.lowercase()}.pdf"
                    val pdfPath = fileStorageService.savePdf(pdfBytes, doc.id, fileName = fileName)
                    val pdfFile = fileStorageService.getPdfFile(pdfPath)

                    pageDataList.forEach { it.bitmap.recycle() }

                    _state.update { it.copy(isExporting = false) }

                    if (pdfFile != null) {
                        withContext(Dispatchers.Main) {
                            onReady(pdfFile)
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isExporting = false, exportError = e.message) }
            }
        }
    }

    /**
     * Appends pages directly into this document from selected image URIs (e.g. Gallery picker).
     */
    fun appendPages(uris: List<Uri>, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isOcrRunning = true, ocrProgressText = "Adding ${uris.size} pages...") }
            try {
                val doc = repository.getDocumentById(documentId) ?: return@launch
                val existingPages = repository.getPagesForDocumentSync(documentId)
                val startIdx = existingPages.size

                val bitmaps = uris.mapNotNull { uri ->
                    try {
                        if (uri.scheme == "content") {
                            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                        } else {
                            uri.path?.let { BitmapFactory.decodeFile(it) }
                        }
                    } catch (_: Exception) { null }
                }
                if (bitmaps.isEmpty()) return@launch

                val settings = preferences.settings.value
                val imageQuality = when (settings.cameraQuality) {
                    com.example.docscanner.data.pref.CameraQuality.UHD_4K -> 100
                    com.example.docscanner.data.pref.CameraQuality.HIGH -> 92
                    com.example.docscanner.data.pref.CameraQuality.STANDARD -> 80
                }

                val newImagePaths = bitmaps.mapIndexed { index, bitmap ->
                    fileStorageService.savePageImage(bitmap, documentId, startIdx + index, quality = imageQuality)
                }

                val newOcrResults = if (settings.autoOcr) {
                    _state.update { it.copy(ocrProgressText = "Running OCR on ${bitmaps.size} new pages...") }
                    bitmaps.map { ocrService.recognizeText(it, settings.ocrLanguage) }
                } else {
                    emptyList()
                }

                val newPages = newImagePaths.mapIndexed { index, path ->
                    Page(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageIndex = startIdx + index,
                        imagePath = path,
                        originalImagePath = path,
                        extractedText = newOcrResults.getOrNull(index)?.fullText ?: "",
                        createdAt = System.currentTimeMillis()
                    )
                }
                repository.savePages(newPages)

                // Re-generate searchable PDF for all pages
                val allPages = repository.getPagesForDocumentSync(documentId)
                val allPageData = allPages.mapNotNull { page ->
                    fileStorageService.loadBitmap(page.imagePath)?.let { PageData(it, page.extractedText) }
                }
                val pdfBytes = pdfGenerator.generatePdf(allPageData, doc.title, settings.pdfQuality)
                val pdfPath = fileStorageService.savePdf(pdfBytes, documentId)
                allPageData.forEach { it.bitmap.recycle() }

                val combinedText = allPages.joinToString("\n\n--- Page Break ---\n\n") { it.extractedText }
                repository.updateDocumentMeta(
                    id = documentId,
                    pageCount = allPages.size,
                    thumbnailPath = doc.thumbnailPath,
                    pdfPath = pdfPath,
                    extractedText = combinedText
                )

                bitmaps.forEach { it.recycle() }
                loadDocument()
            } catch (e: Exception) {
                _state.update { it.copy(ocrError = e.message ?: "Failed to add pages") }
            } finally {
                _state.update { it.copy(isOcrRunning = false, ocrProgressText = "") }
            }
        }
    }

    /**
     * Toggles AES-256 GCM vault encryption on this document.
     */
    fun toggleVault(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = _state.value.document ?: return@launch
            val newVaultStatus = !doc.isVault
            val pages = _state.value.pages

            try {
                if (newVaultStatus) {
                    // Encrypt page images and PDF
                    for (page in pages) {
                        File(page.imagePath).takeIf { it.exists() }?.let { VaultEncryptionService.encryptFile(it) }
                        if (page.originalImagePath.isNotBlank() && page.originalImagePath != page.imagePath) {
                            File(page.originalImagePath).takeIf { it.exists() }?.let { VaultEncryptionService.encryptFile(it) }
                        }
                    }
                    if (doc.pdfPath.isNotBlank()) {
                        File(doc.pdfPath).takeIf { it.exists() }?.let { VaultEncryptionService.encryptFile(it) }
                    }
                    if (doc.thumbnailPath.isNotBlank()) {
                        File(doc.thumbnailPath).takeIf { it.exists() }?.let { VaultEncryptionService.encryptFile(it) }
                    }
                } else {
                    // Decrypt page images and PDF back to plaintext
                    for (page in pages) {
                        File(page.imagePath).takeIf { it.exists() }?.let { VaultEncryptionService.decryptFileInPlace(it) }
                        if (page.originalImagePath.isNotBlank() && page.originalImagePath != page.imagePath) {
                            File(page.originalImagePath).takeIf { it.exists() }?.let { VaultEncryptionService.decryptFileInPlace(it) }
                        }
                    }
                    if (doc.pdfPath.isNotBlank()) {
                        File(doc.pdfPath).takeIf { it.exists() }?.let { VaultEncryptionService.decryptFileInPlace(it) }
                    }
                    if (doc.thumbnailPath.isNotBlank()) {
                        File(doc.thumbnailPath).takeIf { it.exists() }?.let { VaultEncryptionService.decryptFileInPlace(it) }
                    }
                }

                repository.setVaultStatus(documentId, newVaultStatus)
                loadDocument()
                withContext(Dispatchers.Main) {
                    onComplete(newVaultStatus)
                }
            } catch (e: Exception) {
                _state.update { it.copy(exportError = "Vault toggle failed: ${e.message}") }
            }
        }
    }

    /**
     * Moves document to the specified folder (or null for root).
     */
    fun moveToFolder(folderId: String?) {
        viewModelScope.launch {
            repository.setDocumentFolder(documentId, folderId)
            loadDocument()
        }
    }

    /**
     * Stamps an annotation / watermark onto the document's PDF.
     */
    fun stampPdf(config: StampConfig, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = _state.value.document ?: return@launch
            if (doc.pdfPath.isBlank()) return@launch

            val pdfFile = File(doc.pdfPath)
            if (!pdfFile.exists()) return@launch

            val success = PdfAnnotationService.stampPdfFile(pdfFile, config)
            if (success) {
                loadDocument()
            }
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: DocumentRepository,
            fileStorageService: FileStorageService,
            pdfGenerator: PdfGenerator,
            ocrService: OcrService,
            preferences: ScannerPreferences,
            documentId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DocumentDetailViewModel(
                    repository,
                    fileStorageService,
                    pdfGenerator,
                    ocrService,
                    preferences,
                    documentId
                ) as T
            }
        }
    }
}

data class DocumentDetailState(
    val document: Document? = null,
    val pages: List<Page> = emptyList(),
    val currentPageIndex: Int = 0,
    val isExporting: Boolean = false,
    val exportProgressText: String = "",
    val exportError: String? = null,
    val isOcrRunning: Boolean = false,
    val ocrProgressText: String = "",
    val ocrError: String? = null,
    val storageBytes: Long = 0L,
    val availableFolders: List<Folder> = emptyList(),
    val extractedReceiptData: ExtractedReceiptData? = null
)
