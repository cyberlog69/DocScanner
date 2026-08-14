package com.example.docscanner.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Page
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.service.FileStorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DocumentDetailViewModel(
    private val repository: DocumentRepository,
    private val fileStorageService: FileStorageService,
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

    companion object {
        fun provideFactory(
            repository: DocumentRepository,
            fileStorageService: FileStorageService,
            documentId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DocumentDetailViewModel(repository, fileStorageService, documentId) as T
            }
        }
    }
}

data class DocumentDetailState(
    val document: Document? = null,
    val pages: List<Page> = emptyList(),
    val currentPageIndex: Int = 0
)
