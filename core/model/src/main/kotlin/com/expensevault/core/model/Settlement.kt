package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.datetime.Instant

/**
 * Domain model representing a settlement.
 */
data class Settlement(
    val id: Long = 0,
    val personId: Long,
    val settledAmount: BigDecimal,
    val currency: String,
    val note: String? = null,
    val settledAt: Instant,
    val debtRecordIds: List<Long>
)
