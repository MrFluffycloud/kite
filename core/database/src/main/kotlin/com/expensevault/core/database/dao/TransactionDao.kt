package com.expensevault.core.database.dao

import androidx.room.*
import com.expensevault.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import com.expensevault.core.database.model.CategorySpendTuple

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, id DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transactionDate >= :start AND transactionDate <= :end ORDER BY transactionDate DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND transactionDate >= :start AND transactionDate <= :end ORDER BY transactionDate DESC")
    fun getByCategory(categoryId: Long, start: Long, end: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE deduplicationKey = :key LIMIT 1")
    suspend fun getByDeduplicationKey(key: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE transactionDate >= :start AND transactionDate <= :end ORDER BY baseAmount DESC LIMIT :n")
    fun getTopNByDateRange(n: Int, start: Long, end: Long): Flow<List<TransactionEntity>>

    // Aggregation queries for insights
    @Query("""
        SELECT COALESCE(SUM(CAST(baseAmount AS REAL)), 0.0) 
        FROM transactions 
        WHERE type = 'EXPENSE' 
        AND transactionDate >= :start AND transactionDate <= :end
    """)
    fun getTotalSpendByDateRange(start: Long, end: Long): Flow<Double>

    @Query("""
        SELECT t.categoryId as categoryId, 
               c.name as categoryName,
               c.colorHex as colorHex,
               COALESCE(SUM(CAST(t.baseAmount AS REAL)), 0.0) as total
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE' 
        AND t.transactionDate >= :start AND t.transactionDate <= :end 
        AND t.categoryId IS NOT NULL
        GROUP BY t.categoryId, c.name, c.colorHex 
        ORDER BY total DESC
    """)
    fun getSpendByCategoryInRange(start: Long, end: Long): Flow<List<CategorySpendTuple>>

    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE transactionDate >= :start AND transactionDate <= :end
    """)
    fun getTransactionCountByDateRange(start: Long, end: Long): Flow<Int>
}
