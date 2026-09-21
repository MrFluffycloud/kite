package com.expensevault.core.domain.usecase

import com.expensevault.core.domain.repository.DebtRepository
import com.expensevault.core.model.DebtDirection
import com.expensevault.core.model.DebtRecord
import com.expensevault.core.model.DebtStatus
import com.expensevault.core.model.SplitMethod
import com.expensevault.core.model.Transaction
import kotlinx.datetime.Clock
import java.math.BigDecimal
import java.math.RoundingMode

class SplitExpenseUseCase(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(
        transaction: Transaction,
        personIds: List<Long>,
        splitMethod: SplitMethod,
        customAmounts: Map<Long, BigDecimal> = emptyMap(),
        customPercentages: Map<Long, BigDecimal> = emptyMap(),
        splitAmount: BigDecimal? = null
    ): Result<List<Long>> {
        return try {
            val totalAmount = if (splitAmount != null && splitAmount > BigDecimal.ZERO) splitAmount else transaction.originalAmount
            val debts = mutableListOf<DebtRecord>()
            
            when (splitMethod) {
                SplitMethod.EQUAL -> {
                    // Splits among participants plus the payer (1 + personIds.size)
                    // Each other person owes their equal share
                    val totalParticipants = (personIds.size + 1).toBigDecimal()
                    val amountPerPerson = totalAmount.divide(totalParticipants, 2, RoundingMode.HALF_UP)
                    personIds.forEach { personId ->
                        debts.add(createDebt(transaction, personId, amountPerPerson, splitMethod))
                    }
                }
                SplitMethod.CUSTOM_AMOUNT -> {
                    personIds.forEach { personId ->
                        val amount = customAmounts[personId] ?: BigDecimal.ZERO
                        if (amount > BigDecimal.ZERO) {
                            debts.add(createDebt(transaction, personId, amount, splitMethod))
                        }
                    }
                }
                SplitMethod.PERCENTAGE -> {
                    personIds.forEach { personId ->
                        val percentage = customPercentages[personId] ?: BigDecimal.ZERO
                        if (percentage > BigDecimal.ZERO) {
                            val amount = totalAmount.multiply(percentage).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                            debts.add(createDebt(transaction, personId, amount, splitMethod))
                        }
                    }
                }
            }
            
            val ids = debts.map { debtRepository.addDebt(it) }
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun createDebt(
        transaction: Transaction,
        personId: Long,
        amount: BigDecimal,
        splitMethod: SplitMethod
    ): DebtRecord {
        return DebtRecord(
            id = 0,
            personId = personId,
            transactionId = transaction.id,
            amount = amount,
            currency = transaction.originalCurrency,
            direction = DebtDirection.THEY_OWE_ME,
            status = DebtStatus.OPEN,
            splitMethod = splitMethod,
            note = transaction.merchant ?: transaction.note ?: "Split expense",
            createdAt = Clock.System.now(),
            settledAt = null
        )
    }
}
