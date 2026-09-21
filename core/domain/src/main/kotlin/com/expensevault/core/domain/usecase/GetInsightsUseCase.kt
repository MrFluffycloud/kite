package com.expensevault.core.domain.usecase

import com.expensevault.core.domain.model.CategorySpend
import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

data class InsightsData(
    val totalSpend: BigDecimal,
    val categoryBreaks: List<CategorySpend>,
    val topTransactions: List<Transaction>,
    val periodLabel: String
)

class GetInsightsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(startMillis: Long, endMillis: Long, periodLabel: String): Flow<InsightsData> {
        val totalSpendFlow = transactionRepository.getTotalSpendByDateRange(startMillis, endMillis)
        val categoryBreaksFlow = transactionRepository.getSpendByCategoryInRange(startMillis, endMillis)
        val topTransactionsFlow = transactionRepository.getTopTransactions(5, startMillis, endMillis)
        
        return combine(totalSpendFlow, categoryBreaksFlow, topTransactionsFlow) { totalSpend, categoryBreaks, topTransactions ->
            InsightsData(
                totalSpend = totalSpend ?: BigDecimal.ZERO,
                categoryBreaks = categoryBreaks,
                topTransactions = topTransactions,
                periodLabel = periodLabel
            )
        }
    }
}
