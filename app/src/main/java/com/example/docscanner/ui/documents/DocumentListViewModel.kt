package com.example.docscanner.ui.documents

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Folder
import com.example.docscanner.data.model.Page
import com.example.docscanner.data.model.SortOrder
import com.example.docscanner.data.model.applySort
import com.example.docscanner.data.pref.ScannerPreferences
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.service.PageData
import com.example.docscanner.service.PdfGenerator
import com.example.docscanner.BuildConfig
import com.example.docscanner.model.AppUpdateInfo
import com.example.docscanner.model.ScannerResult
import com.example.docscanner.service.AppUpdateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed interface UpdateCheckState {
    object Idle : UpdateCheckState
    object Checking : UpdateCheckState
    data class Available(val updateInfo: AppUpdateInfo) : UpdateCheckState
    object UpToDate : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DocumentListViewModel(
    private val repository: DocumentRepository,
    private val fileStorageService: FileStorageService,
    private val pdfGenerator: PdfGenerator,
    private val preferences: ScannerPreferences,
    private val appUpdateService: AppUpdateService
) : ViewModel() {

    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _isBannerDismissed = MutableStateFlow(false)
    val isBannerDismissed: StateFlow<Boolean> = _isBannerDismissed.asStateFlow()

    init {
        if (preferences.settings.value.autoCheckUpdates) {
            checkForUpdates(BuildConfig.VERSION_NAME, isManual = false)
        }
    }

    fun dismissUpdateBanner() {
        _isBannerDismissed.value = true
    }

    fun resetUpdateState() {
        _updateCheckState.value = UpdateCheckState.Idle
        _downloadProgress.value = null
    }

    fun checkForUpdates(currentVersion: String = BuildConfig.VERSION_NAME, isManual: Boolean = false) {
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            when (val result = appUpdateService.checkForUpdates(currentVersion)) {
                is ScannerResult.Success -> {
                    if (result.data.hasUpdate) {
                        _updateCheckState.value = UpdateCheckState.Available(result.data)
                        _isBannerDismissed.value = false
                    } else {
                        _updateCheckState.value = UpdateCheckState.UpToDate
                    }
                }
                is ScannerResult.Failure -> {
                    if (isManual) {
                        _updateCheckState.value = UpdateCheckState.Error(result.message)
                    } else {
                        _updateCheckState.value = UpdateCheckState.Idle
                    }
                }
            }
        }
    }

    fun downloadAndInstall(appUpdateInfo: AppUpdateInfo, context: Context) {
        val downloadUrl = appUpdateInfo.downloadUrl ?: return
        viewModelScope.launch {
            _downloadProgress.value = 0f
            when (val result = appUpdateService.downloadApk(downloadUrl) { progress, _, _ ->
                _downloadProgress.value = progress
            }) {
                is ScannerResult.Success -> {
                    _downloadProgress.value = 1f
                    appUpdateService.installApk(context, result.data)
                }
                is ScannerResult.Failure -> {
                    _downloadProgress.value = null
                    _updateCheckState.value = UpdateCheckState.Error(result.message)
                }
            }
        }
    }

private data class DocumentFilter(
    val category: DocumentCategory,
    val query: String,
    val folderId: String?,
    val isVault: Boolean
)

    private val _selectedCategory = MutableStateFlow(DocumentCategory.ALL)
    val selectedCategory: StateFlow<DocumentCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DEFAULT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _selectedDocIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedDocIds: StateFlow<Set<String>> = _selectedDocIds.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _isVaultMode = MutableStateFlow(false)
    val isVaultMode: StateFlow<Boolean> = _isVaultMode.asStateFlow()

    val folders: StateFlow<List<Folder>> = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<Document>> = combine(
        _selectedCategory,
        _searchQuery.debounce(300),
        _selectedFolderId,
        _isVaultMode
    ) { category, query, folderId, isVault ->
        DocumentFilter(category, query, folderId, isVault)
    }
        .flatMapLatest { filter ->
            when {
                filter.isVault -> {
                    if (filter.query.isNotBlank()) {
                        repository.getVaultDocuments().map { list ->
                            list.filter { doc ->
                                doc.title.contains(filter.query, ignoreCase = true) ||
                                doc.extractedText.contains(filter.query, ignoreCase = true)
                            }
                        }
                    } else {
                        repository.getVaultDocuments()
                    }
                }
                filter.folderId != null -> {
                    if (filter.query.isNotBlank()) {
                        repository.getDocumentsByFolder(filter.folderId).map { list ->
                            list.filter { doc ->
                                doc.title.contains(filter.query, ignoreCase = true) ||
                                doc.extractedText.contains(filter.query, ignoreCase = true)
                            }
                        }
                    } else {
                        repository.getDocumentsByFolder(filter.folderId)
                    }
                }
                filter.query.isNotBlank() -> repository.searchDocuments(filter.query)
                filter.category == DocumentCategory.ALL -> repository.getAllDocuments()
                else -> repository.getDocumentsByCategory(filter.category)
            }
        }
        .combine(_sortOrder) { docs, sort ->
            docs.applySort(sort)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStorageBytes: StateFlow<Long> = documents
        .map {
            withContext(Dispatchers.IO) {
                fileStorageService.getTotalStorageUsed()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setCategory(category: DocumentCategory) {
        _selectedCategory.update { category }
        _selectedFolderId.value = null
        _isVaultMode.value = false
    }

    fun setSearchQuery(query: String) = _searchQuery.update { query }

    fun setSortOrder(order: SortOrder) = _sortOrder.update { order }

    fun toggleViewMode() = _isGridView.update { !it }

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
        if (folderId != null) {
            _isVaultMode.value = false
        }
    }

    fun setVaultMode(enabled: Boolean) {
        _isVaultMode.value = enabled
        if (enabled) {
            _selectedFolderId.value = null
        }
    }

    fun createFolder(name: String, color: Int = 0) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val folder = Folder(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                color = color,
                createdAt = System.currentTimeMillis()
            )
            repository.insertFolder(folder)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = null
            }
            repository.deleteFolder(folderId)
        }
    }

    // ── Document Operations ───────────────────────────────────────────────────

    fun togglePin(document: Document) {
        viewModelScope.launch {
            repository.togglePin(document.id, !document.isPinned)
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            fileStorageService.deleteDocumentFiles(document.id)
            repository.deleteDocument(document)
        }
    }

    fun renameDocument(id: String, newTitle: String) {
        viewModelScope.launch { repository.renameDocument(id, newTitle) }
    }

    // ── Multi-select & Batch Operations ───────────────────────────────────────

    fun toggleSelection(documentId: String) {
        _selectedDocIds.update { current ->
            if (current.contains(documentId)) current - documentId else current + documentId
        }
    }

    fun selectAll(allDocIds: List<String>) {
        _selectedDocIds.update { allDocIds.toSet() }
    }

    fun clearSelection() {
        _selectedDocIds.update { emptySet() }
    }

    fun deleteSelectedDocuments(documents: List<Document>) {
        val idsToDelete = _selectedDocIds.value
        viewModelScope.launch {
            documents.filter { it.id in idsToDelete }.forEach { doc ->
                fileStorageService.deleteDocumentFiles(doc.id)
                repository.deleteDocument(doc)
            }
            clearSelection()
        }
    }

    fun changeCategoryForSelected(newCategory: DocumentCategory) {
        val targetIds = _selectedDocIds.value
        viewModelScope.launch {
            targetIds.forEach { id ->
                repository.updateCategory(id, newCategory)
            }
            clearSelection()
        }
    }

    fun togglePinForSelected(pin: Boolean) {
        val targetIds = _selectedDocIds.value
        viewModelScope.launch {
            targetIds.forEach { id ->
                repository.togglePin(id, pin)
            }
            clearSelection()
        }
    }

    // ── Merge Selected Documents ──────────────────────────────────────────────

    /**
     * Merges all selected documents into a single new document (pages combined in
     * chronological order). The originals are deleted and [onComplete] is invoked
     * with the new document id.
     */
    fun mergeSelectedDocuments(documents: List<Document>, onComplete: (String) -> Unit) {
        val targetIds = _selectedDocIds.value
        viewModelScope.launch(Dispatchers.IO) {
            val selected = documents.filter { it.id in targetIds }.sortedBy { it.createdAt }
            if (selected.size < 2) return@launch

            val newId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val imageQuality = when (preferences.settings.value.cameraQuality) {
                com.example.docscanner.data.pref.CameraQuality.UHD_4K -> 100
                com.example.docscanner.data.pref.CameraQuality.HIGH -> 92
                com.example.docscanner.data.pref.CameraQuality.STANDARD -> 80
            }

            try {
                val allPages = mutableListOf<Page>()
                for (doc in selected) {
                    allPages += repository.getPagesForDocumentSync(doc.id)
                }
                if (allPages.isEmpty()) return@launch

                // Copy page images into the new document folder and re-index them.
                val reindexed = allPages.mapIndexedNotNull { index, page ->
                    val bitmap = fileStorageService.loadBitmap(page.imagePath) ?: return@mapIndexedNotNull null
                    val newPath = fileStorageService.savePageImage(bitmap, newId, index, quality = imageQuality)
                    bitmap.recycle()
                    page.copy(
                        id = UUID.randomUUID().toString(),
                        documentId = newId,
                        pageIndex = index,
                        imagePath = newPath,
                        originalImagePath = newPath
                    )
                }
                if (reindexed.isEmpty()) return@launch

                val thumbnailPath = reindexed.firstOrNull()?.let { firstPage ->
                    fileStorageService.loadBitmap(firstPage.imagePath)?.let { bmp ->
                        fileStorageService.saveThumbnail(bmp, newId).also { bmp.recycle() }
                    } ?: ""
                } ?: ""

                val combinedText = reindexed.joinToString("\n\n--- Page Break ---\n\n") { it.extractedText }

                // Regenerate a searchable PDF for the merged document.
                val pageDataList = reindexed.mapNotNull { p ->
                    fileStorageService.loadBitmap(p.imagePath)?.let { PageData(it, p.extractedText) }
                }
                val pdfBytes = pdfGenerator.generatePdf(
                    pages = pageDataList,
                    title = selected.first().title,
                    quality = preferences.settings.value.pdfQuality
                )
                val pdfPath = fileStorageService.savePdf(pdfBytes, newId)
                pageDataList.forEach { it.bitmap.recycle() }

                val mergedDoc = Document(
                    id = newId,
                    title = "Merged: ${selected.first().title}",
                    category = selected.first().category,
                    createdAt = now,
                    modifiedAt = now,
                    pageCount = reindexed.size,
                    thumbnailPath = thumbnailPath,
                    pdfPath = pdfPath,
                    extractedText = combinedText
                )
                repository.saveDocument(mergedDoc)
                repository.savePages(reindexed)

                // Delete the originals now that the merge is complete.
                selected.forEach { doc ->
                    fileStorageService.deleteDocumentFiles(doc.id)
                    repository.deleteDocument(doc)
                }

                clearSelection()
                withContext(Dispatchers.Main) { onComplete(newId) }
            } catch (e: Exception) {
                // Roll back the partially-created merged document on failure.
                fileStorageService.deleteDocumentFiles(newId)
                withContext(Dispatchers.Main) { onComplete("") }
            }
        }
    }

    // ── Batch ZIP Export ──────────────────────────────────────────────────────

    /**
     * Packages the selected documents' PDF files into a single ZIP in the cache
     * directory and hands it to [onReady] for sharing.
     */
    fun exportSelectedAsZip(
        documents: List<Document>,
        context: Context,
        onReady: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val targetIds = _selectedDocIds.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selected = documents.filter { it.id in targetIds }
                if (selected.isEmpty()) return@launch

                val zipFile = File(context.cacheDir, "DocScanner_export_${System.currentTimeMillis()}.zip")
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    selected.forEachIndexed { index, doc ->
                        val pdf = fileStorageService.getPdfFile(doc.pdfPath)
                        if (pdf != null && pdf.exists()) {
                            val safeName = doc.title
                                .replace(Regex("[^A-Za-z0-9 _\\-]"), "")
                                .ifBlank { "document" }
                            zos.putNextEntry(ZipEntry("${index + 1}_${safeName}.pdf"))
                            pdf.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }

                clearSelection()
                withContext(Dispatchers.Main) { onReady(zipFile) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Export failed") }
            }
        }
    }
}
