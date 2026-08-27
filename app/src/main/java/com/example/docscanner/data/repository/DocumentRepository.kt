package com.example.docscanner.data.repository

import android.util.Log
import com.example.docscanner.data.db.DocumentDao
import com.example.docscanner.model.Document
import com.example.docscanner.model.DocumentCategory
import com.example.docscanner.model.Page
import com.example.docscanner.model.ScannerResult
import com.example.docscanner.repository.DocumentRepository as IDocumentRepository
import kotlinx.coroutines.flow.Flow

/**
 * Concrete Android repository implementation interacting with [DocumentDao].
 * Wraps database mutations in safe [ScannerResult] values.
 */
class DocumentRepository(
    private val documentDao: DocumentDao
) : IDocumentRepository {

    override fun getAllDocuments(): Flow<List<Document>> = documentDao.getAllDocuments()

    override fun getDocumentsByCategory(category: DocumentCategory): Flow<List<Document>> =
        documentDao.getDocumentsByCategory(category)

    override fun searchDocuments(query: String): Flow<List<Document>> =
        documentDao.searchDocuments(query)

    override suspend fun getDocumentById(id: String): Document? = try {
        documentDao.getDocumentById(id)
    } catch (e: Exception) {
        Log.e("DocumentRepository", "Error getting document by id=$id", e)
        null
    }

    override suspend fun saveDocument(document: Document): ScannerResult<Unit> = safeDbCall {
        documentDao.insertDocument(document)
    }

    override suspend fun updateDocument(document: Document): ScannerResult<Unit> = safeDbCall {
        documentDao.updateDocument(document)
    }

    override suspend fun deleteDocument(document: Document): ScannerResult<Unit> = safeDbCall {
        documentDao.deleteAllPagesForDocument(document.id)
        documentDao.deleteDocument(document)
    }

    override suspend fun renameDocument(id: String, title: String): ScannerResult<Unit> = safeDbCall {
        documentDao.renameDocument(id, title)
    }

    override suspend fun updateCategory(id: String, category: DocumentCategory): ScannerResult<Unit> = safeDbCall {
        documentDao.updateCategory(id, category)
    }

    override suspend fun togglePin(id: String, isPinned: Boolean): ScannerResult<Unit> = safeDbCall {
        documentDao.togglePin(id, isPinned)
    }

    override suspend fun updateTags(id: String, tags: List<String>): ScannerResult<Unit> = safeDbCall {
        documentDao.updateTags(id, tags)
    }

    override suspend fun updateDocumentMeta(
        id: String,
        pageCount: Int,
        thumbnailPath: String,
        pdfPath: String,
        extractedText: String
    ): ScannerResult<Unit> = safeDbCall {
        documentDao.updateDocumentMeta(id, pageCount, thumbnailPath, pdfPath, extractedText)
    }

    // ── Pages ─────────────────────────────────────────────────────────────

    override suspend fun savePage(page: Page): ScannerResult<Unit> = safeDbCall {
        documentDao.insertPage(page)
    }

    override suspend fun savePages(pages: List<Page>): ScannerResult<Unit> = safeDbCall {
        documentDao.insertPages(pages)
    }

    override fun getPagesForDocument(documentId: String): Flow<List<Page>> =
        documentDao.getPagesForDocument(documentId)

    override suspend fun getPagesForDocumentSync(documentId: String): List<Page> = try {
        documentDao.getPagesForDocumentSync(documentId)
    } catch (e: Exception) {
        Log.e("DocumentRepository", "Error getting pages for docId=$documentId", e)
        emptyList()
    }

    override suspend fun deletePage(page: Page): ScannerResult<Unit> = safeDbCall {
        documentDao.deletePage(page)
    }

    private inline fun safeDbCall(action: () -> Unit): ScannerResult<Unit> = try {
        action()
        ScannerResult.Success(Unit)
    } catch (e: Exception) {
        Log.e("DocumentRepository", "Database operation failed", e)
        ScannerResult.Failure.DatabaseError(
            message = e.localizedMessage ?: "Database operation failed",
            cause = e
        )
    }
}

