package com.expensevault.core.domain.repository

import com.expensevault.core.model.Account
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    fun getActiveAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun addAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: Long)
    suspend fun updateBalance(accountId: Long, newBalance: BigDecimal)
}
