package com.expensevault.core.database.dao

import androidx.room.*
import com.expensevault.core.database.entity.BudgetCapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCapDao {
    @Query("SELECT * FROM budget_caps")
    fun getAll(): Flow<List<BudgetCapEntity>>

    @Query("SELECT * FROM budget_caps WHERE categoryId = :categoryId")
    suspend fun getByCategoryId(categoryId: Long): BudgetCapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budgetCap: BudgetCapEntity): Long

    @Update
    suspend fun update(budgetCap: BudgetCapEntity)

    @Delete
    suspend fun delete(budgetCap: BudgetCapEntity)
}
