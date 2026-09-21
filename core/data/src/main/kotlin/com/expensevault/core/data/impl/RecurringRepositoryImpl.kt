package com.expensevault.core.data.impl

import com.expensevault.core.database.dao.RecurringRuleDao
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.database.mapper.toEpochDaysLong
import com.expensevault.core.domain.repository.RecurringRepository
import com.expensevault.core.model.RecurringRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class RecurringRepositoryImpl(
    private val recurringRuleDao: RecurringRuleDao
) : RecurringRepository {

    override fun getAllRules(): Flow<List<RecurringRule>> =
        recurringRuleDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getActiveRules(): Flow<List<RecurringRule>> =
        recurringRuleDao.getActiveRules().map { list -> list.map { it.toDomain() } }

    override suspend fun getRuleById(id: Long): RecurringRule? =
        recurringRuleDao.getById(id)?.toDomain()

    override fun getDueRules(currentDate: LocalDate): Flow<List<RecurringRule>> =
        recurringRuleDao.getDueRules(currentDate.toEpochDaysLong()).map { list -> list.map { it.toDomain() } }

    override suspend fun addRule(rule: RecurringRule): Long =
        recurringRuleDao.insert(rule.toEntity())

    override suspend fun updateRule(rule: RecurringRule) {
        recurringRuleDao.update(rule.toEntity())
    }

    override suspend fun deleteRule(rule: RecurringRule) {
        recurringRuleDao.delete(rule.toEntity())
    }

    override suspend fun deactivateRule(id: Long) {
        recurringRuleDao.deactivate(id)
    }
}
