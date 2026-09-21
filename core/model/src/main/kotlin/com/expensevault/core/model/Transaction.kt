package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Domain model representing a transaction.
 */
data class Transaction(
    val id: Long = 0,
    val accountId: Long,
    val categoryId: Long? = null,
    val type: TransactionType,
    val originalAmount: BigDecimal,
    val originalCurrency: String,
    val exchangeRate: BigDecimal? = null,
    val baseAmount: BigDecimal,
    val note: String? = null,
    val merchant: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationName: String? = null,
    val transactionDate: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
    val recurringRuleId: Long? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
    val isEstimatedRate: Boolean = false,
    val receiptPhotoPath: String? = null
)
