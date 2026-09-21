package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.datetime.Instant

/**
 * Domain model representing a budget cap.
 */
data class BudgetCap(
    val id: Long = 0,
    val categoryId: Long,
    val limitAmount: BigDecimal,
    val currency: String,
    val periodType: BudgetPeriod,
    val warningThreshold: Float = 0.80f,
    val isActive: Boolean = true,
    val createdAt: Instant
)
