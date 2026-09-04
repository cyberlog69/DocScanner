package com.example.docscanner.repository

import com.example.docscanner.model.Document
import com.example.docscanner.model.DocumentCategory
import com.example.docscanner.model.Page
import com.example.docscanner.model.ScannerResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing scanned documents and pages.
 * Decouples domain logic and UI from concrete storage implementations.
 */
interface DocumentRepository {
    fun getAllDocuments(): Flow<List<Document>>
    fun getDocumentsByCategory(category: DocumentCategory): Flow<List<Document>>
    fun searchDocuments(query: String): Flow<List<Document>>
    suspend fun getDocumentById(id: String): Document?

    suspend fun saveDocument(document: Document): ScannerResult<Unit>
    suspend fun updateDocument(document: Document): ScannerResult<Unit>
    suspend fun deleteDocument(document: Document): ScannerResult<Unit>
    suspend fun renameDocument(id: String, title: String): ScannerResult<Unit>
    suspend fun updateCategory(id: String, category: DocumentCategory): ScannerResult<Unit>
    suspend fun togglePin(id: String, isPinned: Boolean): ScannerResult<Unit>
    suspend fun updateTags(id: String, tags: List<String>): ScannerResult<Unit>
    suspend fun updateDocumentMeta(
        id: String,
        pageCount: Int,
        thumbnailPath: String,
        pdfPath: String,
        extractedText: String
    ): ScannerResult<Unit>
    fun getVaultDocuments(): Flow<List<Document>>
    fun getDocumentsByFolder(folderId: String): Flow<List<Document>>
    suspend fun setVaultStatus(id: String, isVault: Boolean): ScannerResult<Unit>
    suspend fun setDocumentFolder(id: String, folderId: String?): ScannerResult<Unit>
    fun getAllFolders(): Flow<List<com.example.docscanner.model.Folder>>
    suspend fun insertFolder(folder: com.example.docscanner.model.Folder): ScannerResult<Unit>
    suspend fun deleteFolder(folderId: String): ScannerResult<Unit>

    // ── Pages ─────────────────────────────────────────────────────────────
    suspend fun savePage(page: Page): ScannerResult<Unit>
    suspend fun savePages(pages: List<Page>): ScannerResult<Unit>
    fun getPagesForDocument(documentId: String): Flow<List<Page>>
    suspend fun getPagesForDocumentSync(documentId: String): List<Page>
    suspend fun deletePage(page: Page): ScannerResult<Unit>
}
