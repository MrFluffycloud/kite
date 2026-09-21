package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Domain model representing an exchange rate.
 */
data class ExchangeRate(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: BigDecimal,
    val rateDate: LocalDate,
    val fetchedAt: Instant
)
