package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Domain model representing a sensitive or partner-shared expense stored strictly
 * in the cryptographically isolated Vault database.
 */
data class VaultExpense(
    val id: Long = 0,
    val amount: BigDecimal,
    val currency: String = "INR",
    val title: String,
    val note: String? = null,
    val category: String = "Personal",
    val date: LocalDate,
    val isSharedWithPartner: Boolean = false,
    val partnerShare: BigDecimal? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
