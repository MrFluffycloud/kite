package com.expensevault.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.domain.repository.RecurringRepository
import com.expensevault.core.domain.util.RecurringDateCalculator
import com.expensevault.core.model.Account
import com.expensevault.core.model.Category
import com.expensevault.core.model.RecurringFrequency
import com.expensevault.core.model.RecurringRule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

data class AddEditRecurringUiState(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val amountString: String = "",
    val currency: String = "INR",
    val note: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val dayOfWeek: Int = 1,
    val dayOfMonth: Int = 1,
    val customIntervalDays: String = "30",
    val requireConfirmation: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AddEditRecurringRuleViewModel(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditRecurringUiState())
    val uiState: StateFlow<AddEditRecurringUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.getAccounts().collect { list ->
                _uiState.update { current ->
                    current.copy(
                        accounts = list,
                        selectedAccountId = current.selectedAccountId ?: list.firstOrNull()?.id
                    )
                }
            }
        }

        viewModelScope.launch {
            categoryRepository.getCategories().collect { list ->
                _uiState.update { current ->
                    current.copy(
                        categories = list,
                        selectedCategoryId = current.selectedCategoryId ?: list.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun setAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(amountString = amount) }
        }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun selectAccount(id: Long) {
        _uiState.update { it.copy(selectedAccountId = id) }
    }

    fun selectCategory(id: Long) {
        _uiState.update { it.copy(selectedCategoryId = id) }
    }

    fun setFrequency(freq: RecurringFrequency) {
        _uiState.update { it.copy(frequency = freq) }
    }

    fun setDayOfWeek(day: Int) {
        _uiState.update { it.copy(dayOfWeek = day) }
    }

    fun setDayOfMonth(day: Int) {
        _uiState.update { it.copy(dayOfMonth = day) }
    }

    fun setCustomIntervalDays(days: String) {
        if (days.isEmpty() || days.all { it.isDigit() }) {
            _uiState.update { it.copy(customIntervalDays = days) }
        }
    }

    fun setRequireConfirmation(req: Boolean) {
        _uiState.update { it.copy(requireConfirmation = req) }
    }

    fun saveRule() {
        val state = _uiState.value
        val amount = state.amountString.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }
        val accountId = state.selectedAccountId
        if (accountId == null) {
            _uiState.update { it.copy(errorMessage = "Please select an account") }
            return
        }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val initialRule = RecurringRule(
            id = 0,
            accountId = accountId,
            categoryId = state.selectedCategoryId,
            amount = amount,
            currency = state.currency,
            note = state.note.takeIf { it.isNotBlank() },
            frequency = state.frequency,
            customIntervalDays = state.customIntervalDays.toIntOrNull(),
            dayOfWeek = state.dayOfWeek,
            dayOfMonth = state.dayOfMonth,
            startDate = today,
            endDate = null,
            nextOccurrence = today,
            requireConfirmation = state.requireConfirmation,
            isActive = true,
            createdAt = Clock.System.now()
        )

        // Calculate actual next occurrence based on frequency
        val nextOccurrence = RecurringDateCalculator.calculateNextOccurrence(initialRule, today)
        val finalRule = initialRule.copy(nextOccurrence = nextOccurrence)

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                recurringRepository.addRule(finalRule)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save recurring rule")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
