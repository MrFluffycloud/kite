package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.datetime.Instant

/**
 * Domain model representing an account.
 */
data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val defaultCurrency: String,  // ISO 4217
    val initialBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val iconName: String? = null,
    val colorHex: String? = null,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
)
