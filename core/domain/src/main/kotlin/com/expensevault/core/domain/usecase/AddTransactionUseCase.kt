package com.expensevault.core.domain.usecase

import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.ExchangeRateRepository
import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionType
import java.math.BigDecimal

/**
 * Use case for adding a new transaction.
 *
 * Validates the input, handles exchange rate lookup for multi-currency
 * transactions, updates the account balance, and persists the transaction.
 */
class AddTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        return try {
            // Validate amount
            require(transaction.originalAmount > BigDecimal.ZERO) {
                "Amount must be greater than zero"
            }

            // Validate account exists
            val account = accountRepository.getAccountById(transaction.accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))

            // Handle exchange rate for multi-currency transactions
            var baseAmount = transaction.baseAmount
            if (transaction.originalCurrency != account.defaultCurrency && transaction.exchangeRate == null) {
                val rate = exchangeRateRepository.getLatestRate(
                    transaction.originalCurrency,
                    account.defaultCurrency
                )
                if (rate != null) {
                    baseAmount = transaction.originalAmount.multiply(rate.rate)
                } else {
                    return Result.failure(
                        IllegalStateException(
                            "Exchange rate not available for ${transaction.originalCurrency} → ${account.defaultCurrency}. " +
                                "Please enter the rate manually."
                        )
                    )
                }
            }

            // Update the final transaction with computed base amount
            val finalTransaction = transaction.copy(baseAmount = baseAmount)

            // Adjust account balance based on transaction type
            val balanceChange = when (finalTransaction.type) {
                TransactionType.EXPENSE -> baseAmount.negate()
                TransactionType.INCOME -> baseAmount
                TransactionType.TRANSFER -> baseAmount.negate() // debit from source
            }
            val newBalance = account.currentBalance.add(balanceChange)
            accountRepository.updateBalance(account.id, newBalance)

            // Persist the transaction
            val id = transactionRepository.addTransaction(finalTransaction)
            Result.success(id)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
