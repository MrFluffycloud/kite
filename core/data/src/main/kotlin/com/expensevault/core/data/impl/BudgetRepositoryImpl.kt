package com.expensevault.core.data.impl

import com.expensevault.core.database.dao.BudgetCapDao
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.domain.repository.BudgetRepository
import com.expensevault.core.model.BudgetCap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val budgetCapDao: BudgetCapDao
) : BudgetRepository {
    override fun getBudgetCaps(): Flow<List<BudgetCap>> =
        budgetCapDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getBudgetCapByCategoryId(categoryId: Long): BudgetCap? =
        budgetCapDao.getByCategoryId(categoryId)?.toDomain()

    override suspend fun addBudgetCap(budgetCap: BudgetCap): Long =
        budgetCapDao.insert(budgetCap.toEntity())

    override suspend fun updateBudgetCap(budgetCap: BudgetCap) =
        budgetCapDao.update(budgetCap.toEntity())

    override suspend fun deleteBudgetCap(budgetCap: BudgetCap) =
        budgetCapDao.delete(budgetCap.toEntity())
}
