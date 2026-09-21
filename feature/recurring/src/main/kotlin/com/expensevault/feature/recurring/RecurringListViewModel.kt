package com.expensevault.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.domain.repository.RecurringRepository
import com.expensevault.core.domain.usecase.ProcessRecurringRulesUseCase
import com.expensevault.core.model.RecurringRule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RecurringRuleWithDetails(
    val rule: RecurringRule,
    val categoryName: String?,
    val accountName: String?
)

data class RecurringListUiState(
    val activeRules: List<RecurringRuleWithDetails> = emptyList(),
    val pausedRules: List<RecurringRuleWithDetails> = emptyList(),
    val pendingConfirmations: List<RecurringRuleWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val autoLoggedMessage: String? = null
)

class RecurringListViewModel(
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val processRecurringRulesUseCase: ProcessRecurringRulesUseCase
) : ViewModel() {

    private val _pendingConfirmations = MutableStateFlow<List<RecurringRule>>(emptyList())
    private val _autoLoggedMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecurringListUiState> = combine(
        recurringRepository.getAllRules(),
        categoryRepository.getCategories(),
        accountRepository.getAccounts(),
        _pendingConfirmations,
        _autoLoggedMessage
    ) { allRules, categories, accounts, pending, message ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }

        val detailedRules = allRules.map { rule ->
            RecurringRuleWithDetails(
                rule = rule,
                categoryName = rule.categoryId?.let { categoryMap[it]?.name },
                accountName = accountMap[rule.accountId]?.name
            )
        }

        val detailedPending = pending.map { rule ->
            RecurringRuleWithDetails(
                rule = rule,
                categoryName = rule.categoryId?.let { categoryMap[it]?.name },
                accountName = accountMap[rule.accountId]?.name
            )
        }

        val (active, paused) = detailedRules.partition { it.rule.isActive }

        RecurringListUiState(
            activeRules = active,
            pausedRules = paused,
            pendingConfirmations = detailedPending,
            autoLoggedMessage = message,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringListUiState(isLoading = true)
    )

    init {
        // Cold-start catch-up: process due rules on launch
        viewModelScope.launch {
            try {
                val result = processRecurringRulesUseCase()
                if (result.autoLoggedCount > 0) {
                    _autoLoggedMessage.value = "Auto-logged ${result.autoLoggedCount} scheduled expense(s)"
                }
                _pendingConfirmations.value = result.pendingConfirmationRules
            } catch (_: Exception) {
                // Background catch-up failure handled gracefully
            }
        }
    }

    fun toggleRuleActive(rule: RecurringRule) {
        viewModelScope.launch {
            recurringRepository.updateRule(rule.copy(isActive = !rule.isActive))
        }
    }

    fun deleteRule(rule: RecurringRule) {
        viewModelScope.launch {
            recurringRepository.deleteRule(rule)
        }
    }

    fun confirmPendingRule(rule: RecurringRule) {
        viewModelScope.launch {
            processRecurringRulesUseCase.confirmRule(rule)
            _pendingConfirmations.update { list -> list.filter { it.id != rule.id } }
        }
    }

    fun skipPendingRule(rule: RecurringRule) {
        viewModelScope.launch {
            processRecurringRulesUseCase.skipRule(rule)
            _pendingConfirmations.update { list -> list.filter { it.id != rule.id } }
        }
    }

    fun dismissMessage() {
        _autoLoggedMessage.value = null
    }
}
