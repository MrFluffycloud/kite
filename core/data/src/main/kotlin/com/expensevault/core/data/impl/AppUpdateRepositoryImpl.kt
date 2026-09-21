package com.expensevault.core.data.impl

import com.expensevault.core.domain.repository.AppUpdateRepository
import com.expensevault.core.model.AppUpdateInfo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubAssetDto(
    val name: String = "",
    val browser_download_url: String = "",
    val size: Long = 0
)

@Serializable
data class GitHubReleaseDto(
    val tag_name: String = "",
    val name: String = "",
    val body: String? = null,
    val html_url: String = "",
    val assets: List<GitHubAssetDto> = emptyList()
)

class AppUpdateRepositoryImpl(
    private val httpClient: HttpClient
) : AppUpdateRepository {

    companion object {
        private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/MrFluffycloud/kite/releases/latest"
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(GITHUB_LATEST_RELEASE_URL) {
                header("Accept", "application/vnd.github.v3+json")
                header("User-Agent", "Kite-App")
            }

            if (response.status.isSuccess()) {
                val releaseDto: GitHubReleaseDto = response.body()
                val latestTag = releaseDto.tag_name
                val latestVersionClean = latestTag.removePrefix("v").trim()
                val currentVersionClean = currentVersion.removePrefix("v").trim()

                val isNewer = isNewerVersion(latestVersionClean, currentVersionClean)
                val apkAsset = releaseDto.assets.find { it.name.endsWith(".apk", ignoreCase = true) }

                val updateInfo = AppUpdateInfo(
                    isUpdateAvailable = isNewer,
                    latestVersion = latestTag,
                    currentVersion = currentVersion,
                    releaseNotes = releaseDto.body,
                    apkDownloadUrl = apkAsset?.browser_download_url,
                    releasePageUrl = releaseDto.html_url.ifBlank { "https://github.com/MrFluffycloud/kite/releases" }
                )
                Result.success(updateInfo)
            } else if (response.status == HttpStatusCode.NotFound) {
                // No releases yet on repo
                Result.success(
                    AppUpdateInfo(
                        isUpdateAvailable = false,
                        latestVersion = currentVersion,
                        currentVersion = currentVersion,
                        releaseNotes = "No new releases found.",
                        apkDownloadUrl = null,
                        releasePageUrl = "https://github.com/MrFluffycloud/kite/releases"
                    )
                )
            } else {
                Result.failure(IllegalStateException("GitHub API returned ${response.status.value}: ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
