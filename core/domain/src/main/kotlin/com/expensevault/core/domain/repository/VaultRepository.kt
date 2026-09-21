package com.expensevault.core.domain.repository

import com.expensevault.core.model.VaultExpense
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface VaultRepository {
    fun getVaultExpenses(): Flow<List<VaultExpense>>
    suspend fun getVaultExpenseById(id: Long): VaultExpense?
    fun getSharedWithPartnerExpenses(): Flow<List<VaultExpense>>
    fun getTotalVaultSpend(): Flow<BigDecimal>
    suspend fun addVaultExpense(expense: VaultExpense): Long
    suspend fun updateVaultExpense(expense: VaultExpense)
    suspend fun deleteVaultExpense(id: Long)
    suspend fun exportVaultCsv(): String
}
