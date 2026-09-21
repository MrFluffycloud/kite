package com.expensevault.feature.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.usecase.AddTransactionUseCase
import com.expensevault.core.model.Account
import com.expensevault.core.model.CategoryWithSubcategories
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionSource
import com.expensevault.core.model.TransactionType
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class AddTransactionUiState(
    val amountString: String = "",
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val note: String = "",
    val transactionDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val isMoreExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val categories: List<CategoryWithSubcategories> = emptyList(),
    val selectedParentCategoryId: Long? = null,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
    val selectedCurrency: String = "",
    val exchangeRateInput: String = "",
    val destinationAccountId: Long? = null
)

class AddTransactionViewModel(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private fun sortCategoriesForType(
        categories: List<CategoryWithSubcategories>,
        type: TransactionType
    ): List<CategoryWithSubcategories> {
        return if (type == TransactionType.INCOME) {
            categories.sortedByDescending { it.isIncomeCategory }
        } else {
            categories.sortedBy { it.isIncomeCategory }
        }
    }

    init {
        viewModelScope.launch {
            combine(
                accountRepository.getActiveAccounts(),
                categoryRepository.getCategoriesWithSubcategories()
            ) { accounts, categories ->
                Pair(accounts, categories)
            }.collect { (accounts, categories) ->
                _uiState.update { state ->
                    val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
                    val defaultAccountId = prefs.getLong("pref_default_account_id", -1L)
                    val defaultAccount = if (defaultAccountId != -1L) accounts.find { it.id == defaultAccountId } else null
                    val selectedAccountId = state.selectedAccountId ?: defaultAccount?.id ?: accounts.firstOrNull()?.id
                    val selectedAccount = accounts.find { it.id == selectedAccountId }

                    val sortedCategories = sortCategoriesForType(categories, state.transactionType)
                    val initialParent = state.selectedParentCategoryId ?: sortedCategories.firstOrNull()?.category?.id
                    val initialChild = state.selectedCategoryId ?: run {
                        val parent = sortedCategories.find { it.category.id == initialParent }
                        parent?.subcategories?.firstOrNull()?.id ?: initialParent
                    }

                    val initialDestId = state.destinationAccountId ?: accounts.find { it.id != selectedAccountId }?.id

                    state.copy(
                        accounts = accounts,
                        categories = sortedCategories,
                        selectedAccountId = selectedAccountId,
                        selectedParentCategoryId = initialParent,
                        selectedCategoryId = initialChild,
                        selectedCurrency = state.selectedCurrency.ifEmpty { selectedAccount?.defaultCurrency ?: "" },
                        exchangeRateInput = if (state.exchangeRateInput.isEmpty()) "1.0" else state.exchangeRateInput,
                        destinationAccountId = initialDestId
                    )
                }
            }
        }
    }

    fun onDigitPress(digit: String) {
        _uiState.update { state ->
            if (state.amountString.contains(".") && state.amountString.substringAfter(".").length >= 2) {
                state // limit to 2 decimal places
            } else {
                val newAmount = if (state.amountString == "0") digit else state.amountString + digit
                state.copy(amountString = newAmount)
            }
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            val newAmount = if (state.amountString.length > 1) {
                state.amountString.dropLast(1)
            } else {
                ""
            }
            state.copy(amountString = newAmount)
        }
    }

    fun onDecimalPress() {
        _uiState.update { state ->
            if (state.amountString.contains(".")) {
                state
            } else {
                state.copy(amountString = if (state.amountString.isEmpty()) "0." else "${state.amountString}.")
            }
        }
    }

    fun onQuickAmountSelect(amount: Int) {
        _uiState.update { state ->
            val currentVal = state.amountString.toDoubleOrNull() ?: 0.0
            val newVal = if (currentVal == 0.0) amount.toDouble() else currentVal + amount
            val formatted = if (newVal % 1.0 == 0.0) newVal.toLong().toString() else "%.2f".format(java.util.Locale.US, newVal)
            state.copy(amountString = formatted)
        }
    }

    fun onCategorySelect(categoryId: Long, isParent: Boolean) {
        _uiState.update { state ->
            if (isParent) {
                // Check if this parent has subcategories
                val category = state.categories.find { it.category.id == categoryId }
                if (category?.subcategories.isNullOrEmpty()) {
                    state.copy(selectedCategoryId = categoryId, selectedParentCategoryId = categoryId)
                } else {
                    state.copy(selectedParentCategoryId = categoryId, selectedCategoryId = null)
                }
            } else {
                state.copy(selectedCategoryId = categoryId)
            }
        }
    }

    fun onAccountSelect(accountId: Long) {
        _uiState.update { state -> 
            val account = state.accounts.find { it.id == accountId }
            state.copy(
                selectedAccountId = accountId,
                selectedCurrency = account?.defaultCurrency ?: state.selectedCurrency,
                exchangeRateInput = "1.0"
            ) 
        }
    }

    fun onCurrencySelect(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
        
        viewModelScope.launch {
            val state = _uiState.value
            val accountId = state.selectedAccountId ?: return@launch
            val account = state.accounts.find { it.id == accountId } ?: return@launch
            
            if (currency != account.defaultCurrency) {
                val rate = exchangeRateRepository.getLatestRate(baseCurrency = currency, targetCurrency = account.defaultCurrency)
                if (rate != null) {
                    _uiState.update { it.copy(exchangeRateInput = rate.rate.toString()) }
                }
            } else {
                _uiState.update { it.copy(exchangeRateInput = "1.0") }
            }
        }
    }

    fun onExchangeRateChange(rate: String) {
        _uiState.update { it.copy(exchangeRateInput = rate) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(transactionDate = date) }
    }

    fun prefill(amount: String?, merchant: String?, categoryName: String?) {
        _uiState.update { state ->
            val matchingCategory = if (!categoryName.isNullOrBlank()) {
                state.categories.find { it.category.name.equals(categoryName, ignoreCase = true) }
            } else null

            val type = if (matchingCategory?.isIncomeCategory == true) TransactionType.INCOME else state.transactionType
            val sortedCategories = sortCategoriesForType(state.categories, type)

            state.copy(
                amountString = if (!amount.isNullOrBlank()) amount.replace(",", "") else state.amountString,
                note = if (!merchant.isNullOrBlank() && state.note.isBlank()) merchant else state.note,
                transactionType = type,
                categories = sortedCategories,
                selectedParentCategoryId = matchingCategory?.category?.id ?: state.selectedParentCategoryId,
                selectedCategoryId = matchingCategory?.category?.id ?: state.selectedCategoryId
            )
        }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.update { state ->
            val sortedCategories = sortCategoriesForType(state.categories, type)
            val currentParent = state.categories.find { it.category.id == state.selectedParentCategoryId }
            val isCurrentIncome = currentParent?.isIncomeCategory ?: false
            val needsReselection = (type == TransactionType.INCOME && !isCurrentIncome) || 
                                   (type == TransactionType.EXPENSE && isCurrentIncome)

            val newParentId = if (needsReselection) {
                sortedCategories.firstOrNull()?.category?.id
            } else {
                state.selectedParentCategoryId
            }
            val newCategory = sortedCategories.find { it.category.id == newParentId }
            val newCategoryId = if (needsReselection) {
                newCategory?.subcategories?.firstOrNull()?.id ?: newParentId
            } else {
                state.selectedCategoryId
            }

            state.copy(
                transactionType = type,
                categories = sortedCategories,
                selectedParentCategoryId = newParentId,
                selectedCategoryId = newCategoryId
            )
        }
    }

    fun onDestinationAccountChange(accountId: Long) {
        _uiState.update { it.copy(destinationAccountId = accountId) }
    }

    fun onToggleMore() {
        _uiState.update { it.copy(isMoreExpanded = !it.isMoreExpanded) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveTransaction() {
        val currentState = _uiState.value
        val amount = currentState.amountString.toBigDecimalOrNull()
        if (amount == null || amount <= java.math.BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }

        val accountId = currentState.selectedAccountId ?: return
        val account = currentState.accounts.find { it.id == accountId } ?: return

        val isTransfer = currentState.transactionType == TransactionType.TRANSFER
        val categoryId = if (isTransfer) null else (currentState.selectedCategoryId ?: return)

        if (isTransfer) {
            if (currentState.destinationAccountId == null) {
                _uiState.update { it.copy(errorMessage = "Please select a destination account") }
                return
            }
            if (currentState.destinationAccountId == accountId) {
                _uiState.update { it.copy(errorMessage = "Destination account must be different from source account") }
                return
            }
        }

        val exchangeRate = currentState.exchangeRateInput.toBigDecimalOrNull() ?: java.math.BigDecimal.ONE
        val baseAmount = amount * exchangeRate

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val destAccount = if (isTransfer) currentState.accounts.find { it.id == currentState.destinationAccountId } else null
            val defaultNote = if (isTransfer && destAccount != null) "Transfer to ${destAccount.name}" else null
            val defaultMerchant = if (isTransfer && destAccount != null) "Transfer to ${destAccount.name}" else null

            val transaction = Transaction(
                accountId = accountId,
                categoryId = categoryId,
                type = currentState.transactionType,
                originalAmount = amount,
                originalCurrency = currentState.selectedCurrency.ifEmpty { account.defaultCurrency },
                baseAmount = baseAmount,
                note = currentState.note.ifBlank { defaultNote },
                merchant = defaultMerchant,
                transactionDate = currentState.transactionDate,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                source = TransactionSource.MANUAL
            )

            val result = addTransactionUseCase(transaction, if (isTransfer) currentState.destinationAccountId else null)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } else {
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to save transaction"
                    ) 
                }
            }
        }
    }
}
