package com.example.docscanner.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionComparatorTest {

    @Test
    fun testMinorUpgrade() {
        assertTrue(VersionComparator.isNewerVersion("1.6.0", "1.7.0"))
        assertFalse(VersionComparator.isNewerVersion("1.7.0", "1.6.0"))
    }

    @Test
    fun testMajorUpgrade() {
        assertTrue(VersionComparator.isNewerVersion("1.9.9", "2.0.0"))
        assertFalse(VersionComparator.isNewerVersion("2.0.0", "1.9.9"))
    }

    @Test
    fun testPatchUpgrade() {
        assertTrue(VersionComparator.isNewerVersion("1.6.0", "1.6.1"))
        assertFalse(VersionComparator.isNewerVersion("1.6.1", "1.6.1"))
    }

    @Test
    fun testBetaToStableUpgrade() {
        assertTrue(VersionComparator.isNewerVersion("1.6.0-beta", "1.6.0"))
        assertTrue(VersionComparator.isNewerVersion("1.6.0-beta", "v1.6.0"))
        assertFalse(VersionComparator.isNewerVersion("1.6.0", "1.6.0-beta"))
    }

    @Test
    fun testVPrefixHandling() {
        assertTrue(VersionComparator.isNewerVersion("v1.6.0", "v1.7.0"))
        assertFalse(VersionComparator.isNewerVersion("v1.7.0", "1.7.0"))
    }
}
