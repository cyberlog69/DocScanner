package com.example.docscanner.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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
        const val DATABASE_VERSION = 2   // v1→v2: category column changed INTEGER→TEXT

        const val TABLE_DOCUMENTS = "documents"
        const val TABLE_PAGES = "pages"
        const val TABLE_DOCUMENTS_FTS = "documents_fts"
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
                extractedText TEXT NOT NULL
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
                extractedText
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var version = oldVersion

        // v1 → v2: Convert category column from INTEGER ordinal to TEXT name.
        // Existing user data is preserved by reading each row's ordinal and
        // writing back the enum name in the new column.
        if (version < 2) {
            // 1. Create replacement table with TEXT category column
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
                    extractedText TEXT NOT NULL
                )
                """.trimIndent()
            )

            // 2. Copy rows, converting INTEGER ordinal → TEXT name on the fly
            val cursor = db.rawQuery("SELECT * FROM $TABLE_DOCUMENTS", null)
            cursor.use { c ->
                while (c.moveToNext()) {
                    val ordinal = c.getInt(c.getColumnIndexOrThrow("category"))
                    val categoryName = DocumentCategory.fromOrdinal(ordinal).name
                    db.execSQL(
                        """
                        INSERT INTO documents_v2 (id, title, category, createdAt, modifiedAt,
                            pageCount, thumbnailPath, pdfPath, extractedText)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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

            // 3. Swap tables and rebuild FTS index
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DOCUMENTS_FTS")
            db.execSQL("DROP TABLE $TABLE_DOCUMENTS")
            db.execSQL("ALTER TABLE documents_v2 RENAME TO $TABLE_DOCUMENTS")
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_DOCUMENTS_FTS USING fts4(
                    content="$TABLE_DOCUMENTS",
                    title,
                    extractedText
                )
                """.trimIndent()
            )
            // Rebuild FTS content from migrated data
            db.execSQL("INSERT INTO $TABLE_DOCUMENTS_FTS(docid, title, extractedText) SELECT rowid, title, extractedText FROM $TABLE_DOCUMENTS")

            version = 2
        }

        // Future migrations: add more `if (version < N)` blocks here
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
        }
        writableDatabase.insertWithOnConflict(TABLE_DOCUMENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        updateFts(document.id, document.title, document.extractedText)
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
        }
        writableDatabase.update(TABLE_DOCUMENTS, values, "id = ?", arrayOf(document.id))
        updateFts(document.id, document.title, document.extractedText)
        notifyChanged()
    }

    override suspend fun deleteDocument(document: Document): Unit = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_DOCUMENTS, "id = ?", arrayOf(document.id))
        writableDatabase.delete(TABLE_DOCUMENTS_FTS, "docid IN (SELECT rowid FROM $TABLE_DOCUMENTS WHERE id = ?)", arrayOf(document.id))
        notifyChanged()
    }

    override fun getAllDocuments(): Flow<List<Document>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                queryDocuments("SELECT * FROM $TABLE_DOCUMENTS ORDER BY createdAt DESC", null)
            }
        }
    }

    override fun getDocumentsByCategory(category: DocumentCategory): Flow<List<Document>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                queryDocuments(
                    "SELECT * FROM $TABLE_DOCUMENTS WHERE category = ? ORDER BY createdAt DESC",
                    arrayOf(category.name)   // Match by enum name, not ordinal
                )
            }
        }
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
            updateFts(id, doc.title, text)
        }
        notifyChanged()
    }

    override fun searchDocuments(query: String): Flow<List<Document>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                val cleanQuery = query.trim()
                if (cleanQuery.isEmpty()) {
                    queryDocuments("SELECT * FROM $TABLE_DOCUMENTS ORDER BY createdAt DESC", null)
                } else {
                    // Build an FTS4 MATCH expression with prefix wildcard on each token.
                    // e.g. "hello world" → "hello* world*" so partial words still match.
                    val ftsQuery = cleanQuery
                        .split(Regex("\\s+"))
                        .filter { it.isNotEmpty() }
                        .joinToString(" ") { "${it}*" }
                    queryDocuments(
                        """
                        SELECT d.* FROM $TABLE_DOCUMENTS d
                        INNER JOIN $TABLE_DOCUMENTS_FTS fts ON fts.docid = d.rowid
                        WHERE $TABLE_DOCUMENTS_FTS MATCH ?
                        ORDER BY d.createdAt DESC
                        """.trimIndent(),
                        arrayOf(ftsQuery)
                    )
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

    private fun updateFts(docId: String, title: String, text: String) {
        try {
            val cursor = readableDatabase.rawQuery("SELECT rowid FROM $TABLE_DOCUMENTS WHERE id = ?", arrayOf(docId))
            if (cursor.moveToFirst()) {
                val rowId = cursor.getLong(0)
                val values = ContentValues().apply {
                    put("docid", rowId)
                    put("title", title)
                    put("extractedText", text)
                }
                writableDatabase.insertWithOnConflict(TABLE_DOCUMENTS_FTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            cursor.close()
        } catch (_: Exception) {}
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

            while (c.moveToNext()) {
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
                        extractedText = c.getString(extractedTextIdx)
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
