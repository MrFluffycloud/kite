package com.expensevault.core.database.dao

import androidx.room.*
import com.expensevault.core.database.entity.VaultExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultExpenseDao {
    @Query("SELECT * FROM vault_expenses ORDER BY date DESC, id DESC")
    fun getAll(): Flow<List<VaultExpenseEntity>>

    @Query("SELECT * FROM vault_expenses WHERE id = :id")
    suspend fun getById(id: Long): VaultExpenseEntity?

    @Query("SELECT * FROM vault_expenses WHERE isSharedWithPartner = 1 ORDER BY date DESC")
    fun getSharedWithPartner(): Flow<List<VaultExpenseEntity>>

    @Query("SELECT COALESCE(SUM(CAST(amount AS REAL)), 0.0) FROM vault_expenses")
    fun getTotalSpend(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: VaultExpenseEntity): Long

    @Update
    suspend fun update(expense: VaultExpenseEntity)

    @Query("DELETE FROM vault_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_expenses")
    suspend fun deleteAll()
}
