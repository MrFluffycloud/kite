package com.expensevault.core.domain.repository

import com.expensevault.core.model.SavingsTip
import com.expensevault.core.model.SpendSummary

interface SpendAdvisorRepository {
    suspend fun getSavingsAdvice(summary: SpendSummary): List<SavingsTip>
}
