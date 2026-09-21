package com.expensevault.core.domain.usecase

import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.model.BudgetCap
import com.expensevault.core.model.TransactionType
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode

data class BudgetStatus(
    val categoryId: Long,
    val categoryName: String,
    val spent: BigDecimal,
    val limit: BigDecimal,
    val percentage: Double,
    val isWarning: Boolean,
    val isExceeded: Boolean
)

class CheckBudgetUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(budgetCap: BudgetCap, startMillis: Long, endMillis: Long, categoryName: String = ""): BudgetStatus {
        val transactions = transactionRepository.getTransactionsByCategory(budgetCap.categoryId, startMillis, endMillis).first()
        
        var spent = BigDecimal.ZERO
        transactions.filter { it.type == TransactionType.EXPENSE }.forEach { tx ->
            spent = spent.add(tx.baseAmount)
        }
        
        val limit = budgetCap.limitAmount
        val percentage = if (limit > BigDecimal.ZERO) {
            spent.multiply(BigDecimal(100)).divide(limit, 2, RoundingMode.HALF_UP).toDouble()
        } else {
            0.0
        }
        
        return BudgetStatus(
            categoryId = budgetCap.categoryId,
            categoryName = categoryName,
            spent = spent,
            limit = limit,
            percentage = percentage,
            isWarning = percentage >= (budgetCap.warningThreshold * 100).toDouble() && percentage < 100.0,
            isExceeded = percentage >= 100.0
        )
    }
}
