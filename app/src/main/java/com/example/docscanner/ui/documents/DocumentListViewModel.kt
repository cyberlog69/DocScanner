package com.example.docscanner.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.service.FileStorageService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DocumentListViewModel(
    private val repository: DocumentRepository,
    private val fileStorageService: FileStorageService
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(DocumentCategory.ALL)
    val selectedCategory: StateFlow<DocumentCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val documents: StateFlow<List<Document>> = combine(
        _selectedCategory,
        _searchQuery.debounce(300)
    ) { category, query -> Pair(category, query) }
        .flatMapLatest { (category, query) ->
            when {
                query.isNotBlank() -> repository.searchDocuments(query)
                category == DocumentCategory.ALL -> repository.getAllDocuments()
                else -> repository.getDocumentsByCategory(category)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategory(category: DocumentCategory) = _selectedCategory.update { category }

    fun setSearchQuery(query: String) = _searchQuery.update { query }

    fun toggleViewMode() = _isGridView.update { !it }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            fileStorageService.deleteDocumentFiles(document.id)
            repository.deleteDocument(document)
        }
    }

    fun renameDocument(id: String, newTitle: String) {
        viewModelScope.launch { repository.renameDocument(id, newTitle) }
    }
}
