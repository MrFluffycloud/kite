package com.expensevault.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.model.Category
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionType
import com.expensevault.feature.transactions.TransactionDisplayItem
import com.expensevault.core.domain.model.CategorySpendDisplay
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.domain.repository.TransactionRepository
import kotlinx.coroutines.delay
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
import java.math.RoundingMode
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

data class HomeUiState(
    val monthlyTotal: BigDecimal = BigDecimal.ZERO,
    val todayTotal: BigDecimal = BigDecimal.ZERO,
    val weeklyTotal: BigDecimal = BigDecimal.ZERO,
    val monthTransactionCount: Int = 0,
    val recentTransactions: List<TransactionDisplayItem> = emptyList(),
    val topCategories: List<CategorySpendDisplay> = emptyList(),
    val dailySpendsLast7Days: List<Float> = emptyList(),
    val currentMonthLabel: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        _isRefreshing,
        transactionRepository.getTransactions(),
        categoryRepository.getCategories(),
        accountRepository.getAccounts()
    ) { refreshing, allTransactions, categories, accounts ->
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val weekAgo = today.minus(7, DateTimeUnit.DAY)
        
        val thisMonthTransactions = allTransactions.filter { 
            it.transactionDate.month == today.month && it.transactionDate.year == today.year 
        }
        
        val todayTransactions = allTransactions.filter { it.transactionDate == today }
        val weeklyTransactions = allTransactions.filter { it.transactionDate >= weekAgo }
        
        val monthlyTotal = thisMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.baseAmount }
            
        val todayTotal = todayTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.baseAmount }
            
        val weeklyTotal = weeklyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.baseAmount }
            
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        
        val recentTransactions = allTransactions
            .sortedByDescending { it.transactionDate }
            .take(5)
            .map { transaction ->
                TransactionDisplayItem(
                    transaction = transaction,
                    category = transaction.categoryId?.let { categoryMap[it] },
                    account = accountMap[transaction.accountId]
                )
            }
            
        val expensesByCategory = thisMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.baseAmount } }
            
        val topCategories = expensesByCategory.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapNotNull { (catId, amount) ->
                val cat = catId?.let { categoryMap[it] }
                if (cat != null && monthlyTotal > BigDecimal.ZERO) {
                    val percentage = (amount.toFloat() / monthlyTotal.toFloat())
                    CategorySpendDisplay(
                        name = cat.name,
                        amount = amount,
                        percentage = percentage,
                        colorHex = cat.colorHex ?: "#808080"
                    )
                } else null
            }

        val dailySpends = (6 downTo 0).map { daysAgo ->
            val date = today.minus(daysAgo, DateTimeUnit.DAY)
            allTransactions
                .filter { it.transactionDate == date && it.type == TransactionType.EXPENSE }
                .sumOf { it.baseAmount }
                .toFloat()
        }
            
        val monthName = Month.of(today.monthNumber).getDisplayName(TextStyle.FULL, Locale.getDefault())
        val currentMonthLabel = "$monthName ${today.year}"

        HomeUiState(
            monthlyTotal = monthlyTotal,
            todayTotal = todayTotal,
            weeklyTotal = weeklyTotal,
            monthTransactionCount = thisMonthTransactions.size,
            recentTransactions = recentTransactions,
            topCategories = topCategories,
            dailySpendsLast7Days = dailySpends,
            currentMonthLabel = currentMonthLabel,
            isLoading = false,
            isRefreshing = refreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _isRefreshing.value = false
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }
}
