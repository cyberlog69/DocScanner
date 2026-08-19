package com.example.docscanner.data.db

import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Page
import kotlinx.coroutines.flow.Flow

interface DocumentDao {
    suspend fun insertDocument(document: Document)
    suspend fun updateDocument(document: Document)
    suspend fun deleteDocument(document: Document)
    fun getAllDocuments(): Flow<List<Document>>
    fun getDocumentsByCategory(category: DocumentCategory): Flow<List<Document>>
    suspend fun getDocumentById(id: String): Document?
    suspend fun renameDocument(id: String, title: String, now: Long = System.currentTimeMillis())
    suspend fun updateCategory(id: String, category: DocumentCategory, now: Long = System.currentTimeMillis())
    suspend fun togglePin(id: String, isPinned: Boolean, now: Long = System.currentTimeMillis())
    suspend fun updateTags(id: String, tags: List<String>, now: Long = System.currentTimeMillis())
    suspend fun updateDocumentMeta(
        id: String,
        count: Int,
        thumbnail: String,
        pdf: String,
        text: String,
        now: Long = System.currentTimeMillis()
    )
    fun searchDocuments(query: String): Flow<List<Document>>
    suspend fun insertPage(page: Page)
    suspend fun insertPages(pages: List<Page>)
    fun getPagesForDocument(documentId: String): Flow<List<Page>>
    suspend fun getPagesForDocumentSync(documentId: String): List<Page>
    suspend fun deletePage(page: Page)
    suspend fun deleteAllPagesForDocument(documentId: String)
}
