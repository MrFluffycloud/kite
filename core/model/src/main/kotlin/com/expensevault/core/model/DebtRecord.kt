package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.datetime.Instant

/**
 * Domain model representing a debt record.
 */
data class DebtRecord(
    val id: Long = 0,
    val personId: Long,
    val transactionId: Long? = null,
    val amount: BigDecimal,
    val currency: String,
    val direction: DebtDirection,
    val status: DebtStatus,
    val splitMethod: SplitMethod? = null,
    val note: String? = null,
    val createdAt: Instant,
    val settledAt: Instant? = null
)
