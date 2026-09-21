package com.expensevault.core.model

import kotlinx.datetime.Instant

/**
 * Domain model representing an attached receipt photo.
 */
data class ReceiptPhoto(
    val id: Long = 0,
    val transactionId: Long,
    val filePath: String,
    val thumbnailPath: String? = null,
    val capturedAt: Instant
)
