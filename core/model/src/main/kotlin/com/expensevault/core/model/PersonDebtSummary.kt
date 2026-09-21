package com.expensevault.core.model

import java.math.BigDecimal

/**
 * Computed view model for debt dashboard.
 */
data class PersonDebtSummary(
    val person: Person,
    val netBalance: BigDecimal,  // positive = they owe me, negative = I owe them
    val currency: String,
    val openDebtsCount: Int
)
