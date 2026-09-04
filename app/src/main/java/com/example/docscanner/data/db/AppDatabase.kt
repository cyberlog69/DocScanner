package com.example.docscanner.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), DocumentDao {

    companion object {
        const val DATABASE_NAME = "docscanner.db"
        const val DATABASE_VERSION = 5   // v4→v5: added isVault, folderId columns and folders table

        const val TABLE_DOCUMENTS = "documents"
        const val TABLE_PAGES = "pages"
        const val TABLE_DOCUMENTS_FTS = "documents_fts"
        const val TABLE_FOLDERS = "folders"
    }

    private val changeNotifier = MutableStateFlow(System.currentTimeMillis())

    private fun notifyChanged() {
        changeNotifier.value = System.currentTimeMillis()
    }

    val documentDao: DocumentDao get() = this

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DOCUMENTS (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                modifiedAt INTEGER NOT NULL,
                pageCount INTEGER NOT NULL,
                thumbnailPath TEXT NOT NULL,
                pdfPath TEXT NOT NULL,
                extractedText TEXT NOT NULL,
                isPinned INTEGER NOT NULL DEFAULT 0,
                tags TEXT NOT NULL DEFAULT '',
                isVault INTEGER NOT NULL DEFAULT 0,
                folderId TEXT DEFAULT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FOLDERS (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                color INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PAGES (
                id TEXT PRIMARY KEY NOT NULL,
                documentId TEXT NOT NULL,
                pageIndex INTEGER NOT NULL,
                imagePath TEXT NOT NULL,
                originalImagePath TEXT NOT NULL,
                extractedText TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(documentId) REFERENCES $TABLE_DOCUMENTS(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_pages_documentId ON $TABLE_PAGES(documentId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_DOCUMENTS_FTS USING fts4(
                content="$TABLE_DOCUMENTS",
                title,
                extractedText,
                tags
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var version = oldVersion

        // v1 → v2: Convert category column from INTEGER ordinal to TEXT name.
        if (version < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS documents_v2 (
                    id TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    modifiedAt INTEGER NOT NULL,
                    pageCount INTEGER NOT NULL,
                    thumbnailPath TEXT NOT NULL,
                    pdfPath TEXT NOT NULL,
                    extractedText TEXT NOT NULL,
                    isPinned INTEGER NOT NULL DEFAULT 0,
                    tags TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )

            val cursor = db.rawQuery("SELECT * FROM $TABLE_DOCUMENTS", null)
            cursor.use { c ->
                while (c.moveToNext()) {
                    val ordinal = c.getInt(c.getColumnIndexOrThrow("category"))
                    val categoryName = DocumentCategory.fromOrdinal(ordinal).name
                    db.execSQL(
                        """
                        INSERT INTO documents_v2 (id, title, category, createdAt, modifiedAt,
                            pageCount, thumbnailPath, pdfPath, extractedText, isPinned, tags)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, '')
                        """.trimIndent(),
                        arrayOf(
                            c.getString(c.getColumnIndexOrThrow("id")),
                            c.getString(c.getColumnIndexOrThrow("title")),
                            categoryName,
                            c.getLong(c.getColumnIndexOrThrow("createdAt")).toString(),
                            c.getLong(c.getColumnIndexOrThrow("modifiedAt")).toString(),
                            c.getInt(c.getColumnIndexOrThrow("pageCount")).toString(),
                            c.getString(c.getColumnIndexOrThrow("thumbnailPath")),
                            c.getString(c.getColumnIndexOrThrow("pdfPath")),
                            c.getString(c.getColumnIndexOrThrow("extractedText"))
                        )
                    )
                }
            }

            db.execSQL("DROP TABLE IF EXISTS $TABLE_DOCUMENTS_FTS")
            db.execSQL("DROP TABLE $TABLE_DOCUMENTS")
            db.execSQL("ALTER TABLE documents_v2 RENAME TO $TABLE_DOCUMENTS")
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_DOCUMENTS_FTS USING fts4(
                    content="$TABLE_DOCUMENTS",
                    title,
                    extractedText,
                    tags
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO $TABLE_DOCUMENTS_FTS(docid, title, extractedText, tags) SELECT rowid, title, extractedText, '' FROM $TABLE_DOCUMENTS")

            version = 2
        }

        // v2 → v3: Add isPinned column safely
        if (version < 3) {
            try {
                val cursor = db.rawQuery("PRAGMA table_info($TABLE_DOCUMENTS)", null)
                var hasIsPinned = false
                cursor.use { c ->
                    val nameIdx = c.getColumnIndex("name")
                    while (c.moveToNext()) {
                        if (nameIdx >= 0 && c.getString(nameIdx) == "isPinned") {
                            hasIsPinned = true
                            break
                        }
                    }
                }
                if (!hasIsPinned) {
                    db.execSQL("ALTER TABLE $TABLE_DOCUMENTS ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error migrating database to v3 (isPinned column)", e)
            }
            version = 3
        }

        // v3 → v4: Add tags column safely and update FTS
        if (version < 4) {
            try {
                val cursor = db.rawQuery("PRAGMA table_info($TABLE_DOCUMENTS)", null)
                var hasTags = false
                cursor.use { c ->
                    val nameIdx = c.getColumnIndex("name")
                    while (c.moveToNext()) {
                        if (nameIdx >= 0 && c.getString(nameIdx) == "tags") {
                            hasTags = true
                            break
                        }
                    }
                }
                if (!hasTags) {
                    db.execSQL("ALTER TABLE $TABLE_DOCUMENTS ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL("DROP TABLE IF EXISTS $TABLE_DOCUMENTS_FTS")
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_DOCUMENTS_FTS USING fts4(
                        content="$TABLE_DOCUMENTS",
                        title,
                        extractedText,
                        tags
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO $TABLE_DOCUMENTS_FTS(docid, title, extractedText, tags) SELECT rowid, title, extractedText, tags FROM $TABLE_DOCUMENTS")
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error migrating database to v4 (tags column and FTS update)", e)
            }
            version = 4
        }

        // v4 → v5: Add isVault, folderId columns safely and create folders table
        if (version < 5) {
            try {
                val cursor = db.rawQuery("PRAGMA table_info($TABLE_DOCUMENTS)", null)
                var hasIsVault = false
                var hasFolderId = false
                cursor.use { c ->
                    val nameIdx = c.getColumnIndex("name")
                    while (c.moveToNext()) {
                        if (nameIdx >= 0) {
                            val name = c.getString(nameIdx)
                            if (name == "isVault") hasIsVault = true
                            if (name == "folderId") hasFolderId = true
                        }
                    }
                }
                if (!hasIsVault) {
                    db.execSQL("ALTER TABLE $TABLE_DOCUMENTS ADD COLUMN isVault INTEGER NOT NULL DEFAULT 0")
                }
                if (!hasFolderId) {
                    db.execSQL("ALTER TABLE $TABLE_DOCUMENTS ADD COLUMN folderId TEXT DEFAULT NULL")
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $TABLE_FOLDERS (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error migrating database to v5 (isVault, folderId, folders table)", e)
            }
            version = 5
        }
    }

    // ── Document DAO Implementation ───────────────────────────────────────────

    override suspend fun insertDocument(document: Document): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", document.id)
            put("title", document.title)
            put("category", document.category.name)   // Store enum name, not ordinal
            put("createdAt", document.createdAt)
            put("modifiedAt", document.modifiedAt)
            put("pageCount", document.pageCount)
            put("thumbnailPath", document.thumbnailPath)
            put("pdfPath", document.pdfPath)
            put("extractedText", document.extractedText)
            put("isPinned", if (document.isPinned) 1 else 0)
            put("tags", document.tagsToDbString())
            put("isVault", if (document.isVault) 1 else 0)
            put("folderId", document.folderId)
        }
        writableDatabase.insertWithOnConflict(TABLE_DOCUMENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        updateFts(document.id, document.title, document.extractedText, document.tagsToDbString())
        notifyChanged()
    }

    override suspend fun updateDocument(document: Document): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("title", document.title)
            put("category", document.category.name)   // Store enum name, not ordinal
            put("modifiedAt", document.modifiedAt)
            put("pageCount", document.pageCount)
            put("thumbnailPath", document.thumbnailPath)
            put("pdfPath", document.pdfPath)
            put("extractedText", document.extractedText)
            put("isPinned", if (document.isPinned) 1 else 0)
            put("tags", document.tagsToDbString())
            put("isVault", if (document.isVault) 1 else 0)
            put("folderId", document.folderId)
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "id = ?", arrayOf(document.id))
        updateFts(document.id, document.title, document.extractedText, document.tagsToDbString())
        notifyChanged()
    }

    override suspend fun deleteDocument(document: Document): Unit = withContext(Dispatchers.IO) {
        // Delete FTS rows first while the documents row still exists (docid subquery needs it).
        writableDatabase.delete(TABLE_DOCUMENTS_FTS, "docid IN (SELECT rowid FROM $TABLE_DOCUMENTS WHERE id = ?)", arrayOf(document.id))
        writableDatabase.delete(TABLE_DOCUMENTS, "id = ?", arrayOf(document.id))
        notifyChanged()
    }

    override fun getAllDocuments(): Flow<List<Document>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                queryDocuments("SELECT * FROM $TABLE_DOCUMENTS WHERE isVault = 0 ORDER BY isPinned DESC, createdAt DESC", null)
            }
        }
    }

    override fun getDocumentsByCategory(category: DocumentCategory): Flow<List<Document>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                queryDocuments(
                    "SELECT * FROM $TABLE_DOCUMENTS WHERE category = ? AND isVault = 0 ORDER BY isPinned DESC, createdAt DESC",
                    arrayOf(category.name)   // Match by enum name, not ordinal
                )
            }
        }
    }

    override fun getVaultDocuments(): Flow<List<Document>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                queryDocuments("SELECT * FROM $TABLE_DOCUMENTS WHERE isVault = 1 ORDER BY isPinned DESC, createdAt DESC", null)
            }
        }
    }

    override fun getDocumentsByFolder(folderId: String): Flow<List<Document>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                queryDocuments(
                    "SELECT * FROM $TABLE_DOCUMENTS WHERE folderId = ? AND isVault = 0 ORDER BY isPinned DESC, createdAt DESC",
                    arrayOf(folderId)
                )
            }
        }
    }

    override suspend fun setVaultStatus(id: String, isVault: Boolean, now: Long): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("isVault", if (isVault) 1 else 0)
            put("modifiedAt", now)
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "id = ?", arrayOf(id))
        notifyChanged()
    }

    override suspend fun setDocumentFolder(id: String, folderId: String?, now: Long): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("folderId", folderId)
            put("modifiedAt", now)
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "id = ?", arrayOf(id))
        notifyChanged()
    }

    override suspend fun insertFolder(folder: com.example.docscanner.data.model.Folder): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", folder.id)
            put("name", folder.name)
            put("color", folder.color)
            put("createdAt", folder.createdAt)
        }
        writableDatabase.insertWithOnConflict(TABLE_FOLDERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        notifyChanged()
    }

    override suspend fun updateFolder(folder: com.example.docscanner.data.model.Folder): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("name", folder.name)
            put("color", folder.color)
        }
        writableDatabase.update(TABLE_FOLDERS, values, "id = ?", arrayOf(folder.id))
        notifyChanged()
    }

    override suspend fun deleteFolder(folderId: String): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            putNull("folderId")
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "folderId = ?", arrayOf(folderId))
        writableDatabase.delete(TABLE_FOLDERS, "id = ?", arrayOf(folderId))
        notifyChanged()
    }

    override fun getAllFolders(): Flow<List<com.example.docscanner.data.model.Folder>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                getAllFoldersSync()
            }
        }
    }

    override suspend fun getAllFoldersSync(): List<com.example.docscanner.data.model.Folder> = withContext(Dispatchers.IO) {
        val list = mutableListOf<com.example.docscanner.data.model.Folder>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_FOLDERS ORDER BY createdAt ASC", null)
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val nameIdx = c.getColumnIndexOrThrow("name")
            val colorIdx = c.getColumnIndexOrThrow("color")
            val createdAtIdx = c.getColumnIndexOrThrow("createdAt")
            while (c.moveToNext()) {
                list.add(
                    com.example.docscanner.data.model.Folder(
                        id = c.getString(idIdx),
                        name = c.getString(nameIdx),
                        color = c.getInt(colorIdx),
                        createdAt = c.getLong(createdAtIdx)
                    )
                )
            }
        }
        list
    }

    override suspend fun getDocumentById(id: String): Document? = withContext(Dispatchers.IO) {
        val list = queryDocuments("SELECT * FROM $TABLE_DOCUMENTS WHERE id = ? LIMIT 1", arrayOf(id))
        list.firstOrNull()
    }

    override suspend fun renameDocument(id: String, title: String, now: Long): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("title", title)
            put("modifiedAt", now)
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "id = ?", arrayOf(id))
        val doc = getDocumentById(id)
        if (doc != null) {
            updateFts(id, title, doc.extractedText)
        }
        notifyChanged()
    }

    override suspend fun updateCategory(id: String, category: DocumentCategory, now: Long): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("category", category.name)   // Store enum name, not ordinal
            put("modifiedAt", now)
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "id = ?", arrayOf(id))
        notifyChanged()
    }

    override suspend fun togglePin(id: String, isPinned: Boolean, now: Long): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("isPinned", if (isPinned) 1 else 0)
            put("modifiedAt", now)
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "id = ?", arrayOf(id))
        notifyChanged()
    }

    override suspend fun updateTags(id: String, tags: List<String>, now: Long): Unit = withContext(Dispatchers.IO) {
        val tagsString = tags.joinToString(",") { it.trim() }
        val values = ContentValues().apply {
            put("tags", tagsString)
            put("modifiedAt", now)
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "id = ?", arrayOf(id))
        val doc = getDocumentById(id)
        if (doc != null) {
            updateFts(id, doc.title, doc.extractedText, tagsString)
        }
        notifyChanged()
    }

    override suspend fun updateDocumentMeta(
        id: String,
        count: Int,
        thumbnail: String,
        pdf: String,
        text: String,
        now: Long
    ): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("pageCount", count)
            put("thumbnailPath", thumbnail)
            put("pdfPath", pdf)
            put("extractedText", text)
            put("modifiedAt", now)
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "id = ?", arrayOf(id))
        val doc = getDocumentById(id)
        if (doc != null) {
            updateFts(id, doc.title, text, doc.tagsToDbString())
        }
        notifyChanged()
    }

    override fun searchDocuments(query: String): Flow<List<Document>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                val cleanQuery = query.trim()
                if (cleanQuery.isEmpty()) {
                    queryDocuments("SELECT * FROM $TABLE_DOCUMENTS WHERE isVault = 0 ORDER BY isPinned DESC, createdAt DESC", null)
                } else {
                    try {
                        // Build an FTS4 MATCH expression with a prefix wildcard on each quoted token.
                        // Quoting tokens keeps special FTS characters (" - ( ) : ^ etc.) safe.
                        val ftsQuery = buildFtsMatchQuery(cleanQuery)
                        queryDocuments(
                            """
                            SELECT d.* FROM $TABLE_DOCUMENTS d
                            INNER JOIN $TABLE_DOCUMENTS_FTS fts ON fts.docid = d.rowid
                            WHERE $TABLE_DOCUMENTS_FTS MATCH ? AND d.isVault = 0
                            ORDER BY d.isPinned DESC, d.createdAt DESC
                            """.trimIndent(),
                            arrayOf(ftsQuery)
                        )
                    } catch (_: Exception) {
                        // Fallback to a safe LIKE search if FTS parsing fails for any reason.
                        queryDocuments(
                            """
                            SELECT * FROM $TABLE_DOCUMENTS
                            WHERE (title LIKE ? OR extractedText LIKE ? OR tags LIKE ?) AND isVault = 0
                            ORDER BY isPinned DESC, createdAt DESC
                            """.trimIndent(),
                            arrayOf("%$cleanQuery%", "%$cleanQuery%", "%$cleanQuery%")
                        )
                    }
                }
            }
        }
    }

    // ── Page DAO Implementation ───────────────────────────────────────────────

    override suspend fun insertPage(page: Page): Unit = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", page.id)
            put("documentId", page.documentId)
            put("pageIndex", page.pageIndex)
            put("imagePath", page.imagePath)
            put("originalImagePath", page.originalImagePath)
            put("extractedText", page.extractedText)
            put("createdAt", page.createdAt)
        }
        writableDatabase.insertWithOnConflict(TABLE_PAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        notifyChanged()
    }

    override suspend fun insertPages(pages: List<Page>): Unit = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (page in pages) {
                val values = ContentValues().apply {
                    put("id", page.id)
                    put("documentId", page.documentId)
                    put("pageIndex", page.pageIndex)
                    put("imagePath", page.imagePath)
                    put("originalImagePath", page.originalImagePath)
                    put("extractedText", page.extractedText)
                    put("createdAt", page.createdAt)
                }
                db.insertWithOnConflict(TABLE_PAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        notifyChanged()
    }

    override fun getPagesForDocument(documentId: String): Flow<List<Page>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                queryPages("SELECT * FROM $TABLE_PAGES WHERE documentId = ? ORDER BY pageIndex ASC", arrayOf(documentId))
            }
        }
    }

    override suspend fun getPagesForDocumentSync(documentId: String): List<Page> = withContext(Dispatchers.IO) {
        queryPages("SELECT * FROM $TABLE_PAGES WHERE documentId = ? ORDER BY pageIndex ASC", arrayOf(documentId))
    }

    override suspend fun deletePage(page: Page): Unit = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_PAGES, "id = ?", arrayOf(page.id))
        notifyChanged()
    }

    override suspend fun deleteAllPagesForDocument(documentId: String): Unit = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_PAGES, "documentId = ?", arrayOf(documentId))
        notifyChanged()
    }

    // ── Helper Queries ────────────────────────────────────────────────────────

    /**
     * Builds a safe FTS4 MATCH expression from a user-supplied search query.
     * Each whitespace-separated token is quoted (escaping embedded double-quotes) and
     * given a prefix wildcard, e.g. `"tax"* "invoice"*` so partial words still match.
     */
    private fun buildFtsMatchQuery(query: String): String {
        return query
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                "\"${token.replace("\"", "\"\"")}\"*"
            }
    }

    private fun updateFts(docId: String, title: String, text: String, tags: String = "") {
        try {
            readableDatabase.rawQuery("SELECT rowid FROM $TABLE_DOCUMENTS WHERE id = ?", arrayOf(docId)).use { cursor ->
                if (cursor.moveToFirst()) {
                    val rowId = cursor.getLong(0)
                    val values = ContentValues().apply {
                        put("docid", rowId)
                        put("title", title)
                        put("extractedText", text)
                        put("tags", tags)
                    }
                    writableDatabase.insertWithOnConflict(TABLE_DOCUMENTS_FTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                } else {
                    Log.w("AppDatabase", "FTS indexing skipped: No rowid found for document id=$docId")
                }
            }
        } catch (e: Exception) {
            Log.e("AppDatabase", "Failed to update FTS for docId=$docId", e)
        }
    }

    private fun queryDocuments(sql: String, args: Array<String>?): List<Document> {
        val list = mutableListOf<Document>()
        val cursor: Cursor = readableDatabase.rawQuery(sql, args)
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val titleIdx = c.getColumnIndexOrThrow("title")
            val categoryIdx = c.getColumnIndexOrThrow("category")
            val createdAtIdx = c.getColumnIndexOrThrow("createdAt")
            val modifiedAtIdx = c.getColumnIndexOrThrow("modifiedAt")
            val pageCountIdx = c.getColumnIndexOrThrow("pageCount")
            val thumbnailPathIdx = c.getColumnIndexOrThrow("thumbnailPath")
            val pdfPathIdx = c.getColumnIndexOrThrow("pdfPath")
            val extractedTextIdx = c.getColumnIndexOrThrow("extractedText")
            val isPinnedIdx = c.getColumnIndex("isPinned")
            val tagsIdx = c.getColumnIndex("tags")
            val isVaultIdx = c.getColumnIndex("isVault")
            val folderIdIdx = c.getColumnIndex("folderId")

            while (c.moveToNext()) {
                val isPinned = if (isPinnedIdx >= 0) c.getInt(isPinnedIdx) == 1 else false
                val tags = if (tagsIdx >= 0) Document.parseTagsString(c.getString(tagsIdx)) else emptyList()
                val isVault = if (isVaultIdx >= 0) c.getInt(isVaultIdx) == 1 else false
                val folderId = if (folderIdIdx >= 0) c.getString(folderIdIdx) else null
                list.add(
                    Document(
                        id = c.getString(idIdx),
                        title = c.getString(titleIdx),
                        category = DocumentCategory.fromName(c.getString(categoryIdx)),   // Read TEXT name
                        createdAt = c.getLong(createdAtIdx),
                        modifiedAt = c.getLong(modifiedAtIdx),
                        pageCount = c.getInt(pageCountIdx),
                        thumbnailPath = c.getString(thumbnailPathIdx),
                        pdfPath = c.getString(pdfPathIdx),
                        extractedText = c.getString(extractedTextIdx),
                        isPinned = isPinned,
                        tags = tags,
                        isVault = isVault,
                        folderId = folderId
                    )
                )
            }
        }
        return list
    }

    private fun queryPages(sql: String, args: Array<String>?): List<Page> {
        val list = mutableListOf<Page>()
        val cursor: Cursor = readableDatabase.rawQuery(sql, args)
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val documentIdIdx = c.getColumnIndexOrThrow("documentId")
            val pageIndexIdx = c.getColumnIndexOrThrow("pageIndex")
            val imagePathIdx = c.getColumnIndexOrThrow("imagePath")
            val origPathIdx = c.getColumnIndexOrThrow("originalImagePath")
            val extractedTextIdx = c.getColumnIndexOrThrow("extractedText")
            val createdAtIdx = c.getColumnIndexOrThrow("createdAt")

            while (c.moveToNext()) {
                list.add(
                    Page(
                        id = c.getString(idIdx),
                        documentId = c.getString(documentIdIdx),
                        pageIndex = c.getInt(pageIndexIdx),
                        imagePath = c.getString(imagePathIdx),
                        originalImagePath = c.getString(origPathIdx),
                        extractedText = c.getString(extractedTextIdx),
                        createdAt = c.getLong(createdAtIdx)
                    )
                )
            }
        }
        return list
    }
}
