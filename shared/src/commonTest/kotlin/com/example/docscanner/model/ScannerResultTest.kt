package com.example.docscanner.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScannerResultTest {

    @Test
    fun testSuccessResult() {
        val result: ScannerResult<String> = ScannerResult.Success("Scanned Document")
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals("Scanned Document", result.getOrNull())
        assertEquals("Scanned Document", result.getOrDefault("Default"))

        var called = false
        result.onSuccess { data ->
            called = true
            assertEquals("Scanned Document", data)
        }
        assertTrue(called)

        val mapped = result.map { it.length }
        assertEquals(16, mapped.getOrNull())
    }

    @Test
    fun testFailureResult() {
        val failure: ScannerResult<String> = ScannerResult.Failure.DatabaseError("Disk full")
        assertTrue(failure.isFailure)
        assertFalse(failure.isSuccess)
        assertNull(failure.getOrNull())
        assertEquals("Default", failure.getOrDefault("Default"))

        var failureMessage = ""
        failure.onFailure { err ->
            failureMessage = err.message
        }
        assertEquals("Disk full", failureMessage)
    }
}
