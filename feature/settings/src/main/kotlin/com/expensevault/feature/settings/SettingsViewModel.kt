package com.expensevault.feature.settings

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.model.Account
import com.expensevault.core.model.AppUpdateInfo
import com.expensevault.core.model.BinanceSyncState
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.AppUpdateRepository
import com.expensevault.core.domain.repository.BinanceRepository
import com.expensevault.core.domain.usecase.ClearAllDataUseCase
import com.expensevault.core.domain.usecase.ExportDataUseCase
import com.expensevault.core.domain.usecase.ExportFormat
import com.expensevault.core.domain.usecase.ExportResult
import com.expensevault.core.domain.usecase.ImportDataUseCase
import com.expensevault.platform.security.AppLockManager
import com.expensevault.platform.security.BiometricAuthManager
import com.expensevault.platform.security.BiometricAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseCurrency: String = "INR",
    val accounts: List<Account> = emptyList(),
    val defaultAccountId: Long = -1L,
    val defaultAccountName: String = "Main wallet",
    val isAppLockEnabled: Boolean = false,
    val isNotificationDetectionEnabled: Boolean = false,
    val showCurrencyDialog: Boolean = false,
    val showDefaultAccountDialog: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val showNotificationDisclosure: Boolean = false,
    val showNoSecurityDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showBinanceDialog: Boolean = false,
    val binanceSyncState: BinanceSyncState = BinanceSyncState(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportResult: ExportResult? = null,
    val importMessage: String? = null,
    val isClearingData: Boolean = false,
    val isCheckingForUpdates: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val showUpdateDialog: Boolean = false
)

class SettingsViewModel(
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val clearAllDataUseCase: ClearAllDataUseCase,
    private val appUpdateRepository: AppUpdateRepository,
    private val accountRepository: AccountRepository,
    private val binanceRepository: BinanceRepository,
    private val appLockManager: AppLockManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isAppLockEnabled = appLockManager.isAppLockEnabled.value))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadAccountsAndDefaults()
        observeSecuritySettings()
        observeBinanceSyncState()
    }

    private fun observeBinanceSyncState() {
        viewModelScope.launch {
            binanceRepository.syncState.collect { state ->
                _uiState.update { it.copy(binanceSyncState = state) }
            }
        }
    }

    fun showBinanceDialog(show: Boolean = true) {
        _uiState.update { it.copy(showBinanceDialog = show) }
    }

    fun saveBinanceCredentials(apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            val res = binanceRepository.saveCredentials(apiKey, apiSecret)
            if (res.isSuccess) {
                binanceRepository.syncAccount(_uiState.value.baseCurrency)
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

    fun syncBinanceNow() {
        viewModelScope.launch {
            binanceRepository.syncAccount(_uiState.value.baseCurrency)
        }
    }

    fun unlinkBinance() {
        viewModelScope.launch {
            binanceRepository.unlinkAccount()
        }
    }

    private fun observeSecuritySettings() {
        viewModelScope.launch {
            appLockManager.isAppLockEnabled.collect { enabled ->
                _uiState.update { it.copy(isAppLockEnabled = enabled) }
            }
        }
    }

    private fun loadAccountsAndDefaults() {
        viewModelScope.launch {
            accountRepository.getAccounts().collect { accounts ->
                val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
                val defId = prefs.getLong("pref_default_account_id", -1L)
                val matched = accounts.find { it.id == defId } ?: accounts.firstOrNull()
                _uiState.update {
                    it.copy(
                        accounts = accounts,
                        defaultAccountId = matched?.id ?: -1L,
                        defaultAccountName = matched?.name ?: "Main wallet"
                    )
                }
            }
        }
    }

    fun setDefaultAccount(accountId: Long) {
        val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
        prefs.edit().putLong("pref_default_account_id", accountId).apply()
        val matched = _uiState.value.accounts.find { it.id == accountId }
        _uiState.update {
            it.copy(
                defaultAccountId = accountId,
                defaultAccountName = matched?.name ?: "Main wallet",
                showDefaultAccountDialog = false
            )
        }
    }

    fun showDefaultAccountDialog() {
        _uiState.update { it.copy(showDefaultAccountDialog = true) }
    }

    fun hideDefaultAccountDialog() {
        _uiState.update { it.copy(showDefaultAccountDialog = false) }
    }

    fun setBaseCurrency(currency: String) {
        _uiState.update { it.copy(baseCurrency = currency) }
    }

    fun canAuthenticateSecurity(): Boolean {
        return biometricAuthManager.canAuthenticate() == BiometricAvailability.AVAILABLE || biometricAuthManager.isDeviceSecure()
    }

    fun requestToggleAppLock(
        activity: FragmentActivity,
        enable: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (enable) {
            if (!canAuthenticateSecurity()) {
                _uiState.update { it.copy(showNoSecurityDialog = true) }
                return
            }
            biometricAuthManager.authenticate(
                activity = activity,
                title = "Enable App Lock",
                subtitle = "Confirm your biometric or device PIN to secure Kite",
                onSuccess = {
                    appLockManager.setLockEnabled(true)
                    appLockManager.unlock()
                    onSuccess()
                },
                onError = { err -> onError(err) }
            )
        } else {
            biometricAuthManager.authenticate(
                activity = activity,
                title = "Disable App Lock",
                subtitle = "Confirm your identity to disable app lock",
                onSuccess = {
                    appLockManager.setLockEnabled(false)
                    onSuccess()
                },
                onError = { err -> onError(err) }
            )
        }
    }

    fun dismissNoSecurityDialog() {
        _uiState.update { it.copy(showNoSecurityDialog = false) }
    }

    fun setNotificationDetection(enabled: Boolean) {
        _uiState.update { it.copy(isNotificationDetectionEnabled = enabled) }
    }

    fun showNotificationDisclosure() {
        _uiState.update { it.copy(showNotificationDisclosure = true) }
    }

    fun hideNotificationDisclosure() {
        _uiState.update { it.copy(showNotificationDisclosure = false) }
    }

    fun showCurrencyDialog() {
        _uiState.update { it.copy(showCurrencyDialog = true) }
    }

    fun hideCurrencyDialog() {
        _uiState.update { it.copy(showCurrencyDialog = false) }
    }

    fun showClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = true) }
    }

    fun hideClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = false) }
    }

    fun clearAllData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingData = true) }
            val result = clearAllDataUseCase()
            loadAccountsAndDefaults()
            _uiState.update { it.copy(isClearingData = false, showClearDataDialog = false) }
            onComplete(result.isSuccess)
        }
    }

    fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingForUpdates = true) }
            val result = appUpdateRepository.checkForUpdate(currentVersion)
            _uiState.update {
                it.copy(
                    isCheckingForUpdates = false,
                    updateInfo = result.getOrNull(),
                    showUpdateDialog = true
                )
            }
        }
    }

    fun hideUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun showExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }

    fun hideExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }

    fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, showExportDialog = false) }
            try {
                val result = exportDataUseCase.export(format)
                _uiState.update { it.copy(isExporting = false, exportResult = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    fun importData(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val result = importDataUseCase.import(content)
            _uiState.update {
                it.copy(
                    isImporting = false,
                    importMessage = if (result.isSuccess) {
                        "Imported ${result.getOrThrow().importedCount} transactions successfully"
                    } else {
                        "Import failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                    }
                )
            }
        }
    }

    fun clearImportMessage() {
        _uiState.update { it.copy(importMessage = null) }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }
}
