package com.expensevault.core.domain.repository

import com.expensevault.core.model.RecurringRule
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface RecurringRepository {
    fun getAllRules(): Flow<List<RecurringRule>>
    fun getActiveRules(): Flow<List<RecurringRule>>
    suspend fun getRuleById(id: Long): RecurringRule?
    fun getDueRules(currentDate: LocalDate): Flow<List<RecurringRule>>
    suspend fun addRule(rule: RecurringRule): Long
    suspend fun updateRule(rule: RecurringRule)
    suspend fun deleteRule(rule: RecurringRule)
    suspend fun deactivateRule(id: Long)
}
