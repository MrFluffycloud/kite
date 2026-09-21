package com.expensevault.core.domain.repository

import com.expensevault.core.model.Account
import com.expensevault.core.model.BinanceSyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository coordinating Binance account synchronization, balance calculations,
 * and integration with Kite accounts.
 */
interface BinanceRepository {
    val syncState: StateFlow<BinanceSyncState>
    suspend fun saveCredentials(apiKey: String, apiSecret: String): Result<Unit>
    suspend fun saveWeb3Config(address: String, chains: Set<String>, baseCurrency: String): Result<Account>
    suspend fun switchIntegrationType(type: com.expensevault.core.model.BinanceIntegrationType): Result<Unit>
    suspend fun testConnection(apiKey: String, apiSecret: String): Result<Unit>
    suspend fun syncAccount(baseCurrency: String): Result<Account>
    suspend fun updateEnabledWallets(wallets: Set<String>, baseCurrency: String): Result<Unit>
    suspend fun unlinkAccount(): Result<Unit>
    suspend fun getLinkedAccountId(): Long?
}
