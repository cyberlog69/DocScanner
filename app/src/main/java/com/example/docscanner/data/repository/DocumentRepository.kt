package com.example.docscanner.data.repository

import com.example.docscanner.data.db.DocumentDao
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Page
import kotlinx.coroutines.flow.Flow
class DocumentRepository(
    private val documentDao: DocumentDao
) {
    fun getAllDocuments(): Flow<List<Document>> = documentDao.getAllDocuments()

    fun getDocumentsByCategory(category: DocumentCategory): Flow<List<Document>> =
        documentDao.getDocumentsByCategory(category)

    fun searchDocuments(query: String): Flow<List<Document>> {
        // Wrap query for FTS MATCH syntax
        val ftsQuery = query.trim().split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" OR ") { "$it*" }
        return documentDao.searchDocuments(ftsQuery)
    }

    suspend fun getDocumentById(id: String): Document? = documentDao.getDocumentById(id)

    suspend fun saveDocument(document: Document) = documentDao.insertDocument(document)

    suspend fun updateDocument(document: Document) = documentDao.updateDocument(document)

    suspend fun deleteDocument(document: Document) {
        documentDao.deleteAllPagesForDocument(document.id)
        documentDao.deleteDocument(document)
    }

    suspend fun renameDocument(id: String, title: String) = documentDao.renameDocument(id, title)

    suspend fun updateCategory(id: String, category: DocumentCategory) =
        documentDao.updateCategory(id, category)

    suspend fun togglePin(id: String, isPinned: Boolean) =
        documentDao.togglePin(id, isPinned)

    suspend fun updateDocumentMeta(
        id: String,
        pageCount: Int,
        thumbnailPath: String,
        pdfPath: String,
        extractedText: String
    ) = documentDao.updateDocumentMeta(id, pageCount, thumbnailPath, pdfPath, extractedText)

    // ── Pages ─────────────────────────────────────────────────────────────

    suspend fun savePage(page: Page) = documentDao.insertPage(page)

    suspend fun savePages(pages: List<Page>) = documentDao.insertPages(pages)

    fun getPagesForDocument(documentId: String): Flow<List<Page>> =
        documentDao.getPagesForDocument(documentId)

    suspend fun getPagesForDocumentSync(documentId: String): List<Page> =
        documentDao.getPagesForDocumentSync(documentId)

    suspend fun deletePage(page: Page) = documentDao.deletePage(page)
}
