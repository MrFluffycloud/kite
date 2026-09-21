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

    override suspend fun downloadApk(
        downloadUrl: String,
        destinationFile: java.io.File,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<java.io.File> = withContext(Dispatchers.IO) {
        try {
            destinationFile.parentFile?.mkdirs()
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentUrl = downloadUrl
            var connection: java.net.HttpURLConnection? = null
            var redirects = 0

            while (true) {
                val url = java.net.URL(currentUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "Kite-App")
                conn.connect()
                val responseCode = conn.responseCode
                if (responseCode in 300..399) {
                    val location = conn.getHeaderField("Location") ?: break
                    currentUrl = location
                    conn.disconnect()
                    redirects++
                    if (redirects > 5) break
                } else {
                    connection = conn
                    break
                }
            }

            val finalConn = connection ?: return@withContext Result.failure(IllegalStateException("Failed to connect to download URL"))
            if (finalConn.responseCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP ${finalConn.responseCode}: Failed to download APK"))
            }

            val contentLength = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                finalConn.contentLengthLong
            } else {
                finalConn.contentLength.toLong()
            }
            var totalRead = 0L

            finalConn.inputStream.use { input ->
                destinationFile.outputStream().use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val prog = if (contentLength > 0) totalRead.toFloat() / contentLength.toFloat() else 0f
                        onProgress(prog, totalRead, contentLength)
                    }
                }
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
