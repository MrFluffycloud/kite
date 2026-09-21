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
    val fiatValue: String = "0"
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

/**
 * Live sync status and portfolio state for the linked Binance account.
 */
data class BinanceSyncState(
    val isLinked: Boolean = false,
    val apiKeyMasked: String = "",
    val lastSyncedAt: Instant? = null,
    val accountId: Long? = null,
    val totalFiatBalance: BigDecimal = BigDecimal.ZERO,
    val assets: List<BinanceAssetBalance> = emptyList(),
    val isSyncing: Boolean = false,
    val lastError: String? = null
)
