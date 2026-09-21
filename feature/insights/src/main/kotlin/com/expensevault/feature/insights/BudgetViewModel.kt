package com.expensevault.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.BudgetRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.domain.usecase.CheckBudgetUseCase
import com.expensevault.core.domain.usecase.BudgetStatus
import com.expensevault.core.model.BudgetCap
import com.expensevault.core.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetUiState(
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
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadBudgets()
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Mock load
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun showAddSheet() {
        _uiState.update { it.copy(showAddSheet = true) }
    }
    
    fun hideAddSheet() {
        _uiState.update { it.copy(showAddSheet = false) }
    }
}
