package com.expensevault.core.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Serializable
data class BinanceTimeResponse(
    val serverTime: Long
)

@Serializable
data class BinanceBalanceDto(
    val asset: String,
    val free: String,
    val locked: String
)

@Serializable
data class BinanceAccountDto(
    val canTrade: Boolean? = null,
    val canWithdraw: Boolean? = null,
    val canDeposit: Boolean? = null,
    val updateTime: Long? = null,
    val accountType: String? = null,
    val balances: List<BinanceBalanceDto> = emptyList()
)

@Serializable
data class BinancePriceTickerDto(
    val symbol: String,
    val price: String
)

@Serializable
data class BinanceErrorDto(
    val code: Int = 0,
    val msg: String = ""
)

@Serializable
data class BinanceWalletAssetBalanceDto(
    val asset: String,
    val free: String = "0",
    val locked: String = "0",
    val freeze: String = "0",
    val withdrawing: String = "0",
    val btcValuation: String = "0"
)

@Serializable
data class BinanceWalletBalanceDto(
    val activate: Boolean = true,
    val balance: String = "0",
    val walletName: String,
    val assetBalances: List<BinanceWalletAssetBalanceDto> = emptyList()
)

@Serializable
data class BinanceFundingAssetDto(
    val asset: String,
    val free: String = "0",
    val locked: String = "0",
    val freeze: String = "0",
    val withdrawing: String = "0",
    val btcValuation: String = "0"
)

class BinanceApiService(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://api.binance.com"
        private const val RECV_WINDOW = 60000L
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Computes the HMAC-SHA256 signature for Binance query strings.
     */
    private fun sign(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Gets Binance server time to calibrate client timestamp and avoid clock skew.
     */
    suspend fun getServerTime(): Long {
        return try {
            val response = httpClient.get("$BASE_URL/api/v3/time")
            if (response.status.isSuccess()) {
                val timeDto: BinanceTimeResponse = response.body()
                timeDto.serverTime
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    /**
     * Fetches current spot account details and balances.
     */
    suspend fun getAccountInfo(apiKey: String, apiSecret: String): BinanceAccountDto {
        val serverTime = getServerTime()
        val queryString = "timestamp=$serverTime&recvWindow=$RECV_WINDOW"
        val signature = sign(queryString, apiSecret)
        val fullUrl = "$BASE_URL/api/v3/account?$queryString&signature=$signature"

        val response = httpClient.get(fullUrl) {
            header("X-MBX-APIKEY", apiKey)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            val errorText = response.bodyAsText()
            val parsedError = try {
                jsonParser.decodeFromString<BinanceErrorDto>(errorText)
            } catch (_: Exception) {
                null
            }
            val msg = parsedError?.msg?.takeIf { it.isNotBlank() } ?: "HTTP ${response.status.value}: $errorText"
            throw IllegalStateException("Binance Error: $msg")
        }
    }

    /**
     * Queries user wallet balances across all Binance wallets (Spot, Funding, Cross Margin, Earn, Futures, etc.).
     */
    suspend fun getWalletBalances(apiKey: String, apiSecret: String): List<BinanceWalletBalanceDto> {
        val serverTime = getServerTime()
        val queryString = "needBalanceDetail=true&timestamp=$serverTime&recvWindow=$RECV_WINDOW"
        val signature = sign(queryString, apiSecret)
        val fullUrl = "$BASE_URL/sapi/v1/asset/wallet/balance?$queryString&signature=$signature"

        val response = httpClient.get(fullUrl) {
            header("X-MBX-APIKEY", apiKey)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            val errorText = response.bodyAsText()
            val parsedError = try {
                jsonParser.decodeFromString<BinanceErrorDto>(errorText)
            } catch (_: Exception) {
                null
            }
            val msg = parsedError?.msg?.takeIf { it.isNotBlank() } ?: "HTTP ${response.status.value}: $errorText"
            throw IllegalStateException("Binance Error: $msg")
        }
    }

    /**
     * Queries assets held in the Funding wallet (Binance Pay, P2P, Cards, Gift Cards).
     */
    suspend fun getFundingAssets(apiKey: String, apiSecret: String): List<BinanceFundingAssetDto> {
        val serverTime = getServerTime()
        val queryString = "timestamp=$serverTime&recvWindow=$RECV_WINDOW"
        val signature = sign(queryString, apiSecret)
        val fullUrl = "$BASE_URL/sapi/v1/asset/get-funding-asset?$queryString&signature=$signature"

        val response = httpClient.post(fullUrl) {
            header("X-MBX-APIKEY", apiKey)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            val errorText = response.bodyAsText()
            val parsedError = try {
                jsonParser.decodeFromString<BinanceErrorDto>(errorText)
            } catch (_: Exception) {
                null
            }
            val msg = parsedError?.msg?.takeIf { it.isNotBlank() } ?: "HTTP ${response.status.value}: $errorText"
            throw IllegalStateException("Binance Error: $msg")
        }
    }

    /**
     * Fetches real-time price tickers in USDT for portfolio valuation.
     */
    suspend fun getPriceTickers(): List<BinancePriceTickerDto> {
        val response = httpClient.get("$BASE_URL/api/v3/ticker/price")
        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw IllegalStateException("Failed to fetch Binance market prices")
        }
    }
}
