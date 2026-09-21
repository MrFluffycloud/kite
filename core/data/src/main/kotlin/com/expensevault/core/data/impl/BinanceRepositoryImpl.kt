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

private data class RawWalletAsset(
    val asset: String,
    val free: String,
    val locked: String,
    val walletName: String
)

private data class AggregatedAsset(
    val asset: String,
    val free: BigDecimal,
    val locked: BigDecimal,
    val walletNames: String
)

class BinanceRepositoryImpl(
    private val apiService: BinanceApiService,
    private val credentialStore: BinanceCredentialStore,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) : BinanceRepository {

    private val _syncState = MutableStateFlow(
        BinanceSyncState(
            isLinked = credentialStore.isLinked(),
            apiKeyMasked = credentialStore.getMaskedApiKey(),
            enabledWallets = credentialStore.getEnabledWallets()
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
                    enabledWallets = credentialStore.getEnabledWallets(),
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

    override suspend fun updateEnabledWallets(wallets: Set<String>, baseCurrency: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialStore.saveEnabledWallets(wallets)
            _syncState.update { it.copy(enabledWallets = wallets) }
            if (credentialStore.isLinked()) {
                syncAccount(baseCurrency)
            }
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
            val enabledWallets = credentialStore.getEnabledWallets()
            val rawAssets = mutableListOf<RawWalletAsset>()
            var usedWalletEndpoint = false

            // 1. Try Binance User Wallet Balance endpoint (/sapi/v1/asset/wallet/balance)
            try {
                val walletBalances = apiService.getWalletBalances(creds.apiKey, creds.apiSecret)
                if (walletBalances.isNotEmpty()) {
                    usedWalletEndpoint = true
                    for (wallet in walletBalances) {
                        val isEnabled = enabledWallets.any { enabled ->
                            wallet.walletName.contains(enabled, ignoreCase = true) ||
                            enabled.contains(wallet.walletName, ignoreCase = true)
                        }
                        if (isEnabled) {
                            for (ab in wallet.assetBalances) {
                                rawAssets.add(
                                    RawWalletAsset(
                                        asset = ab.asset,
                                        free = ab.free,
                                        locked = ab.locked,
                                        walletName = wallet.walletName
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                usedWalletEndpoint = false
            }

            // 2. Fallback to Spot account + Funding wallet if wallet endpoint didn't provide balances
            if (!usedWalletEndpoint) {
                val isSpotEnabled = enabledWallets.any { it.equals("Spot", ignoreCase = true) }
                val isFundingEnabled = enabledWallets.any { it.equals("Funding", ignoreCase = true) }

                if (isSpotEnabled) {
                    try {
                        val spotAccount = apiService.getAccountInfo(creds.apiKey, creds.apiSecret)
                        for (bal in spotAccount.balances) {
                            rawAssets.add(
                                RawWalletAsset(
                                    asset = bal.asset,
                                    free = bal.free,
                                    locked = bal.locked,
                                    walletName = "Spot"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        if (rawAssets.isEmpty() && !isFundingEnabled) throw e
                    }
                }

                if (isFundingEnabled) {
                    try {
                        val fundingAssets = apiService.getFundingAssets(creds.apiKey, creds.apiSecret)
                        for (fa in fundingAssets) {
                            rawAssets.add(
                                RawWalletAsset(
                                    asset = fa.asset,
                                    free = fa.free,
                                    locked = fa.locked,
                                    walletName = "Funding"
                                )
                            )
                        }
                    } catch (_: Exception) {
                        // Keep any spot assets if funding call failed
                    }
                }
            }

            // 3. Aggregate balances across all enabled wallets by asset symbol
            val grouped = rawAssets.groupBy { it.asset.uppercase() }
            val aggregatedList = mutableListOf<AggregatedAsset>()

            for ((asset, items) in grouped) {
                var totalFree = BigDecimal.ZERO
                var totalLocked = BigDecimal.ZERO
                val distinctWallets = items.map { it.walletName }.distinct().joinToString(", ")
                for (item in items) {
                    val f = item.free.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val l = item.locked.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    totalFree = totalFree.add(f)
                    totalLocked = totalLocked.add(l)
                }
                val totalQty = totalFree.add(totalLocked)
                if (totalQty > BigDecimal.ZERO) {
                    aggregatedList.add(AggregatedAsset(asset, totalFree, totalLocked, distinctWallets))
                }
            }

            // 4. Fetch current market prices in USDT
            val pricesMap = try {
                apiService.getPriceTickers().associate { it.symbol to (it.price.toBigDecimalOrNull() ?: BigDecimal.ZERO) }
            } catch (_: Exception) {
                emptyMap()
            }

            // 5. Resolve exchange rate to target baseCurrency (e.g. INR)
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

            // 6. Calculate fiat value per asset
            val btcUsdtPrice = pricesMap["BTCUSDT"] ?: BigDecimal.ZERO
            val holdingList = mutableListOf<BinanceAssetBalance>()
            var totalFiat = BigDecimal.ZERO

            for (item in aggregatedList) {
                val asset = item.asset
                val totalQty = item.free.add(item.locked)

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
                        free = item.free.toPlainString(),
                        locked = item.locked.toPlainString(),
                        fiatValue = assetFiatValue.toPlainString(),
                        walletName = item.walletNames
                    )
                )
            }

            // Sort holdings by largest fiat value first
            holdingList.sortByDescending { it.fiatBigDecimal }

            // 7. Find or create the Binance Account in Kite database
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
                    enabledWallets = enabledWallets,
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
                    lastError = null,
                    enabledWallets = credentialStore.getEnabledWallets()
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
