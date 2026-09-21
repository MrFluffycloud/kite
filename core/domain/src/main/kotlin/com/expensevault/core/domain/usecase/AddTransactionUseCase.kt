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
    suspend operator fun invoke(
        transaction: Transaction,
        destinationAccountId: Long? = null
    ): Result<Long> {
        return try {
            // Validate amount
            require(transaction.originalAmount > BigDecimal.ZERO) {
                "Amount must be greater than zero"
            }

            // Validate source account exists
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

            var finalMerchant = transaction.merchant
            var finalNote = transaction.note

            // If it's a transfer and destination account is provided
            if (transaction.type == TransactionType.TRANSFER && destinationAccountId != null) {
                require(transaction.accountId != destinationAccountId) {
                    "Source and destination accounts must be different"
                }
                val destAccount = accountRepository.getAccountById(destinationAccountId)
                    ?: return Result.failure(IllegalArgumentException("Destination account not found"))

                // Credit destination account
                val creditAmount = if (transaction.originalCurrency == destAccount.defaultCurrency) {
                    transaction.originalAmount
                } else {
                    val destRate = exchangeRateRepository.getLatestRate(
                        transaction.originalCurrency,
                        destAccount.defaultCurrency
                    )
                    if (destRate != null) {
                        transaction.originalAmount.multiply(destRate.rate)
                    } else {
                        baseAmount // fallback
                    }
                }
                val newDestBalance = destAccount.currentBalance.add(creditAmount)
                accountRepository.updateBalance(destAccount.id, newDestBalance)

                if (finalMerchant.isNullOrBlank()) {
                    finalMerchant = "Transfer to ${destAccount.name}"
                }
                if (finalNote.isNullOrBlank()) {
                    finalNote = "Transferred to ${destAccount.name}"
                }
            }

            // Update the final transaction with computed base amount
            val finalTransaction = transaction.copy(
                baseAmount = baseAmount,
                merchant = finalMerchant,
                note = finalNote
            )

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
