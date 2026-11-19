package com.example.docscanner.data.db

import android.database.sqlite.SQLiteDatabase
import com.example.docscanner.data.model.DocumentCategory

/**
 * Interface defining a single incremental database migration step.
 */
interface DatabaseMigration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(db: SQLiteDatabase)
}

/**
 * Migration from v1 to v2:
 * Converts `category` column from integer ordinal to string name for readability and stability.
 */
object Migration1To2 : DatabaseMigration {
    override val fromVersion = 1
    override val toVersion = 2

    override fun migrate(db: SQLiteDatabase) {
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

        val cursor = db.rawQuery("SELECT * FROM ${AppDatabase.TABLE_DOCUMENTS}", null)
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

        db.execSQL("DROP TABLE IF EXISTS ${AppDatabase.TABLE_DOCUMENTS_FTS}")
        db.execSQL("DROP TABLE ${AppDatabase.TABLE_DOCUMENTS}")
        db.execSQL("ALTER TABLE documents_v2 RENAME TO ${AppDatabase.TABLE_DOCUMENTS}")
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS ${AppDatabase.TABLE_DOCUMENTS_FTS} USING fts4(
                content="${AppDatabase.TABLE_DOCUMENTS}",
                title,
                extractedText,
                tags
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO ${AppDatabase.TABLE_DOCUMENTS_FTS}(docid, title, extractedText, tags) SELECT rowid, title, extractedText, '' FROM ${AppDatabase.TABLE_DOCUMENTS}")
    }
}

/**
 * Migration from v2 to v3:
 * Adds `isPinned` column to documents table for quick pinning of important files.
 */
object Migration2To3 : DatabaseMigration {
    override val fromVersion = 2
    override val toVersion = 3

    override fun migrate(db: SQLiteDatabase) {
        if (!db.hasColumn(AppDatabase.TABLE_DOCUMENTS, "isPinned")) {
            db.execSQL("ALTER TABLE ${AppDatabase.TABLE_DOCUMENTS} ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
        }
    }
}

/**
 * Migration from v3 to v4:
 * Adds `tags` column and recreates FTS4 index to enable tag search.
 */
object Migration3To4 : DatabaseMigration {
    override val fromVersion = 3
    override val toVersion = 4

    override fun migrate(db: SQLiteDatabase) {
        if (!db.hasColumn(AppDatabase.TABLE_DOCUMENTS, "tags")) {
            db.execSQL("ALTER TABLE ${AppDatabase.TABLE_DOCUMENTS} ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        }
        db.execSQL("DROP TABLE IF EXISTS ${AppDatabase.TABLE_DOCUMENTS_FTS}")
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS ${AppDatabase.TABLE_DOCUMENTS_FTS} USING fts4(
                content="${AppDatabase.TABLE_DOCUMENTS}",
                title,
                extractedText,
                tags
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO ${AppDatabase.TABLE_DOCUMENTS_FTS}(docid, title, extractedText, tags) SELECT rowid, title, extractedText, tags FROM ${AppDatabase.TABLE_DOCUMENTS}")
    }
}

/**
 * Migration from v4 to v5:
 * Adds `isVault` and `folderId` columns to documents table and creates `folders` table.
 */
object Migration4To5 : DatabaseMigration {
    override val fromVersion = 4
    override val toVersion = 5

    override fun migrate(db: SQLiteDatabase) {
        if (!db.hasColumn(AppDatabase.TABLE_DOCUMENTS, "isVault")) {
            db.execSQL("ALTER TABLE ${AppDatabase.TABLE_DOCUMENTS} ADD COLUMN isVault INTEGER NOT NULL DEFAULT 0")
        }
        if (!db.hasColumn(AppDatabase.TABLE_DOCUMENTS, "folderId")) {
            db.execSQL("ALTER TABLE ${AppDatabase.TABLE_DOCUMENTS} ADD COLUMN folderId TEXT DEFAULT NULL")
        }
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${AppDatabase.TABLE_FOLDERS} (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                color INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

private fun SQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    rawQuery("PRAGMA table_info($table)", null).use { cursor ->
        val nameIdx = cursor.getColumnIndex("name")
        if (nameIdx < 0) return false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIdx) == column) return true
        }
    }
    return false
}

/**
 * Ordered list of all migrations supported by DocScanner.
 */
val ALL_MIGRATIONS: List<DatabaseMigration> = listOf(
    Migration1To2,
    Migration2To3,
    Migration3To4,
    Migration4To5
)
