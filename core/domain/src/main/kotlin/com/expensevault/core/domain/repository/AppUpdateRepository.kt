package com.expensevault.core.domain.repository

import com.expensevault.core.model.AppUpdateInfo
import java.io.File

/**
 * Repository checking for new releases and updates.
 */
interface AppUpdateRepository {
    suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo>
    suspend fun downloadApk(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File>
}
