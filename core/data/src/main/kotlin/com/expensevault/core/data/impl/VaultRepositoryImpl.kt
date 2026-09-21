package com.expensevault.core.data.impl

import com.expensevault.core.database.dao.VaultExpenseDao
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.domain.repository.VaultRepository
import com.expensevault.core.model.VaultExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class VaultRepositoryImpl(
    private val daoProvider: () -> VaultExpenseDao
) : VaultRepository {

    private val dao: VaultExpenseDao
        get() = daoProvider()

    override fun getVaultExpenses(): Flow<List<VaultExpense>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getVaultExpenseById(id: Long): VaultExpense? =
        dao.getById(id)?.toDomain()

    override fun getSharedWithPartnerExpenses(): Flow<List<VaultExpense>> =
        dao.getSharedWithPartner().map { list -> list.map { it.toDomain() } }

    override fun getTotalVaultSpend(): Flow<BigDecimal> =
        dao.getTotalSpend().map { BigDecimal(it.toString()) }

    override suspend fun addVaultExpense(expense: VaultExpense): Long =
        dao.insert(expense.toEntity())

    override suspend fun updateVaultExpense(expense: VaultExpense) {
        dao.update(expense.toEntity())
    }

    override suspend fun deleteVaultExpense(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun exportVaultCsv(): String {
        val expenses = getVaultExpenses().first()
        val sb = StringBuilder()
        sb.append("ID,Date,Title,Amount,Currency,Category,SharedWithPartner,PartnerShare,Note\n")
        expenses.forEach { e ->
            sb.append("${e.id},${e.date},\"${e.title.replace("\"", "\"\"")}\",${e.amount},${e.currency},\"${e.category}\",${e.isSharedWithPartner},${e.partnerShare ?: ""},\"${(e.note ?: "").replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }
}
