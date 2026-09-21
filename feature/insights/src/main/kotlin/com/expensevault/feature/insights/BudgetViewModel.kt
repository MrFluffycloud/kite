package com.expensevault.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.BudgetRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.domain.usecase.BudgetStatus
import com.expensevault.core.domain.usecase.CheckBudgetUseCase
import com.expensevault.core.model.BudgetCap
import com.expensevault.core.model.Category
import com.expensevault.core.model.FixedCommitment
import com.expensevault.core.model.TransactionType
import com.expensevault.core.model.WeeklyAllowanceConfig
import com.expensevault.core.model.WeeklyBudgetSummary
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

data class BudgetUiState(
    val weeklyConfig: WeeklyAllowanceConfig = WeeklyAllowanceConfig(),
    val weeklySummary: WeeklyBudgetSummary? = null,
    val showEditAllowanceSheet: Boolean = false,
    val showAddFixedCommitmentSheet: Boolean = false,
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val showAddSheet: Boolean = false,
    val editingBudget: BudgetCap? = null,
    val categoryId: String = "",
    val limitAmount: String = "",
    val period: String = "Monthly",
    val warningThreshold: Float = 0.8f,
    val isLoading: Boolean = false
)

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val checkBudgetUseCase: CheckBudgetUseCase,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _sheetState = MutableStateFlow(
        Triple(
            false, // showEditAllowanceSheet
            false, // showAddFixedCommitmentSheet
            false  // showAddSheet
        )
    )

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRepository.getWeeklyAllowanceConfig(),
        transactionRepository.getTransactions(),
        categoryRepository.getCategories(),
        _sheetState
    ) { config, transactions, categories, sheets ->
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val isoDay = today.dayOfWeek.isoDayNumber // 1 = Monday, 7 = Sunday
        val mondayThisWeek = today.minus(isoDay - 1, DateTimeUnit.DAY)

        val thisWeekExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && it.transactionDate >= mondayThisWeek && it.transactionDate <= today
        }
        val totalSpentThisWeek = thisWeekExpenses.sumOf { it.baseAmount }

        // Calculate fulfilled fixed commitments
        val updatedCommitments = config.fixedCommitments.map { commitment ->
            val matchingExpense = thisWeekExpenses.any {
                (it.merchant?.contains(commitment.name, ignoreCase = true) == true) ||
                (it.note?.contains(commitment.name, ignoreCase = true) == true)
            }
            if (matchingExpense && !commitment.isFulfilled) {
                commitment.copy(isFulfilled = true)
            } else {
                commitment
            }
        }

        val totalAllowance = config.totalAllowanceBigDecimal
        val totalFixedReserved = updatedCommitments.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amountBigDecimal) }
        val unfulfilledFixed = updatedCommitments.filterNot { it.isFulfilled }.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amountBigDecimal) }

        val discretionaryBudget = totalAllowance.subtract(unfulfilledFixed).coerceAtLeast(BigDecimal.ZERO)
        val fulfilledAmount = updatedCommitments.filter { it.isFulfilled }.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amountBigDecimal) }
        val discretionarySpentThisWeek = totalSpentThisWeek.subtract(fulfilledAmount).coerceAtLeast(BigDecimal.ZERO)
        val discretionaryRemaining = discretionaryBudget.subtract(discretionarySpentThisWeek)

        val daysRemainingInWeek = (8 - isoDay).coerceIn(1, 7)
        val safeToSpendToday = if (discretionaryRemaining > BigDecimal.ZERO) {
            discretionaryRemaining.divide(BigDecimal(daysRemainingInWeek), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val summary = WeeklyBudgetSummary(
            totalAllowance = totalAllowance,
            currencySymbol = config.currencySymbol,
            fixedCommitments = updatedCommitments,
            totalFixedReserved = totalFixedReserved,
            discretionaryBudget = discretionaryBudget,
            totalSpentThisWeek = totalSpentThisWeek,
            discretionarySpentThisWeek = discretionarySpentThisWeek,
            discretionaryRemaining = discretionaryRemaining,
            daysRemainingInWeek = daysRemainingInWeek,
            safeToSpendToday = safeToSpendToday,
            isOverBudget = discretionaryRemaining < BigDecimal.ZERO
        )

        BudgetUiState(
            weeklyConfig = config.copy(fixedCommitments = updatedCommitments),
            weeklySummary = summary,
            showEditAllowanceSheet = sheets.first,
            showAddFixedCommitmentSheet = sheets.second,
            showAddSheet = sheets.third,
            availableCategories = categories
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetUiState())

    fun updateWeeklyAllowance(amount: String, currencySymbol: String = uiState.value.weeklyConfig.currencySymbol) {
        viewModelScope.launch {
            val current = uiState.value.weeklyConfig
            budgetRepository.saveWeeklyAllowanceConfig(
                current.copy(
                    totalAllowance = amount,
                    currencySymbol = currencySymbol
                )
            )
            hideEditAllowanceSheet()
        }
    }

    fun addFixedCommitment(name: String, amount: String) {
        viewModelScope.launch {
            val current = uiState.value.weeklyConfig
            val newCommitment = FixedCommitment(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                amount = amount
            )
            val updated = current.fixedCommitments + newCommitment
            budgetRepository.saveWeeklyAllowanceConfig(current.copy(fixedCommitments = updated))
            hideAddFixedCommitmentSheet()
        }
    }

    fun removeFixedCommitment(id: String) {
        viewModelScope.launch {
            val current = uiState.value.weeklyConfig
            val updated = current.fixedCommitments.filterNot { it.id == id }
            budgetRepository.saveWeeklyAllowanceConfig(current.copy(fixedCommitments = updated))
        }
    }

    fun toggleCommitmentFulfilled(id: String) {
        viewModelScope.launch {
            val current = uiState.value.weeklyConfig
            val updated = current.fixedCommitments.map {
                if (it.id == id) it.copy(isFulfilled = !it.isFulfilled) else it
            }
            budgetRepository.saveWeeklyAllowanceConfig(current.copy(fixedCommitments = updated))
        }
    }

    fun showEditAllowanceSheet() {
        _sheetState.update { it.copy(first = true) }
    }

    fun hideEditAllowanceSheet() {
        _sheetState.update { it.copy(first = false) }
    }

    fun showAddFixedCommitmentSheet() {
        _sheetState.update { it.copy(second = true) }
    }

    fun hideAddFixedCommitmentSheet() {
        _sheetState.update { it.copy(second = false) }
    }

    fun showAddSheet() {
        _sheetState.update { it.copy(third = true) }
    }

    fun hideAddSheet() {
        _sheetState.update { it.copy(third = false) }
    }
}
