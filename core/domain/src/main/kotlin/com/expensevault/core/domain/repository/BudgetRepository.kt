package com.expensevault.core.domain.repository

import com.expensevault.core.model.BudgetCap
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetCaps(): Flow<List<BudgetCap>>
    suspend fun getBudgetCapByCategoryId(categoryId: Long): BudgetCap?
    suspend fun addBudgetCap(budgetCap: BudgetCap): Long
    suspend fun updateBudgetCap(budgetCap: BudgetCap)
    suspend fun deleteBudgetCap(budgetCap: BudgetCap)
}
