package com.expensevault.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.model.Account
import com.expensevault.core.model.AccountType
import com.expensevault.core.model.BinanceSyncState
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.BinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.math.BigDecimal
import android.content.Context

data class AccountUiState(
    val accounts: List<Account> = emptyList(),
    val totalBalance: BigDecimal = BigDecimal.ZERO,
    val defaultAccountId: Long = -1L,
    val showAddDialog: Boolean = false,
    val showBinanceDialog: Boolean = false,
    val binanceSyncState: BinanceSyncState = BinanceSyncState(),
    val editingAccount: Account? = null,
    val deleteError: String? = null,
    val formName: String = "",
    val formType: AccountType = AccountType.CASH,
    val formCurrency: String = "INR",
    val formInitialBalance: String = "",
    val formColorHex: String = "#4CAF50"
)

class AccountViewModel(
    private val accountRepository: AccountRepository,
    private val binanceRepository: BinanceRepository,
    private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
        val defId = prefs.getLong("pref_default_account_id", -1L)
        _uiState.update { it.copy(defaultAccountId = defId) }
        loadAccounts()
        observeBinanceSyncState()
    }

    private fun observeBinanceSyncState() {
        viewModelScope.launch {
            binanceRepository.syncState.collect { state ->
                _uiState.update { it.copy(binanceSyncState = state) }
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAccounts().collect { accounts ->
                val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
                var defId = prefs.getLong("pref_default_account_id", -1L)
                if (defId == -1L && accounts.isNotEmpty()) {
                    defId = accounts.first().id
                    prefs.edit().putLong("pref_default_account_id", defId).apply()
                }
                val total = accounts.filter { !it.isArchived }.sumOf { it.currentBalance }
                _uiState.update { it.copy(accounts = accounts, totalBalance = total, defaultAccountId = defId) }
            }
        }
    }

    fun syncBinanceAccount(accountId: Long? = null) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
            val baseCurrency = prefs.getString("pref_base_currency", "INR") ?: "INR"
            binanceRepository.syncAccount(baseCurrency)
        }
    }

    fun showBinanceDialog(show: Boolean = true) {
        _uiState.update { it.copy(showBinanceDialog = show) }
    }

    fun saveBinanceCredentials(apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            val saveResult = binanceRepository.saveCredentials(apiKey, apiSecret)
            if (saveResult.isSuccess) {
                val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
                val baseCurrency = prefs.getString("pref_base_currency", "INR") ?: "INR"
                binanceRepository.syncAccount(baseCurrency)
            }
        }
    }

    fun testBinanceConnection(apiKey: String, apiSecret: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = binanceRepository.testConnection(apiKey, apiSecret)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }

    fun unlinkBinance() {
        viewModelScope.launch {
            binanceRepository.unlinkAccount()
        }
    }

    fun setDefaultAccount(accountId: Long) {
        val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
        prefs.edit().putLong("pref_default_account_id", accountId).apply()
        _uiState.update { it.copy(defaultAccountId = accountId) }
    }

    fun showDialog(account: Account? = null) {
        if (account != null) {
            _uiState.update {
                it.copy(
                    showAddDialog = true,
                    editingAccount = account,
                    formName = account.name,
                    formType = account.type,
                    formCurrency = account.defaultCurrency,
                    formInitialBalance = account.initialBalance.toString(),
                    formColorHex = account.colorHex ?: "#4CAF50"
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    showAddDialog = true,
                    editingAccount = null,
                    formName = "",
                    formType = AccountType.CASH,
                    formCurrency = "INR",
                    formInitialBalance = "",
                    formColorHex = "#4CAF50"
                )
            }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingAccount = null) }
    }

    fun updateFormName(name: String) { _uiState.update { it.copy(formName = name) } }
    fun updateFormType(type: AccountType) { _uiState.update { it.copy(formType = type) } }
    fun updateFormCurrency(currency: String) { _uiState.update { it.copy(formCurrency = currency) } }
    fun updateFormInitialBalance(balance: String) { _uiState.update { it.copy(formInitialBalance = balance) } }
    fun updateFormColorHex(color: String) { _uiState.update { it.copy(formColorHex = color) } }
    fun dismissDeleteError() { _uiState.update { it.copy(deleteError = null) } }

    fun saveAccount() {
        val state = _uiState.value
        val initialBal = try {
            if (state.formInitialBalance.isNotBlank()) BigDecimal(state.formInitialBalance) else BigDecimal.ZERO
        } catch (e: Exception) {
            BigDecimal.ZERO
        }

        viewModelScope.launch {
            if (state.editingAccount != null) {
                val updated = state.editingAccount.copy(
                    name = state.formName,
                    type = state.formType,
                    defaultCurrency = state.formCurrency,
                    initialBalance = initialBal,
                    colorHex = state.formColorHex,
                    updatedAt = Clock.System.now()
                )
                accountRepository.updateAccount(updated)
            } else {
                val newAccount = Account(
                    name = state.formName,
                    type = state.formType,
                    defaultCurrency = state.formCurrency,
                    initialBalance = initialBal,
                    currentBalance = initialBal,
                    colorHex = state.formColorHex,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                accountRepository.addAccount(newAccount)
            }
            dismissDialog()
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            try {
                accountRepository.deleteAccount(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(deleteError = "Cannot delete account with existing transactions.") }
            }
        }
    }
}
