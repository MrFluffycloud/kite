package com.expensevault.core.data.impl

import android.content.Context
import com.expensevault.core.database.dao.BudgetCapDao
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.domain.repository.BudgetRepository
import com.expensevault.core.model.BudgetCap
import com.expensevault.core.model.WeeklyAllowanceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class BudgetRepositoryImpl(
    private val budgetCapDao: BudgetCapDao,
    private val context: Context
) : BudgetRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val prefs = context.getSharedPreferences("expense_vault_budgets", Context.MODE_PRIVATE)
    private val _weeklyAllowance = MutableStateFlow(loadWeeklyAllowance())

    private fun loadWeeklyAllowance(): WeeklyAllowanceConfig {
        val raw = prefs.getString("key_weekly_allowance", null) ?: return WeeklyAllowanceConfig()
        return try {
            json.decodeFromString<WeeklyAllowanceConfig>(raw)
        } catch (_: Exception) {
            WeeklyAllowanceConfig()
        }
    }

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

    override fun getWeeklyAllowanceConfig(): Flow<WeeklyAllowanceConfig> = _weeklyAllowance

    override suspend fun saveWeeklyAllowanceConfig(config: WeeklyAllowanceConfig) {
        val raw = json.encodeToString(WeeklyAllowanceConfig.serializer(), config)
        prefs.edit().putString("key_weekly_allowance", raw).apply()
        _weeklyAllowance.value = config
    }
}
