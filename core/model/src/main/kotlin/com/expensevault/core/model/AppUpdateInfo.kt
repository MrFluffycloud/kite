package com.expensevault.core.model

import kotlinx.serialization.Serializable

/**
 * Information regarding available application updates from GitHub Releases.
 */
@Serializable
data class AppUpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String = "",
    val currentVersion: String = "",
    val releaseNotes: String? = null,
    val apkDownloadUrl: String? = null,
    val releasePageUrl: String = "https://github.com/MrFluffycloud/kite/releases"
)
