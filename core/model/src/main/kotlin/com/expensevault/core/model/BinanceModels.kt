package com.expensevault.core.model

import java.math.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Credentials for Binance Read-Only API access.
 */
data class BinanceCredentials(
    val apiKey: String,
    val apiSecret: String
)

/**
 * Individual asset holding in Binance.
 */
@Serializable
data class BinanceAssetBalance(
    val asset: String,
    val free: String,
    val locked: String,
    val fiatValue: String = "0",
    val walletName: String? = null
) {
    val totalAmount: BigDecimal
        get() = try {
            BigDecimal(free).add(BigDecimal(locked))
        } catch (_: Exception) {
            BigDecimal.ZERO
        }

    val fiatBigDecimal: BigDecimal
        get() = try {
            BigDecimal(fiatValue)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
}

enum class BinanceIntegrationType {
    WEB3_WALLET,
    EXCHANGE
}

enum class Web3Chain(
    val chainId: Int,
    val chainName: String,
    val nativeSymbol: String,
    val rpcUrls: List<String>
) {
    BSC(
        chainId = 56,
        chainName = "BNB Smart Chain",
        nativeSymbol = "BNB",
        rpcUrls = listOf(
            "https://bsc-dataseed.binance.org",
            "https://bsc-rpc.publicnode.com",
            "https://binance.llamarpc.com"
        )
    ),
    ETHEREUM(
        chainId = 1,
        chainName = "Ethereum",
        nativeSymbol = "ETH",
        rpcUrls = listOf(
            "https://ethereum-rpc.publicnode.com",
            "https://cloudflare-eth.com",
            "https://eth.llamarpc.com"
        )
    ),
    POLYGON(
        chainId = 137,
        chainName = "Polygon",
        nativeSymbol = "POL",
        rpcUrls = listOf(
            "https://polygon-bor-rpc.publicnode.com",
            "https://polygon-rpc.com"
        )
    );

    companion object {
        fun fromName(name: String): Web3Chain? = entries.firstOrNull { 
            it.name.equals(name, ignoreCase = true) || it.chainName.equals(name, ignoreCase = true) 
        }
    }
}

data class Web3TokenConfig(
    val symbol: String,
    val contractAddress: String,
    val decimals: Int,
    val chain: Web3Chain
)

/**
 * Live sync status and portfolio state for the linked Binance account.
 */
data class BinanceSyncState(
    val isLinked: Boolean = false,
    val integrationType: BinanceIntegrationType = BinanceIntegrationType.WEB3_WALLET,
    val apiKeyMasked: String = "",
    val web3Address: String? = null,
    val web3Chains: Set<String> = setOf("BSC"),
    val lastSyncedAt: Instant? = null,
    val accountId: Long? = null,
    val totalFiatBalance: BigDecimal = BigDecimal.ZERO,
    val assets: List<BinanceAssetBalance> = emptyList(),
    val isSyncing: Boolean = false,
    val lastError: String? = null,
    val enabledWallets: Set<String> = setOf("Spot", "Funding", "Earn", "Futures", "Margin")
)
