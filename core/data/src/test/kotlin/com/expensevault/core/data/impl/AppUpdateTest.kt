package com.expensevault.core.data.impl

import io.ktor.client.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateTest {

    private val repo = AppUpdateRepositoryImpl(HttpClient())

    @Test
    fun testVersionComparison() {
        assertTrue(repo.isNewerVersion("0.2.0", "0.1.0"))
        assertTrue(repo.isNewerVersion("1.0.0", "0.9.9"))
        assertTrue(repo.isNewerVersion("0.1.1", "0.1.0"))
        assertFalse(repo.isNewerVersion("0.1.0", "0.1.0"))
        assertFalse(repo.isNewerVersion("0.1.0", "0.2.0"))
        assertFalse(repo.isNewerVersion("0.0.9", "0.1.0"))
    }
}
