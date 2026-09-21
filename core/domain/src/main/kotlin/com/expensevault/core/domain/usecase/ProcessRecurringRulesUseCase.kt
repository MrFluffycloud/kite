package com.expensevault.core.domain.usecase

import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.RecurringRepository
import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.domain.util.RecurringDateCalculator
import com.expensevault.core.model.RecurringRule
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionSource
import com.expensevault.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ProcessRecurringResult(
    val autoLoggedCount: Int,
    val pendingConfirmationRules: List<RecurringRule>
)

class ProcessRecurringRulesUseCase(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        currentDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): ProcessRecurringResult {
        val dueRules = recurringRepository.getDueRules(currentDate).first()
        var loggedCount = 0
        val pendingConfirmation = mutableListOf<RecurringRule>()

        for (rule in dueRules) {
            if (rule.requireConfirmation) {
                pendingConfirmation.add(rule)
            } else {
                // Auto-log transaction
                logRecurringTransaction(rule)
                loggedCount++

                // Advance next occurrence
                advanceRule(rule)
            }
        }

        return ProcessRecurringResult(
            autoLoggedCount = loggedCount,
            pendingConfirmationRules = pendingConfirmation
        )
    }

    suspend fun confirmRule(rule: RecurringRule) {
        logRecurringTransaction(rule)
        advanceRule(rule)
    }

    suspend fun skipRule(rule: RecurringRule) {
        advanceRule(rule)
    }

    private suspend fun logRecurringTransaction(rule: RecurringRule) {
        val now = Clock.System.now()
        val tx = Transaction(
            id = 0,
            accountId = rule.accountId,
            categoryId = rule.categoryId,
            type = TransactionType.EXPENSE,
            originalAmount = rule.amount,
            originalCurrency = rule.currency,
            exchangeRate = null,
            baseAmount = rule.amount,
            note = rule.note ?: "Recurring expense",
            merchant = null,
            transactionDate = rule.nextOccurrence,
            createdAt = now,
            updatedAt = now,
            recurringRuleId = rule.id,
            source = TransactionSource.AUTO_RECURRING
        )

        transactionRepository.addTransaction(tx)

        // Deduct from account balance
        val account = accountRepository.getAccountById(rule.accountId)
        if (account != null) {
            val newBalance = account.currentBalance.subtract(rule.amount)
            accountRepository.updateAccount(account.copy(currentBalance = newBalance, updatedAt = now))
        }
    }

    private suspend fun advanceRule(rule: RecurringRule) {
        val nextDate = RecurringDateCalculator.calculateNextOccurrence(rule)
        val endDate = rule.endDate
        val shouldDeactivate = endDate != null && nextDate > endDate

        recurringRepository.updateRule(
            rule.copy(
                nextOccurrence = nextDate,
                isActive = !shouldDeactivate
            )
        )
    }
}
