package com.expensevault.core.data.remote

import com.expensevault.core.model.Web3Chain
import com.expensevault.core.model.Web3TokenConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class OnChainBalance(
    val chain: Web3Chain,
    val symbol: String,
    val balance: BigDecimal,
    val contractAddress: String? = null
)

class Web3RpcService(
    private val httpClient: HttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        val TRACKED_TOKENS = listOf(
            // BSC Tokens
            Web3TokenConfig("USDT", "0x55d398326f99059fF775485246999027B3197955", 18, Web3Chain.BSC),
            Web3TokenConfig("USDC", "0x8AC76a51cc950d9822D68b83fE1Ad97B32Cd580d", 18, Web3Chain.BSC),
            Web3TokenConfig("BTCB", "0x7130d2A12B9BCbFAe4f2634d864A1Ee1Ce3Ead9c", 18, Web3Chain.BSC),
            Web3TokenConfig("ETH", "0x2170Ed0880ac9A755fd29B2688956BD959F933F8", 18, Web3Chain.BSC),
            Web3TokenConfig("CAKE", "0x0E09FaBB73Bd3Ade0a17ECC321fD13a19e81cE82", 18, Web3Chain.BSC),
            Web3TokenConfig("FDUSD", "0xc5f0f7b66764F6ec8C8Dff7BA683102295E16409", 18, Web3Chain.BSC),

            // Ethereum Tokens
            Web3TokenConfig("USDT", "0xdAC17F958D2ee523a2206206994597C13D831ec7", 6, Web3Chain.ETHEREUM),
            Web3TokenConfig("USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", 6, Web3Chain.ETHEREUM),
            Web3TokenConfig("WBTC", "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599", 8, Web3Chain.ETHEREUM),
            Web3TokenConfig("DAI", "0x6B175474E89094C44Da98b954EedeAC495271d0F", 18, Web3Chain.ETHEREUM),

            // Polygon Tokens
            Web3TokenConfig("USDT", "0xc2132D05D31c914a87C6611C10748AEb04B58e8F", 6, Web3Chain.POLYGON),
            Web3TokenConfig("USDC", "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359", 6, Web3Chain.POLYGON)
        )

        fun parseHexBalance(hex: String?, decimals: Int): BigDecimal {
            if (hex.isNullOrBlank() || hex == "0x" || hex == "0x0") return BigDecimal.ZERO
            val cleanHex = hex.removePrefix("0x").trim()
            if (cleanHex.isEmpty()) return BigDecimal.ZERO
            return try {
                val rawBigInt = BigInteger(cleanHex, 16)
                if (rawBigInt == BigInteger.ZERO) {
                    BigDecimal.ZERO
                } else {
                    BigDecimal(rawBigInt).divide(BigDecimal.TEN.pow(decimals), 8, RoundingMode.HALF_UP)
                }
            } catch (_: Exception) {
                BigDecimal.ZERO
            }
        }
    }

    /**
     * Fetch all balances for a wallet address across the specified chains.
     */
    suspend fun getWalletBalances(
        address: String,
        chains: Set<Web3Chain>
    ): List<OnChainBalance> = coroutineScope {
        val cleanAddress = address.trim()
        if (!cleanAddress.startsWith("0x") || cleanAddress.length != 42) {
            return@coroutineScope emptyList()
        }

        val deferredResults = chains.map { chain ->
            async(Dispatchers.IO) {
                fetchBalancesForChain(cleanAddress, chain)
            }
        }

        deferredResults.awaitAll().flatten().filter { it.balance > BigDecimal.ZERO }
    }

    private suspend fun fetchBalancesForChain(
        address: String,
        chain: Web3Chain
    ): List<OnChainBalance> = coroutineScope {
        val results = mutableListOf<OnChainBalance>()

        // 1. Fetch Native Coin Balance
        val nativeDeferred = async(Dispatchers.IO) {
            val hex = callRpc(chain, "eth_getBalance", listOf(JsonPrimitive(address), JsonPrimitive("latest")))
            val balance = parseHexBalance(hex, 18)
            if (balance > BigDecimal.ZERO) {
                OnChainBalance(chain, chain.nativeSymbol, balance)
            } else null
        }

        // 2. Fetch Tracked Token Balances
        val tokensForChain = TRACKED_TOKENS.filter { it.chain == chain }
        val tokenDeferreds = tokensForChain.map { token ->
            async(Dispatchers.IO) {
                val paddedAddress = address.removePrefix("0x").padStart(64, '0')
                val callData = "0x70a08231$paddedAddress"
                val callObj = buildJsonObject {
                    put("to", token.contractAddress)
                    put("data", callData)
                }
                val hex = callRpc(chain, "eth_call", listOf(callObj, JsonPrimitive("latest")))
                val balance = parseHexBalance(hex, token.decimals)
                if (balance > BigDecimal.ZERO) {
                    OnChainBalance(chain, token.symbol, balance, token.contractAddress)
                } else null
            }
        }

        nativeDeferred.await()?.let { results.add(it) }
        tokenDeferreds.awaitAll().filterNotNull().forEach { results.add(it) }

        results
    }

    private suspend fun callRpc(
        chain: Web3Chain,
        method: String,
        params: List<kotlinx.serialization.json.JsonElement>
    ): String? {
        val requestBody = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", JsonArray(params))
            put("id", 1)
        }.toString()

        for (url in chain.rpcUrls) {
            try {
                val responseText = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.bodyAsText()

                val root = json.parseToJsonElement(responseText)
                if (root is kotlinx.serialization.json.JsonObject) {
                    val result = root["result"]
                    if (result is JsonPrimitive && result.isString) {
                        return result.content
                    }
                }
            } catch (_: Exception) {
                // Try fallback RPC in the list
                continue
            }
        }
        return null
    }
}
