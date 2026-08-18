package com.example.docscanner.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.SortOrder
import com.example.docscanner.data.model.applySort
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.service.FileStorageService
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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DocumentListViewModel(
    private val repository: DocumentRepository,
    private val fileStorageService: FileStorageService
) : ViewModel() {

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

    private val _totalStorageBytes = MutableStateFlow(0L)
    val totalStorageBytes: StateFlow<Long> = _totalStorageBytes.asStateFlow()

    init {
        refreshStorageStats()
    }

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
        .combine(_sortOrder) { docs, sort ->
            refreshStorageStats()
            docs.applySort(sort)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategory(category: DocumentCategory) = _selectedCategory.update { category }

    fun setSearchQuery(query: String) = _searchQuery.update { query }

    fun setSortOrder(order: SortOrder) = _sortOrder.update { order }

    fun toggleViewMode() = _isGridView.update { !it }

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
            refreshStorageStats()
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
            refreshStorageStats()
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

    private fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val total = fileStorageService.getTotalStorageUsed()
            _totalStorageBytes.value = total
        }
    }
}
