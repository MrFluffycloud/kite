package com.expensevault.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.usecase.GetInsightsUseCase
import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.domain.model.CategorySpendDisplay
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionType
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import java.math.BigDecimal

enum class InsightsPeriod { THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_QUARTER, THIS_YEAR }

data class InsightsUiState(
    val selectedPeriod: InsightsPeriod = InsightsPeriod.THIS_MONTH,
    val totalSpend: BigDecimal = BigDecimal.ZERO,
    val previousPeriodSpend: BigDecimal? = null,
    val percentChange: Double? = null,
    val categorySpends: List<CategorySpendDisplay> = emptyList(),
    val hasSufficientCategoryData: Boolean = false,
    val hasSufficientTrendData: Boolean = false,
    val topTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false
)

class InsightsViewModel(
    private val getInsightsUseCase: GetInsightsUseCase,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(InsightsPeriod.THIS_MONTH)

    val barChartModelProducer = CartesianChartModelProducer()
    val lineChartModelProducer = CartesianChartModelProducer()

    val uiState: StateFlow<InsightsUiState> = combine(
        _selectedPeriod,
        transactionRepository.getTransactions(),
        categoryRepository.getCategories()
    ) { period, allTransactions, categories ->
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // 1. Filter current & previous period transactions
        val (periodTransactions, prevTransactions) = when (period) {
            InsightsPeriod.THIS_WEEK -> {
                val weekAgo = today.minus(7, DateTimeUnit.DAY)
                val twoWeeksAgo = today.minus(14, DateTimeUnit.DAY)
                Pair(
                    allTransactions.filter { it.transactionDate >= weekAgo && it.transactionDate <= today },
                    allTransactions.filter { it.transactionDate >= twoWeeksAgo && it.transactionDate < weekAgo }
                )
            }
            InsightsPeriod.THIS_MONTH -> {
                val prevMonthDate = today.minus(1, DateTimeUnit.MONTH)
                Pair(
                    allTransactions.filter { it.transactionDate.month == today.month && it.transactionDate.year == today.year },
                    allTransactions.filter { it.transactionDate.month == prevMonthDate.month && it.transactionDate.year == prevMonthDate.year }
                )
            }
            InsightsPeriod.LAST_MONTH -> {
                val lastMonthDate = today.minus(1, DateTimeUnit.MONTH)
                val twoMonthsAgoDate = lastMonthDate.minus(1, DateTimeUnit.MONTH)
                Pair(
                    allTransactions.filter { it.transactionDate.month == lastMonthDate.month && it.transactionDate.year == lastMonthDate.year },
                    allTransactions.filter { it.transactionDate.month == twoMonthsAgoDate.month && it.transactionDate.year == twoMonthsAgoDate.year }
                )
            }
            InsightsPeriod.THIS_QUARTER -> {
                val quarterAgo = today.minus(90, DateTimeUnit.DAY)
                val twoQuartersAgo = today.minus(180, DateTimeUnit.DAY)
                Pair(
                    allTransactions.filter { it.transactionDate >= quarterAgo && it.transactionDate <= today },
                    allTransactions.filter { it.transactionDate >= twoQuartersAgo && it.transactionDate < quarterAgo }
                )
            }
            InsightsPeriod.THIS_YEAR -> {
                Pair(
                    allTransactions.filter { it.transactionDate.year == today.year },
                    allTransactions.filter { it.transactionDate.year == today.year - 1 }
                )
            }
        }

        val periodExpenses = periodTransactions.filter { it.type == TransactionType.EXPENSE }
        val totalSpend = periodExpenses.sumOf { it.baseAmount }

        // Previous period comparison (only if valid previous data exists)
        val prevExpenses = prevTransactions.filter { it.type == TransactionType.EXPENSE }
        val prevTotal = prevExpenses.sumOf { it.baseAmount }
        val percentChange = if (prevExpenses.isNotEmpty() && prevTotal > BigDecimal.ZERO) {
            ((totalSpend.toDouble() - prevTotal.toDouble()) / prevTotal.toDouble()) * 100.0
        } else {
            null
        }

        // 2. Spending by Category
        val categoryMap = categories.associateBy { it.id }
        val expensesByCategory = periodExpenses
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.baseAmount } }

        val categorySpends = expensesByCategory.entries
            .sortedByDescending { it.value }
            .mapNotNull { (catId, amount) ->
                val cat = catId?.let { categoryMap[it] }
                if (cat != null && totalSpend > BigDecimal.ZERO) {
                    CategorySpendDisplay(
                        name = cat.name,
                        amount = amount,
                        percentage = (amount.toFloat() / totalSpend.toFloat()),
                        colorHex = cat.colorHex ?: "#808080"
                    )
                } else null
            }

        // Honest data gate: need at least 2 distinct categories and at least 3 transactions
        val hasSufficientCategoryData = categorySpends.size >= 2 && periodExpenses.size >= 3

        if (hasSufficientCategoryData) {
            val amounts = categorySpends.map { it.amount.toFloat() }
            viewModelScope.launch {
                barChartModelProducer.runTransaction {
                    columnSeries { series(amounts) }
                }
            }
        }

        // 3. Daily Spending Trend
        // Honest data gate: need at least 3 distinct days with transaction data or at least 3 transactions
        val distinctSpendDays = periodExpenses.map { it.transactionDate }.distinct().size
        val hasSufficientTrendData = distinctSpendDays >= 2 && periodExpenses.size >= 3

        if (hasSufficientTrendData) {
            val daysToTrack = when (period) {
                InsightsPeriod.THIS_WEEK -> 7
                InsightsPeriod.THIS_MONTH -> today.dayOfMonth.coerceIn(7, 31)
                else -> 14
            }
            val dailyPoints = (daysToTrack - 1 downTo 0).map { daysAgo ->
                val d = today.minus(daysAgo, DateTimeUnit.DAY)
                periodExpenses.filter { it.transactionDate == d }.sumOf { it.baseAmount }.toFloat()
            }
            viewModelScope.launch {
                lineChartModelProducer.runTransaction {
                    lineSeries { series(dailyPoints) }
                }
            }
        }

        val topTransactions = periodExpenses
            .sortedByDescending { it.baseAmount }
            .take(5)

        InsightsUiState(
            selectedPeriod = period,
            totalSpend = totalSpend,
            previousPeriodSpend = if (prevExpenses.isNotEmpty()) prevTotal else null,
            percentChange = percentChange,
            categorySpends = categorySpends,
            hasSufficientCategoryData = hasSufficientCategoryData,
            hasSufficientTrendData = hasSufficientTrendData,
            topTransactions = topTransactions,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())

    fun onPeriodSelected(period: InsightsPeriod) {
        _selectedPeriod.value = period
    }
}
