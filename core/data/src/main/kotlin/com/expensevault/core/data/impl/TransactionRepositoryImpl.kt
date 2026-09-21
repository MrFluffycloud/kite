package com.expensevault.core.data.impl

import com.expensevault.core.database.dao.AccountDao
import com.expensevault.core.database.dao.TransactionDao
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.domain.model.CategorySpend
import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : TransactionRepository {
    override fun getTransactions(): Flow<List<Transaction>> =
        transactionDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByDateRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startMillis, endMillis).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByCategory(categoryId: Long, startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        transactionDao.getByCategory(categoryId, startMillis, endMillis).map { list -> list.map { it.toDomain() } }

    override suspend fun getTransactionById(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun addTransaction(transaction: Transaction): Long =
        transactionDao.insert(transaction.toEntity())

    override suspend fun updateTransaction(transaction: Transaction) {
        val existing = transactionDao.getById(transaction.id)
        if (existing != null) {
            // Revert previous transaction's effect on its account balance
            val oldReversal = when (existing.type) {
                TransactionType.EXPENSE -> existing.baseAmount
                TransactionType.INCOME -> existing.baseAmount.negate()
                TransactionType.TRANSFER -> existing.baseAmount
            }
            accountDao.updateBalance(existing.accountId, oldReversal)

            // Apply updated transaction's effect on its account balance
            val newDelta = when (transaction.type) {
                TransactionType.EXPENSE -> transaction.baseAmount.negate()
                TransactionType.INCOME -> transaction.baseAmount
                TransactionType.TRANSFER -> transaction.baseAmount.negate()
            }
            accountDao.updateBalance(transaction.accountId, newDelta)
        }
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: Long) {
        val existing = transactionDao.getById(id)
        if (existing != null) {
            val balanceReversal = when (existing.type) {
                TransactionType.EXPENSE -> existing.baseAmount
                TransactionType.INCOME -> existing.baseAmount.negate()
                TransactionType.TRANSFER -> existing.baseAmount
            }
            accountDao.updateBalance(existing.accountId, balanceReversal)
            transactionDao.deleteById(id)
        }
    }

    override fun getTotalSpendByDateRange(startMillis: Long, endMillis: Long): Flow<BigDecimal> =
        transactionDao.getTotalSpendByDateRange(startMillis, endMillis).map { BigDecimal.valueOf(it) }

    override fun getTopTransactions(limit: Int, startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        transactionDao.getTopNByDateRange(limit, startMillis, endMillis).map { list -> list.map { it.toDomain() } }

    override fun getSpendByCategoryInRange(startMillis: Long, endMillis: Long): Flow<List<CategorySpend>> =
        transactionDao.getSpendByCategoryInRange(startMillis, endMillis).map { list -> 
            list.map {
                CategorySpend(
                    categoryId = it.categoryId,
                    categoryName = it.categoryName ?: "Other",
                    totalSpend = BigDecimal.valueOf(it.total),
                    colorHex = it.colorHex
                )
            }
        }
}
