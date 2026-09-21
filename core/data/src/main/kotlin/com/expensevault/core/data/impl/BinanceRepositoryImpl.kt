package com.expensevault.core.data.impl

import com.expensevault.core.data.remote.BinanceApiService
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.BinanceCredentialStore
import com.expensevault.core.domain.repository.BinanceRepository
import com.expensevault.core.domain.repository.ExchangeRateRepository
import com.expensevault.core.model.Account
import com.expensevault.core.model.AccountType
import com.expensevault.core.model.BinanceAssetBalance
import com.expensevault.core.model.BinanceCredentials
import com.expensevault.core.model.BinanceSyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.math.BigDecimal
import java.math.RoundingMode

class BinanceRepositoryImpl(
    private val apiService: BinanceApiService,
    private val credentialStore: BinanceCredentialStore,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) : BinanceRepository {

    private val _syncState = MutableStateFlow(
        BinanceSyncState(
            isLinked = credentialStore.isLinked(),
            apiKeyMasked = credentialStore.getMaskedApiKey()
        )
    )
    override val syncState: StateFlow<BinanceSyncState> = _syncState.asStateFlow()

    override suspend fun saveCredentials(apiKey: String, apiSecret: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialStore.saveCredentials(BinanceCredentials(apiKey, apiSecret))
            _syncState.update {
                it.copy(
                    isLinked = true,
                    apiKeyMasked = credentialStore.getMaskedApiKey(),
                    lastError = null
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(apiKey: String, apiSecret: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.getAccountInfo(apiKey.trim(), apiSecret.trim())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncAccount(baseCurrency: String): Result<Account> = withContext(Dispatchers.IO) {
        val creds = credentialStore.getCredentials()
            ?: return@withContext Result.failure(IllegalStateException("Binance account is not linked. Please provide API keys."))

        _syncState.update { it.copy(isSyncing = true, lastError = null) }

        try {
            // 1. Fetch live account balances from Binance
            val accountDto = apiService.getAccountInfo(creds.apiKey, creds.apiSecret)

            // 2. Filter non-zero assets
            val nonZeroBalances = accountDto.balances.filter {
                val freeBd = it.free.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val lockedBd = it.locked.toBigDecimalOrNull() ?: BigDecimal.ZERO
                freeBd.add(lockedBd) > BigDecimal.ZERO
            }

            // 3. Fetch current market prices in USDT
            val pricesMap = try {
                apiService.getPriceTickers().associate { it.symbol to (it.price.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
            } catch (_: Exception) {
                emptyMap()
            }

            // 4. Resolve exchange rate to target baseCurrency (e.g. INR)
            val fiatRate = if (baseCurrency.equals("USD", ignoreCase = true) || baseCurrency.equals("USDT", ignoreCase = true)) {
                BigDecimal.ONE
            } else {
                val cachedRate = exchangeRateRepository.getLatestRate("USD", baseCurrency)?.rate
                if (cachedRate != null && cachedRate > BigDecimal.ZERO) {
                    cachedRate
                } else {
                    val fetched = exchangeRateRepository.fetchAndCacheRates("USD").getOrNull()
                    fetched?.find { it.targetCurrency.equals(baseCurrency, ignoreCase = true) }?.rate ?: BigDecimal.ONE
                }
            }

            // 5. Calculate fiat value per asset
            val btcUsdtPrice = pricesMap["BTCUSDT"] ?: BigDecimal.ZERO
            val holdingList = mutableListOf<BinanceAssetBalance>()
            var totalFiat = BigDecimal.ZERO

            for (bal in nonZeroBalances) {
                val asset = bal.asset
                val freeBd = bal.free.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val lockedBd = bal.locked.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val totalQty = freeBd.add(lockedBd)

                val usdtPrice = when {
                    asset.equals("USDT", ignoreCase = true) ||
                    asset.equals("USD", ignoreCase = true) ||
                    asset.equals("USDC", ignoreCase = true) -> BigDecimal.ONE

                    pricesMap.containsKey("${asset}USDT") -> pricesMap["${asset}USDT"] ?: BigDecimal.ZERO
                    pricesMap.containsKey("${asset}BUSD") -> pricesMap["${asset}BUSD"] ?: BigDecimal.ZERO
                    pricesMap.containsKey("${asset}BTC") -> (pricesMap["${asset}BTC"] ?: BigDecimal.ZERO).multiply(btcUsdtPrice)
                    else -> BigDecimal.ZERO
                }

                val assetUsdtValue = totalQty.multiply(usdtPrice)
                val assetFiatValue = assetUsdtValue.multiply(fiatRate).setScale(2, RoundingMode.HALF_UP)
                totalFiat = totalFiat.add(assetFiatValue)

                holdingList.add(
                    BinanceAssetBalance(
                        asset = asset,
                        free = bal.free,
                        locked = bal.locked,
                        fiatValue = assetFiatValue.toPlainString()
                    )
                )
            }

            // Sort holdings by largest fiat value first
            holdingList.sortByDescending { it.fiatBigDecimal }

            // 6. Find or create the Binance Account in Kite database
            val existingAccounts = accountRepository.getAccounts().first()
            val existingBinanceAccount = existingAccounts.find { it.type == AccountType.BINANCE }

            val savedAccount = if (existingBinanceAccount != null) {
                val updated = existingBinanceAccount.copy(
                    currentBalance = totalFiat,
                    defaultCurrency = baseCurrency,
                    updatedAt = Clock.System.now()
                )
                accountRepository.updateAccount(updated)
                updated
            } else {
                val newAccount = Account(
                    name = "Binance",
                    type = AccountType.BINANCE,
                    defaultCurrency = baseCurrency,
                    initialBalance = totalFiat,
                    currentBalance = totalFiat,
                    colorHex = "#F0B90B",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                val newId = accountRepository.addAccount(newAccount)
                newAccount.copy(id = newId)
            }

            val now = Clock.System.now()
            _syncState.update {
                it.copy(
                    isLinked = true,
                    apiKeyMasked = credentialStore.getMaskedApiKey(),
                    lastSyncedAt = now,
                    accountId = savedAccount.id,
                    totalFiatBalance = totalFiat,
                    assets = holdingList,
                    isSyncing = false,
                    lastError = null
                )
            }

            Result.success(savedAccount)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to sync Binance account"
            _syncState.update { it.copy(isSyncing = false, lastError = errorMsg) }
            Result.failure(e)
        }
    }

    override suspend fun unlinkAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialStore.clearCredentials()
            _syncState.update {
                BinanceSyncState(
                    isLinked = false,
                    apiKeyMasked = "",
                    lastSyncedAt = null,
                    accountId = null,
                    totalFiatBalance = BigDecimal.ZERO,
                    assets = emptyList(),
                    isSyncing = false,
                    lastError = null
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLinkedAccountId(): Long? {
        val existing = accountRepository.getAccounts().first().find { it.type == AccountType.BINANCE }
        return existing?.id
    }
}
