package com.expensevault.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.Serializable

@Serializable
data class FixedCommitment(
    val id: String,
    val name: String,
    val amount: String,
    val isFulfilled: Boolean = false
) {
    val amountBigDecimal: BigDecimal
        get() = try { BigDecimal(amount) } catch (_: Exception) { BigDecimal.ZERO }
}

fun resolveCurrencySymbol(currencyCode: String?): String {
    if (currencyCode.isNullOrBlank()) return "₹"
    return when (currencyCode.uppercase().trim()) {
        "INR", "₹" -> "₹"
        "USD", "$" -> "$"
        "EUR", "€" -> "€"
        "GBP", "£" -> "£"
        "JPY", "¥" -> "¥"
        "CAD" -> "CA$"
        "AUD" -> "AU$"
        else -> try {
            java.util.Currency.getInstance(currencyCode.uppercase()).symbol
        } catch (_: Exception) {
            currencyCode
        }
    }
}

@Serializable
data class WeeklyAllowanceConfig(
    val totalAllowance: String = "0",
    val currencySymbol: String = "",
    val fixedCommitments: List<FixedCommitment> = emptyList()
) {
    val totalAllowanceBigDecimal: BigDecimal
        get() = try { BigDecimal(totalAllowance) } catch (_: Exception) { BigDecimal.ZERO }

    val totalFixedCommitmentsBigDecimal: BigDecimal
        get() = fixedCommitments.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amountBigDecimal) }

    val reservedUnfulfilledFixedBigDecimal: BigDecimal
        get() = fixedCommitments.filterNot { it.isFulfilled }.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amountBigDecimal) }

    val discretionaryBudget: BigDecimal
        get() = totalAllowanceBigDecimal.subtract(reservedUnfulfilledFixedBigDecimal).coerceAtLeast(BigDecimal.ZERO)
}

data class WeeklyBudgetSummary(
    val totalAllowance: BigDecimal,
    val currencySymbol: String = "₹",
    val fixedCommitments: List<FixedCommitment>,
    val totalFixedReserved: BigDecimal,
    val discretionaryBudget: BigDecimal,
    val totalSpentThisWeek: BigDecimal,
    val discretionarySpentThisWeek: BigDecimal,
    val discretionaryRemaining: BigDecimal,
    val daysRemainingInWeek: Int,
    val safeToSpendToday: BigDecimal,
    val isOverBudget: Boolean
)
