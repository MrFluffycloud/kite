package com.expensevault.core.data.impl

import com.expensevault.core.data.remote.BinanceApiService
import com.expensevault.core.data.remote.Web3RpcService
import com.expensevault.core.domain.repository.AccountRepository
import com.expensevault.core.domain.repository.BinanceCredentialStore
import com.expensevault.core.domain.repository.BinanceRepository
import com.expensevault.core.domain.repository.ExchangeRateRepository
import com.expensevault.core.model.Account
import com.expensevault.core.model.AccountType
import com.expensevault.core.model.BinanceAssetBalance
import com.expensevault.core.model.BinanceCredentials
import com.expensevault.core.model.BinanceIntegrationType
import com.expensevault.core.model.BinanceSyncState
import com.expensevault.core.model.Web3Chain
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
    private val exchangeRateRepository: ExchangeRateRepository,
    private val web3RpcService: Web3RpcService
) : BinanceRepository {

    private val _syncState = MutableStateFlow(
        BinanceSyncState(
            isLinked = credentialStore.isLinked(),
            integrationType = credentialStore.getIntegrationType(),
            apiKeyMasked = credentialStore.getMaskedApiKey(),
            web3Address = credentialStore.getWeb3Address(),
            web3Chains = credentialStore.getWeb3Chains(),
            enabledWallets = credentialStore.getEnabledWallets()
        )
    )
    override val syncState: StateFlow<BinanceSyncState> = _syncState.asStateFlow()

    override suspend fun saveCredentials(apiKey: String, apiSecret: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialStore.saveCredentials(BinanceCredentials(apiKey, apiSecret))
            credentialStore.saveIntegrationType(BinanceIntegrationType.EXCHANGE)
            _syncState.update {
                it.copy(
                    isLinked = true,
                    integrationType = BinanceIntegrationType.EXCHANGE,
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

    override suspend fun saveWeb3Config(
        address: String,
        chains: Set<String>,
        baseCurrency: String
    ): Result<Account> = withContext(Dispatchers.IO) {
        try {
            credentialStore.saveWeb3Address(address)
            credentialStore.saveWeb3Chains(chains)
            credentialStore.saveIntegrationType(BinanceIntegrationType.WEB3_WALLET)
            _syncState.update {
                it.copy(
                    isLinked = true,
                    integrationType = BinanceIntegrationType.WEB3_WALLET,
                    web3Address = address,
                    web3Chains = chains,
                    apiKeyMasked = credentialStore.getMaskedApiKey(),
                    lastError = null
                )
            }
            syncAccount(baseCurrency)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun switchIntegrationType(type: BinanceIntegrationType): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialStore.saveIntegrationType(type)
            _syncState.update {
                it.copy(
                    integrationType = type,
                    isLinked = credentialStore.isLinked(),
                    apiKeyMasked = credentialStore.getMaskedApiKey(),
                    web3Address = credentialStore.getWeb3Address(),
                    web3Chains = credentialStore.getWeb3Chains()
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
            if (credentialStore.isLinked() && credentialStore.getIntegrationType() == BinanceIntegrationType.EXCHANGE) {
                syncAccount(baseCurrency)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncAccount(baseCurrency: String): Result<Account> = withContext(Dispatchers.IO) {
        _syncState.update { it.copy(isSyncing = true, lastError = null) }
        try {
            when (credentialStore.getIntegrationType()) {
                BinanceIntegrationType.WEB3_WALLET -> syncWeb3Wallet(baseCurrency)
                BinanceIntegrationType.EXCHANGE -> syncExchangeAccount(baseCurrency)
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to sync Binance account"
            _syncState.update { it.copy(isSyncing = false, lastError = errorMsg) }
            Result.failure(e)
        }
    }

    private suspend fun syncWeb3Wallet(baseCurrency: String): Result<Account> {
        val address = credentialStore.getWeb3Address()
            ?: return Result.failure(IllegalStateException("No Binance Web3 wallet address configured. Please enter your address."))

        val enabledChainsStrings = credentialStore.getWeb3Chains()
        val enabledChains = enabledChainsStrings.mapNotNull { Web3Chain.fromName(it) }.toSet().ifEmpty { setOf(Web3Chain.BSC) }

        val onChainBalances = web3RpcService.getWalletBalances(address, enabledChains)

        val pricesMap = try {
            apiService.getPriceTickers().associate { it.symbol to BigDecimal(it.price) }
        } catch (_: Exception) {
            emptyMap()
        }

        val fiatRate = resolveFiatRate(baseCurrency)
        val btcUsdtPrice = pricesMap["BTCUSDT"] ?: BigDecimal.ZERO

        val holdingList = mutableListOf<BinanceAssetBalance>()
        var totalFiat = BigDecimal.ZERO

        for (item in onChainBalances) {
            val lookupSymbol = when (item.symbol) {
                "BTC", "BTCB", "WBTC", "cbBTC", "BTC.b" -> "BTC"
                "POL" -> "MATIC"
                "AVAX" -> "AVAX"
                "ARB" -> "ARB"
                "OP" -> "OP"
                else -> item.symbol
            }

            val usdtPrice = when {
                lookupSymbol.equals("USDT", ignoreCase = true) ||
                lookupSymbol.equals("USD", ignoreCase = true) ||
                lookupSymbol.equals("USDC", ignoreCase = true) ||
                lookupSymbol.equals("FDUSD", ignoreCase = true) ||
                lookupSymbol.equals("DAI", ignoreCase = true) -> BigDecimal.ONE

                pricesMap.containsKey("${lookupSymbol}USDT") -> pricesMap["${lookupSymbol}USDT"] ?: BigDecimal.ZERO
                pricesMap.containsKey("${lookupSymbol}BUSD") -> pricesMap["${lookupSymbol}BUSD"] ?: BigDecimal.ZERO
                pricesMap.containsKey("${lookupSymbol}BTC") -> (pricesMap["${lookupSymbol}BTC"] ?: BigDecimal.ZERO).multiply(btcUsdtPrice)
                else -> BigDecimal.ZERO
            }

            val assetUsdtValue = item.balance.multiply(usdtPrice)
            val assetFiatValue = assetUsdtValue.multiply(fiatRate).setScale(2, RoundingMode.HALF_UP)
            totalFiat = totalFiat.add(assetFiatValue)

            val displayAsset = if (lookupSymbol == "BTC") "BTC" else item.symbol

            holdingList.add(
                BinanceAssetBalance(
                    asset = displayAsset,
                    free = item.balance.stripTrailingZeros().toPlainString(),
                    locked = "0",
                    fiatValue = assetFiatValue.toPlainString(),
                    walletName = item.chain.chainName
                )
            )
        }

        holdingList.sortByDescending { it.fiatBigDecimal }

        val existingAccounts = accountRepository.getAccounts().first()
        val existingBinanceAccount = existingAccounts.find { it.type == AccountType.BINANCE }
        val maskedAddr = credentialStore.getMaskedApiKey()

        val savedAccount = if (existingBinanceAccount != null) {
            val updated = existingBinanceAccount.copy(
                name = "Binance Web3 Wallet",
                currentBalance = totalFiat,
                defaultCurrency = baseCurrency,
                updatedAt = Clock.System.now()
            )
            accountRepository.updateAccount(updated)
            updated
        } else {
            val newAccount = Account(
                name = "Binance Web3 Wallet",
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
                integrationType = BinanceIntegrationType.WEB3_WALLET,
                web3Address = address,
                web3Chains = enabledChainsStrings,
                apiKeyMasked = maskedAddr,
                lastSyncedAt = now,
                accountId = savedAccount.id,
                totalFiatBalance = totalFiat,
                assets = holdingList,
                isSyncing = false,
                lastError = null
            )
        }

        return Result.success(savedAccount)
    }

    private suspend fun syncExchangeAccount(baseCurrency: String): Result<Account> {
        val creds = credentialStore.getCredentials()
            ?: return Result.failure(IllegalStateException("Binance Exchange is not linked. Please provide API keys."))

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
            val walletNamesSet = mutableSetOf<String>()

            for (item in items) {
                totalFree = totalFree.add(BigDecimal(item.free).stripTrailingZeros())
                totalLocked = totalLocked.add(BigDecimal(item.locked).stripTrailingZeros())
                if (item.walletName.isNotBlank()) {
                    walletNamesSet.add(item.walletName)
                }
            }

            if (totalFree > BigDecimal.ZERO || totalLocked > BigDecimal.ZERO) {
                aggregatedList.add(
                    AggregatedAsset(
                        asset = asset,
                        free = totalFree,
                        locked = totalLocked,
                        walletNames = walletNamesSet.joinToString(", ")
                    )
                )
            }
        }

        // 4. Fetch price tickers
        val pricesMap = try {
            apiService.getPriceTickers().associate { it.symbol to BigDecimal(it.price) }
        } catch (_: Exception) {
            emptyMap()
        }

        // 5. Resolve exchange rate to target baseCurrency
        val fiatRate = resolveFiatRate(baseCurrency)
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

        holdingList.sortByDescending { it.fiatBigDecimal }

        val existingAccounts = accountRepository.getAccounts().first()
        val existingBinanceAccount = existingAccounts.find { it.type == AccountType.BINANCE }

        val savedAccount = if (existingBinanceAccount != null) {
            val updated = existingBinanceAccount.copy(
                name = "Binance Exchange",
                currentBalance = totalFiat,
                defaultCurrency = baseCurrency,
                updatedAt = Clock.System.now()
            )
            accountRepository.updateAccount(updated)
            updated
        } else {
            val newAccount = Account(
                name = "Binance Exchange",
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
                integrationType = BinanceIntegrationType.EXCHANGE,
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

        return Result.success(savedAccount)
    }

    private suspend fun resolveFiatRate(baseCurrency: String): BigDecimal {
        if (baseCurrency.equals("USD", ignoreCase = true) || baseCurrency.equals("USDT", ignoreCase = true)) {
            return BigDecimal.ONE
        }
        val cachedRate = exchangeRateRepository.getLatestRate("USD", baseCurrency)?.rate
        return if (cachedRate != null && cachedRate > BigDecimal.ZERO) {
            cachedRate
        } else {
            val fetched = exchangeRateRepository.fetchAndCacheRates("USD").getOrNull()
            fetched?.find { it.targetCurrency.equals(baseCurrency, ignoreCase = true) }?.rate ?: BigDecimal.ONE
        }
    }

    override suspend fun unlinkAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialStore.clearCredentials()
            _syncState.update {
                BinanceSyncState(
                    isLinked = false,
                    integrationType = credentialStore.getIntegrationType(),
                    apiKeyMasked = "",
                    web3Address = null,
                    web3Chains = credentialStore.getWeb3Chains(),
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
