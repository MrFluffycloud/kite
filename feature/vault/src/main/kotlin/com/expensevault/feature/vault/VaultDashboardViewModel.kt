package com.expensevault.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.VaultRepository
import com.expensevault.core.model.VaultExpense
import com.expensevault.platform.security.VaultSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class VaultFilter {
    ALL, PARTNER_ONLY
}

data class VaultDashboardUiState(
    val expenses: List<VaultExpense> = emptyList(),
    val totalSpend: BigDecimal = BigDecimal.ZERO,
    val partnerTotalSpend: BigDecimal = BigDecimal.ZERO,
    val filter: VaultFilter = VaultFilter.ALL,
    val exportData: String? = null,
    val isLocked: Boolean = false
)

class VaultDashboardViewModel(
    private val vaultRepository: VaultRepository,
    private val vaultSessionManager: VaultSessionManager
) : ViewModel() {

    private val _filter = MutableStateFlow(VaultFilter.ALL)
    private val _exportData = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VaultDashboardUiState> = combine(
        vaultRepository.getVaultExpenses(),
        _filter,
        _exportData,
        vaultSessionManager.isVaultUnlocked
    ) { allExpenses, filter, exportData, isUnlocked ->
        var total = BigDecimal.ZERO
        var partnerTotal = BigDecimal.ZERO

        allExpenses.forEach { e ->
            total = total.add(e.amount)
            if (e.isSharedWithPartner) {
                partnerTotal = partnerTotal.add(e.partnerShare ?: e.amount)
            }
        }

        val filtered = when (filter) {
            VaultFilter.ALL -> allExpenses
            VaultFilter.PARTNER_ONLY -> allExpenses.filter { it.isSharedWithPartner }
        }

        VaultDashboardUiState(
            expenses = filtered,
            totalSpend = total,
            partnerTotalSpend = partnerTotal,
            filter = filter,
            exportData = exportData,
            isLocked = !isUnlocked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultDashboardUiState()
    )

    fun setFilter(filter: VaultFilter) {
        _filter.value = filter
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            vaultRepository.deleteVaultExpense(id)
        }
    }

    fun lockVault() {
        vaultSessionManager.lock()
    }

    fun exportCsv() {
        viewModelScope.launch {
            val csv = vaultRepository.exportVaultCsv()
            _exportData.value = csv
        }
    }

    fun dismissExport() {
        _exportData.value = null
    }
}
