package com.expensevault.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.model.CategorySpendDisplay
import com.expensevault.core.domain.repository.BudgetRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.domain.repository.SpendAdvisorRepository
import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.domain.usecase.GetInsightsUseCase
import com.expensevault.core.model.SavingsTip
import com.expensevault.core.model.SpendSummary
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionType
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

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
    val weeklyAverageSpend: BigDecimal = BigDecimal.ZERO,
    val currentWeekSpend: BigDecimal = BigDecimal.ZERO,
    val currencySymbol: String = "£",
    val savingsTips: List<SavingsTip> = emptyList(),
    val isAdvisorLoading: Boolean = false,
    val isLoading: Boolean = false
)

private data class InsightsCalculations(
    val selectedPeriod: InsightsPeriod,
    val totalSpend: BigDecimal,
    val previousPeriodSpend: BigDecimal?,
    val percentChange: Double?,
    val categorySpends: List<CategorySpendDisplay>,
    val hasSufficientCategoryData: Boolean,
    val hasSufficientTrendData: Boolean,
    val topTransactions: List<Transaction>,
    val weeklyAverageSpend: BigDecimal,
    val currentWeekSpend: BigDecimal,
    val currencySymbol: String
)

class InsightsViewModel(
    private val getInsightsUseCase: GetInsightsUseCase,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val spendAdvisorRepository: SpendAdvisorRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(InsightsPeriod.THIS_MONTH)
    private val _savingsTips = MutableStateFlow<List<SavingsTip>>(emptyList())
    private val _isAdvisorLoading = MutableStateFlow(false)

    val barChartModelProducer = CartesianChartModelProducer()
    val lineChartModelProducer = CartesianChartModelProducer()

    private val calculationsFlow = combine(
        _selectedPeriod,
        transactionRepository.getTransactions(),
        categoryRepository.getCategories(),
        budgetRepository.getWeeklyAllowanceConfig()
    ) { period, allTransactions, categories, weeklyConfig ->
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

        // Previous period comparison
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

        // 4. Rolling 4-Week Average & Weekly Spend
        val twentyEightDaysAgo = today.minus(28, DateTimeUnit.DAY)
        val last28DaysExpenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE && it.transactionDate >= twentyEightDaysAgo && it.transactionDate <= today
        }
        val total28DaysSpend = last28DaysExpenses.sumOf { it.baseAmount }
        val weeklyAverageSpend = if (total28DaysSpend > BigDecimal.ZERO) {
            total28DaysSpend.divide(BigDecimal(4), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val isoDay = today.dayOfWeek.isoDayNumber
        val mondayThisWeek = today.minus(isoDay - 1, DateTimeUnit.DAY)
        val thisWeekExpenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE && it.transactionDate >= mondayThisWeek && it.transactionDate <= today
        }
        val currentWeekSpend = thisWeekExpenses.sumOf { it.baseAmount }

        val topTransactions = periodExpenses
            .sortedByDescending { it.baseAmount }
            .take(5)

        InsightsCalculations(
            selectedPeriod = period,
            totalSpend = totalSpend,
            previousPeriodSpend = if (prevExpenses.isNotEmpty()) prevTotal else null,
            percentChange = percentChange,
            categorySpends = categorySpends,
            hasSufficientCategoryData = hasSufficientCategoryData,
            hasSufficientTrendData = hasSufficientTrendData,
            topTransactions = topTransactions,
            weeklyAverageSpend = weeklyAverageSpend,
            currentWeekSpend = currentWeekSpend,
            currencySymbol = weeklyConfig.currencySymbol
        )
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        calculationsFlow,
        _savingsTips,
        _isAdvisorLoading
    ) { calc, tips, advisorLoading ->
        InsightsUiState(
            selectedPeriod = calc.selectedPeriod,
            totalSpend = calc.totalSpend,
            previousPeriodSpend = calc.previousPeriodSpend,
            percentChange = calc.percentChange,
            categorySpends = calc.categorySpends,
            hasSufficientCategoryData = calc.hasSufficientCategoryData,
            hasSufficientTrendData = calc.hasSufficientTrendData,
            topTransactions = calc.topTransactions,
            weeklyAverageSpend = calc.weeklyAverageSpend,
            currentWeekSpend = calc.currentWeekSpend,
            currencySymbol = calc.currencySymbol,
            savingsTips = tips,
            isAdvisorLoading = advisorLoading,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())

    init {
        // Automatically generate initial advice once transactions are available
        viewModelScope.launch {
            val transactions = transactionRepository.getTransactions().first()
            val categories = categoryRepository.getCategories().first()
            val weeklyConfig = budgetRepository.getWeeklyAllowanceConfig().first()

            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val twentyEightDaysAgo = today.minus(28, DateTimeUnit.DAY)
            val last28DaysExpenses = transactions.filter {
                it.type == TransactionType.EXPENSE && it.transactionDate >= twentyEightDaysAgo && it.transactionDate <= today
            }
            val total28DaysSpend = last28DaysExpenses.sumOf { it.baseAmount }
            val avg = if (total28DaysSpend > BigDecimal.ZERO) {
                total28DaysSpend.divide(BigDecimal(4), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            val isoDay = today.dayOfWeek.isoDayNumber
            val mondayThisWeek = today.minus(isoDay - 1, DateTimeUnit.DAY)
            val thisWeekSpend = transactions.filter {
                it.type == TransactionType.EXPENSE && it.transactionDate >= mondayThisWeek && it.transactionDate <= today
            }.sumOf { it.baseAmount }

            val categoryMap = categories.associateBy { it.id }
            val topCats = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .map { (catId, txs) ->
                    (categoryMap[catId]?.name ?: "General") to txs.sumOf { it.baseAmount }
                }
                .sortedByDescending { it.second }

            val summary = SpendSummary(
                weeklyAverageSpend = avg,
                currentWeekSpend = thisWeekSpend,
                topCategories = topCats,
                currencySymbol = weeklyConfig.currencySymbol
            )
            _isAdvisorLoading.value = true
            _savingsTips.value = spendAdvisorRepository.getSavingsAdvice(summary)
            _isAdvisorLoading.value = false
        }
    }

    fun onPeriodSelected(period: InsightsPeriod) {
        _selectedPeriod.value = period
    }

    fun refreshSavingsAdvice() {
        viewModelScope.launch {
            _isAdvisorLoading.value = true
            val state = uiState.value
            val topCats = state.categorySpends.map { it.name to it.amount }
            val summary = SpendSummary(
                weeklyAverageSpend = state.weeklyAverageSpend,
                currentWeekSpend = state.currentWeekSpend,
                topCategories = topCats,
                currencySymbol = state.currencySymbol
            )
            _savingsTips.value = spendAdvisorRepository.getSavingsAdvice(summary)
            _isAdvisorLoading.value = false
        }
    }
}
