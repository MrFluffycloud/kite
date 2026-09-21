package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Domain model representing a recurring rule.
 */
data class RecurringRule(
    val id: Long = 0,
    val accountId: Long,
    val categoryId: Long? = null,
    val amount: BigDecimal,
    val currency: String,
    val note: String? = null,
    val frequency: RecurringFrequency,
    val customIntervalDays: Int? = null,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val nextOccurrence: LocalDate,
    val requireConfirmation: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Instant
)
