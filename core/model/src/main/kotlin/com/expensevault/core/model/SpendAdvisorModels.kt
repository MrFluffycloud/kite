package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class SavingsTip(
    val title: String,
    val description: String,
    val potentialWeeklySavings: String,
    val category: String? = null
)

data class SpendSummary(
    val weeklyAverageSpend: BigDecimal,
    val currentWeekSpend: BigDecimal,
    val topCategories: List<Pair<String, BigDecimal>>,
    val currencySymbol: String = "£"
)
