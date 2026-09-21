package com.expensevault.core.domain.repository

import com.expensevault.core.domain.model.CategorySpend
import com.expensevault.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: Long, startMillis: Long, endMillis: Long): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun addTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: Long)
    fun getTotalSpendByDateRange(startMillis: Long, endMillis: Long): Flow<BigDecimal>
    fun getTopTransactions(limit: Int, startMillis: Long, endMillis: Long): Flow<List<Transaction>>
    fun getSpendByCategoryInRange(startMillis: Long, endMillis: Long): Flow<List<CategorySpend>>
}
