package com.expensevault.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.CategoryRepository
import com.expensevault.core.model.Account
import com.expensevault.core.model.AccountType
import com.expensevault.core.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.math.BigDecimal

data class OnboardingUiState(
    val selectedCurrency: String = "INR",
    val accountName: String = "Primary Wallet",
    val initialBalance: String = "0",
    val enableAutoDetect: Boolean = false,
    val isCompleted: Boolean = false,
    val isSubmitting: Boolean = false
)

class OnboardingViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onCurrencySelected(currency: String) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun onAccountNameChange(name: String) {
        _uiState.update { it.copy(accountName = name) }
    }

    fun onInitialBalanceChange(balance: String) {
        _uiState.update { it.copy(initialBalance = balance.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun onToggleAutoDetect(enabled: Boolean) {
        _uiState.update { it.copy(enableAutoDetect = enabled) }
    }

    fun completeOnboarding(context: Context, onDone: () -> Unit) {
        val state = _uiState.value
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                val now = Clock.System.now()
                val parsedBalance = try {
                    BigDecimal(state.initialBalance.ifBlank { "0" })
                } catch (e: Exception) {
                    BigDecimal.ZERO
                }

                // 1. Seed primary account if no accounts exist
                val existingAccounts = accountRepository.getAccounts().firstOrNull() ?: emptyList()
                if (existingAccounts.isEmpty()) {
                    accountRepository.addAccount(
                        Account(
                            name = state.accountName.ifBlank { "Primary Wallet" },
                            type = AccountType.BANK_ACCOUNT,
                            defaultCurrency = state.selectedCurrency,
                            initialBalance = parsedBalance,
                            currentBalance = parsedBalance,
                            iconName = "account_balance",
                            colorHex = "#2196F3",
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }

                // 2. Seed starter categories if empty
                val existingCategories = categoryRepository.getCategories().firstOrNull() ?: emptyList()
                if (existingCategories.isEmpty()) {
                    val starterCategories = listOf(
                        Triple("Food & Dining", "#FF5722", "restaurant"),
                        Triple("Groceries", "#4CAF50", "shopping_cart"),
                        Triple("Transport", "#03A9F4", "directions_car"),
                        Triple("Bills & Utilities", "#FF9800", "receipt_long"),
                        Triple("Shopping", "#9C27B0", "shopping_bag"),
                        Triple("Entertainment", "#E91E63", "movie"),
                        Triple("Health", "#00BCD4", "local_hospital")
                    )

                    starterCategories.forEachIndexed { index, (name, color, icon) ->
                        categoryRepository.addCategory(
                            Category(
                                name = name,
                                colorHex = color,
                                iconName = icon,
                                isDefault = true,
                                sortOrder = index,
                                createdAt = now
                            )
                        )
                    }
                }

                // 3. Save settings in SharedPreferences
                val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_onboarding_completed", true)
                    .putString("base_currency", state.selectedCurrency)
                    .putBoolean("auto_detect_notifications", state.enableAutoDetect)
                    .apply()

                _uiState.update { it.copy(isCompleted = true, isSubmitting = false) }
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
