package com.expensevault.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.model.Account
import com.expensevault.core.model.Category
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionType
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.domain.usecase.AddTransactionUseCase
import com.expensevault.core.domain.usecase.ImportDataUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import java.math.BigDecimal

data class TransactionDisplayItem(
    val transaction: Transaction,
    val category: Category?,
    val account: Account? = null
)

enum class TimePeriod(val title: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL("All")
}

data class TransactionListUiState(
    val transactions: Map<LocalDate, List<TransactionDisplayItem>> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedPeriod: TimePeriod = TimePeriod.THIS_MONTH,
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = true,
    val importMessage: String? = null
)

private data class FilterState(
    val period: TimePeriod,
    val query: String,
    val selectedCategoryId: Long?,
    val isRefreshing: Boolean,
    val importMessage: String?
)

private data class DataState(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val accounts: List<Account>
)

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val importDataUseCase: ImportDataUseCase,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _importMessage = MutableStateFlow<String?>(null)

    private val _filterState = combine(
        _selectedPeriod,
        _searchQuery,
        _selectedCategoryId,
        _isRefreshing,
        _importMessage
    ) { period, query, catId, refreshing, importMsg ->
        FilterState(period, query, catId, refreshing, importMsg)
    }

    private val _dataState = combine(
        transactionRepository.getTransactions(),
        categoryRepository.getCategories(),
        accountRepository.getAccounts()
    ) { transactions, categories, accounts ->
        DataState(transactions, categories, accounts)
    }

    val uiState: StateFlow<TransactionListUiState> = combine(
        _filterState,
        _dataState
    ) { filter, data ->
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val periodFiltered = when (filter.period) {
            TimePeriod.TODAY -> data.transactions.filter { it.transactionDate == today }
            TimePeriod.THIS_WEEK -> {
                val weekAgo = today.minus(7, DateTimeUnit.DAY)
                data.transactions.filter { it.transactionDate >= weekAgo }
            }
            TimePeriod.THIS_MONTH -> {
                data.transactions.filter { it.transactionDate.month == today.month && it.transactionDate.year == today.year }
            }
            TimePeriod.ALL -> data.transactions
        }

        val categoryMap = data.categories.associateBy { it.id }
        val accountMap = data.accounts.associateBy { it.id }

        val searchFiltered = periodFiltered.filter { t ->
            val matchesCategory = filter.selectedCategoryId == null || t.categoryId == filter.selectedCategoryId
            val matchesSearch = filter.query.isBlank() ||
                (t.merchant?.contains(filter.query, ignoreCase = true) == true) ||
                (t.note?.contains(filter.query, ignoreCase = true) == true) ||
                t.originalAmount.toPlainString().contains(filter.query) ||
                (t.categoryId?.let { categoryMap[it]?.name }?.contains(filter.query, ignoreCase = true) == true) ||
                (accountMap[t.accountId]?.name?.contains(filter.query, ignoreCase = true) == true)

            matchesCategory && matchesSearch
        }

        val displayItems = searchFiltered.map { transaction ->
            TransactionDisplayItem(
                transaction = transaction,
                category = transaction.categoryId?.let { categoryMap[it] },
                account = accountMap[transaction.accountId]
            )
        }

        val grouped = displayItems.groupBy { it.transaction.transactionDate }
            .toSortedMap(compareByDescending { it })

        TransactionListUiState(
            transactions = grouped,
            categories = data.categories,
            accounts = data.accounts,
            selectedPeriod = filter.period,
            searchQuery = filter.query,
            selectedCategoryId = filter.selectedCategoryId,
            isLoading = false,
            isRefreshing = filter.isRefreshing,
            isEmpty = grouped.isEmpty(),
            importMessage = filter.importMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionListUiState()
    )

    fun onPeriodChange(period: TimePeriod) {
        _selectedPeriod.update { period }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun onSelectCategory(categoryId: Long?) {
        _selectedCategoryId.update { if (it == categoryId) null else categoryId }
    }

    fun onDeleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
        }
    }

    fun duplicateTransaction(transaction: Transaction, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val duplicate = transaction.copy(
                id = 0,
                createdAt = Clock.System.now(),
                transactionDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
            )
            addTransactionUseCase(duplicate)
            onComplete()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(500)
            _isRefreshing.value = false
        }
    }

    fun importData(content: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = importDataUseCase.import(content)
            _isRefreshing.value = false
            if (result.isSuccess) {
                val res = result.getOrThrow()
                _importMessage.value = "Imported ${res.importedCount} transactions successfully"
            } else {
                _importMessage.value = "Import failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }
}
