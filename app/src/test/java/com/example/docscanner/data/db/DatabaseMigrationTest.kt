package com.example.docscanner.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationTest {

    @Test
    fun testAllMigrationsAreOrderedAndContiguous() {
        val migrations = ALL_MIGRATIONS
        assertTrue("Migrations list should not be empty", migrations.isNotEmpty())

        assertEquals(1, migrations.first().fromVersion)
        assertEquals(AppDatabase.DATABASE_VERSION, migrations.last().toVersion)

        for (i in 0 until migrations.size - 1) {
            val current = migrations[i]
            val next = migrations[i + 1]
            assertEquals(
                "Migration chain must be contiguous from v${current.fromVersion}",
                current.toVersion,
                next.fromVersion
            )
        }
    }

    @Test
    fun testEachVersionStepHasDedicatedMigration() {
        for (v in 1 until AppDatabase.DATABASE_VERSION) {
            val migration = ALL_MIGRATIONS.find { it.fromVersion == v && it.toVersion == v + 1 }
            assertNotNull("Expected migration from v$v to v${v + 1}", migration)
        }
    }
}
