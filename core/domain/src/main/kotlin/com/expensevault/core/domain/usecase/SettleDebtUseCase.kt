package com.expensevault.core.domain.usecase

import com.expensevault.core.domain.repository.DebtRepository
import com.expensevault.core.model.DebtDirection
import com.expensevault.core.model.DebtStatus
import com.expensevault.core.model.Settlement
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

class SettleDebtUseCase(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(
        personId: Long,
        note: String? = "Settled all outstanding balances",
        settleAmount: BigDecimal? = null
    ): Result<Settlement> {
        return try {
            val openDebts = debtRepository.getDebtsByPerson(personId)
                .first()
                .filter { it.status == DebtStatus.OPEN }
                
            if (openDebts.isEmpty()) {
                return Result.failure(IllegalStateException("No open debts to settle"))
            }
            
            var netAmount = BigDecimal.ZERO
            var currency = "INR"
            openDebts.forEach { debt ->
                currency = debt.currency
                if (debt.direction == DebtDirection.THEY_OWE_ME) {
                    netAmount = netAmount.add(debt.amount)
                } else {
                    netAmount = netAmount.subtract(debt.amount)
                }
            }

            val totalAbs = netAmount.abs()
            val amountToSettle = if (settleAmount != null && settleAmount > BigDecimal.ZERO && settleAmount < totalAbs) {
                settleAmount
            } else {
                totalAbs
            }

            val now = Clock.System.now()
            val fullySettledIds = mutableListOf<Long>()
            val involvedDebtIds = mutableListOf<Long>()

            if (amountToSettle >= totalAbs) {
                openDebts.forEach {
                    fullySettledIds.add(it.id)
                    involvedDebtIds.add(it.id)
                }
            } else {
                var remaining = amountToSettle
                for (debt in openDebts) {
                    if (remaining <= BigDecimal.ZERO) break
                    involvedDebtIds.add(debt.id)
                    if (debt.amount <= remaining) {
                        fullySettledIds.add(debt.id)
                        remaining = remaining.subtract(debt.amount)
                    } else {
                        val newDebtAmount = debt.amount.subtract(remaining)
                        debtRepository.updateDebt(debt.copy(amount = newDebtAmount))
                        remaining = BigDecimal.ZERO
                    }
                }
            }
            
            val settlement = Settlement(
                id = 0,
                personId = personId,
                settledAmount = amountToSettle,
                currency = currency,
                note = note ?: if (amountToSettle < totalAbs) "Partial settlement" else "Settled all balances",
                settledAt = now,
                debtRecordIds = involvedDebtIds
            )
            
            debtRepository.settleDebts(personId, fullySettledIds, settlement)
            
            Result.success(settlement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
