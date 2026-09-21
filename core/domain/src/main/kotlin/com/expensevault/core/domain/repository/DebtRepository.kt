package com.expensevault.core.domain.repository

import com.expensevault.core.model.DebtRecord
import com.expensevault.core.model.Settlement
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface DebtRepository {
    fun getAllDebts(): Flow<List<DebtRecord>>
    fun getDebtsByPerson(personId: Long): Flow<List<DebtRecord>>
    fun getOpenDebts(): Flow<List<DebtRecord>>
    suspend fun getNetBalance(personId: Long): BigDecimal
    suspend fun addDebt(debt: DebtRecord): Long
    suspend fun updateDebt(debt: DebtRecord)
    suspend fun settleDebts(personId: Long, debtIds: List<Long>, settlement: Settlement)
    fun getSettlementsByPerson(personId: Long): Flow<List<Settlement>>
}
