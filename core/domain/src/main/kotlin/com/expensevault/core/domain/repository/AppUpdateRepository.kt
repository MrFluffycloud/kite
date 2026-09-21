package com.expensevault.core.domain.repository

import com.expensevault.core.model.AppUpdateInfo

/**
 * Repository checking for new releases and updates.
 */
interface AppUpdateRepository {
    suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo>
}
