package com.example.docscanner.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Page
import com.example.docscanner.data.pref.PdfQuality
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.service.PageData
import com.example.docscanner.service.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DocumentDetailViewModel(
    private val repository: DocumentRepository,
    private val fileStorageService: FileStorageService,
    private val pdfGenerator: PdfGenerator,
    private val documentId: String
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentDetailState())
    val state: StateFlow<DocumentDetailState> = _state.asStateFlow()

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            val doc = repository.getDocumentById(documentId)
            val pages = repository.getPagesForDocumentSync(documentId)
            _state.update { it.copy(document = doc, pages = pages) }
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

    companion object {
        fun provideFactory(
            repository: DocumentRepository,
            fileStorageService: FileStorageService,
            pdfGenerator: PdfGenerator,
            documentId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DocumentDetailViewModel(repository, fileStorageService, pdfGenerator, documentId) as T
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
    val exportError: String? = null
)
