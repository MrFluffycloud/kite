package com.expensevault.core.data.impl

import com.expensevault.core.database.dao.AccountDao
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class AccountRepositoryImpl(
    private val accountDao: AccountDao
) : AccountRepository {
    override fun getAccounts(): Flow<List<Account>> =
        accountDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getActiveAccounts(): Flow<List<Account>> =
        accountDao.getActiveAccounts().map { list -> list.map { it.toDomain() } }

    override suspend fun getAccountById(id: Long): Account? =
        accountDao.getById(id)?.toDomain()

    override suspend fun addAccount(account: Account): Long =
        accountDao.insert(account.toEntity())

    override suspend fun updateAccount(account: Account) {
        accountDao.update(account.toEntity())
    }

    override suspend fun deleteAccount(id: Long) {
        accountDao.deleteById(id)
    }

    override suspend fun updateBalance(accountId: Long, newBalance: BigDecimal) {
        accountDao.setBalance(accountId, newBalance)
    }
}
