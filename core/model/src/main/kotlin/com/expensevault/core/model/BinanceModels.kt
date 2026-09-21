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
    val rpcUrls: List<String>,
    val isEvm: Boolean = true
) {
    BITCOIN(
        chainId = 0,
        chainName = "Bitcoin",
        nativeSymbol = "BTC",
        rpcUrls = emptyList(),
        isEvm = false
    ),
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
    ARBITRUM(
        chainId = 42161,
        chainName = "Arbitrum One",
        nativeSymbol = "ETH",
        rpcUrls = listOf(
            "https://arb1.arbitrum.io/rpc",
            "https://arbitrum-one-rpc.publicnode.com",
            "https://arbitrum.llamarpc.com"
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
    ),
    BASE(
        chainId = 8453,
        chainName = "Base",
        nativeSymbol = "ETH",
        rpcUrls = listOf(
            "https://mainnet.base.org",
            "https://base-rpc.publicnode.com"
        )
    ),
    OPTIMISM(
        chainId = 10,
        chainName = "Optimism",
        nativeSymbol = "ETH",
        rpcUrls = listOf(
            "https://mainnet.optimism.io",
            "https://optimism-rpc.publicnode.com"
        )
    ),
    AVALANCHE(
        chainId = 43114,
        chainName = "Avalanche",
        nativeSymbol = "AVAX",
        rpcUrls = listOf(
            "https://api.avax.network/ext/bc/C/rpc",
            "https://avalanche-c-chain-rpc.publicnode.com"
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
    val web3Chains: Set<String> = setOf(
        "BITCOIN",
        "BSC",
        "ETHEREUM",
        "ARBITRUM",
        "POLYGON",
        "BASE",
        "OPTIMISM",
        "AVALANCHE"
    ),
    val lastSyncedAt: Instant? = null,
    val accountId: Long? = null,
    val totalFiatBalance: BigDecimal = BigDecimal.ZERO,
    val assets: List<BinanceAssetBalance> = emptyList(),
    val isSyncing: Boolean = false,
    val lastError: String? = null,
    val enabledWallets: Set<String> = setOf("Spot", "Funding", "Earn", "Futures", "Margin")
)

object Web3AddressUtils {
    fun isBitcoinAddress(addr: String): Boolean {
        val clean = addr.trim()
        if (clean.length !in 26..62) return false
        if (clean.startsWith("bc1", ignoreCase = true)) {
            return clean.matches(Regex("^bc1[a-zA-HJ-NP-Z0-9]{25,59}$", RegexOption.IGNORE_CASE))
        }
        if (clean.startsWith("1") || clean.startsWith("3")) {
            return clean.matches(Regex("^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$"))
        }
        return false
    }

    fun isEvmAddress(addr: String): Boolean {
        val clean = addr.trim()
        return clean.startsWith("0x", ignoreCase = true) && clean.length == 42 && clean.drop(2).all {
            it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
        }
    }

    fun parseAddresses(raw: String?): Pair<String?, String?> {
        if (raw.isNullOrBlank()) return Pair(null, null)
        val tokens = raw.split(";", ",", " ", "\n").map { it.trim() }.filter { it.isNotEmpty() }
        var evm: String? = null
        var btc: String? = null
        for (t in tokens) {
            if (isEvmAddress(t)) {
                evm = t
            } else if (isBitcoinAddress(t)) {
                btc = t
            }
        }
        return Pair(evm, btc)
    }

    fun combineAddresses(evm: String?, btc: String?): String {
        val e = evm?.trim()?.takeIf { it.isNotEmpty() }
        val b = btc?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            e != null && b != null -> "$e;$b"
            e != null -> e
            b != null -> b
            else -> ""
        }
    }
}

