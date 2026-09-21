package com.expensevault.core.data.remote

import com.expensevault.core.model.Web3Chain
import com.expensevault.core.model.Web3TokenConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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
import kotlinx.coroutines.withContext
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
            Web3TokenConfig("BTC", "0x7130d2A12B9BCbFAe4f2634d864A1Ee1Ce3Ead9c", 18, Web3Chain.BSC),
            Web3TokenConfig("ETH", "0x2170Ed0880ac9A755fd29B2688956BD959F933F8", 18, Web3Chain.BSC),
            Web3TokenConfig("USDT", "0x55d398326f99059fF775485246999027B3197955", 18, Web3Chain.BSC),
            Web3TokenConfig("USDC", "0x8AC76a51cc950d9822D68b83fE1Ad97B32Cd580d", 18, Web3Chain.BSC),
            Web3TokenConfig("FDUSD", "0xc5f0f7b66764F6ec8C8Dff7BA683102295E16409", 18, Web3Chain.BSC),
            Web3TokenConfig("CAKE", "0x0E09FaBB73Bd3Ade0a17ECC321fD13a19e81cE82", 18, Web3Chain.BSC),

            // Ethereum Tokens
            Web3TokenConfig("BTC", "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599", 8, Web3Chain.ETHEREUM),
            Web3TokenConfig("USDT", "0xdAC17F958D2ee523a2206206994597C13D831ec7", 6, Web3Chain.ETHEREUM),
            Web3TokenConfig("USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", 6, Web3Chain.ETHEREUM),
            Web3TokenConfig("DAI", "0x6B175474E89094C44Da98b954EedeAC495271d0F", 18, Web3Chain.ETHEREUM),

            // Arbitrum One Tokens
            Web3TokenConfig("BTC", "0x2f2a2543B76A4166549F7aaB2e41Bf0df7650aef", 8, Web3Chain.ARBITRUM),
            Web3TokenConfig("USDT", "0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9", 6, Web3Chain.ARBITRUM),
            Web3TokenConfig("USDC", "0xaf88d065e77c8cC2239327C5EDb3A432268e5831", 6, Web3Chain.ARBITRUM),
            Web3TokenConfig("ARB", "0x912CE59144191C1204E64559FE8253a0e49E6548", 18, Web3Chain.ARBITRUM),

            // Polygon Tokens
            Web3TokenConfig("BTC", "0x1BFD67037B42Cf73acF2047067bd4F2C47D9BfD6", 8, Web3Chain.POLYGON),
            Web3TokenConfig("USDT", "0xc2132D05D31c914a87C6611C10748AEb04B58e8F", 6, Web3Chain.POLYGON),
            Web3TokenConfig("USDC", "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359", 6, Web3Chain.POLYGON),

            // Base Tokens
            Web3TokenConfig("BTC", "0xcbB7C0000ab88B473b1f5aFd9ef808440eed33Bf", 8, Web3Chain.BASE),
            Web3TokenConfig("USDC", "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", 6, Web3Chain.BASE),
            Web3TokenConfig("USDT", "0xfde4C96c8593536E31F229EA8f37b2ADa2699bb2", 6, Web3Chain.BASE),

            // Optimism Tokens
            Web3TokenConfig("BTC", "0x68f180fcCe6836688e9084f035309E29Bf0A2095", 8, Web3Chain.OPTIMISM),
            Web3TokenConfig("USDT", "0x94b008aA00579c1307B0EF2c499aD98a8ce58e58", 6, Web3Chain.OPTIMISM),
            Web3TokenConfig("USDC", "0x0b2C639c533813f4Aa9D7837CAf62653d097Ff85", 6, Web3Chain.OPTIMISM),
            Web3TokenConfig("OP", "0x4200000000000000000000000000000000000042", 18, Web3Chain.OPTIMISM),

            // Avalanche Tokens
            Web3TokenConfig("BTC", "0x152b9d0FdC40C096757F570A51E494bd4b943E50", 8, Web3Chain.AVALANCHE),
            Web3TokenConfig("USDT", "0x9702230A8Ea53601f5cD2dc00fDBc13d4dF4A8c7", 6, Web3Chain.AVALANCHE),
            Web3TokenConfig("USDC", "0xB97EF9Ef8734C71904D8002F8b6Bc66Dd9c48a6E", 6, Web3Chain.AVALANCHE)
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

        fun isBitcoinAddress(addr: String): Boolean = com.expensevault.core.model.Web3AddressUtils.isBitcoinAddress(addr)

        fun isSolanaAddress(addr: String): Boolean = com.expensevault.core.model.Web3AddressUtils.isSolanaAddress(addr)

        fun parseAddresses(raw: String?) = com.expensevault.core.model.Web3AddressUtils.parseAddresses(raw)
    }

    /**
     * Fetch all balances across specified chains.
     * Supports EVM addresses (0x...), native Bitcoin addresses (bc1..., 1..., 3...), and Solana addresses.
     */
    suspend fun getWalletBalances(
        address: String,
        chains: Set<Web3Chain>
    ): List<OnChainBalance> = coroutineScope {
        val addrs = parseAddresses(address)

        val results = mutableListOf<OnChainBalance>()

        // 1. Fetch native Bitcoin if Bitcoin chain enabled and a BTC address is present
        val btcAddress = addrs.btc
        val btcJob = if (btcAddress != null && chains.contains(Web3Chain.BITCOIN)) {
            async(Dispatchers.IO) {
                fetchBitcoinBalance(btcAddress)
            }
        } else null

        // 2. Fetch Solana if Solana chain enabled and a SOL address is present
        val solAddress = addrs.sol
        val solJob = if (solAddress != null && chains.contains(Web3Chain.SOLANA)) {
            async(Dispatchers.IO) {
                fetchSolanaBalances(solAddress)
            }
        } else null

        // 3. Fetch EVM chains if an EVM address is present
        val evmAddress = addrs.evm
        val evmJobs = if (evmAddress != null) {
            val evmChains = chains.filter { it.isEvm }
            evmChains.map { chain ->
                async(Dispatchers.IO) {
                    fetchBalancesForChain(evmAddress, chain)
                }
            }
        } else emptyList()

        btcJob?.await()?.let { results.add(it) }
        solJob?.await()?.let { results.addAll(it) }
        evmJobs.awaitAll().flatten().forEach { results.add(it) }

        results.filter { it.balance > BigDecimal.ZERO }
    }

    private suspend fun fetchBitcoinBalance(btcAddress: String): OnChainBalance? = withContext(Dispatchers.IO) {
        val cleanAddr = btcAddress.trim()
        if (!isBitcoinAddress(cleanAddr)) return@withContext null

        // Primary: mempool.space API
        try {
            val responseText = httpClient.get("https://mempool.space/api/address/$cleanAddr").bodyAsText()
            val root = json.parseToJsonElement(responseText)
            if (root is kotlinx.serialization.json.JsonObject) {
                val chainStats = root["chain_stats"] as? kotlinx.serialization.json.JsonObject
                val mempoolStats = root["mempool_stats"] as? kotlinx.serialization.json.JsonObject

                val fundedChain = chainStats?.get("funded_txo_sum")?.let { it as? JsonPrimitive }?.content?.toLongOrNull() ?: 0L
                val spentChain = chainStats?.get("spent_txo_sum")?.let { it as? JsonPrimitive }?.content?.toLongOrNull() ?: 0L
                val fundedMempool = mempoolStats?.get("funded_txo_sum")?.let { it as? JsonPrimitive }?.content?.toLongOrNull() ?: 0L
                val spentMempool = mempoolStats?.get("spent_txo_sum")?.let { it as? JsonPrimitive }?.content?.toLongOrNull() ?: 0L

                val netSats = (fundedChain - spentChain) + (fundedMempool - spentMempool)
                if (netSats > 0) {
                    val btcBalance = BigDecimal(netSats).divide(BigDecimal(100_000_000), 8, RoundingMode.HALF_UP)
                    return@withContext OnChainBalance(Web3Chain.BITCOIN, "BTC", btcBalance)
                }
            }
        } catch (_: Exception) {}

        // Fallback: blockchain.info API
        try {
            val responseText = httpClient.get("https://blockchain.info/q/addressbalance/$cleanAddr").bodyAsText()
            val sats = responseText.trim().toLongOrNull()
            if (sats != null && sats > 0) {
                val btcBalance = BigDecimal(sats).divide(BigDecimal(100_000_000), 8, RoundingMode.HALF_UP)
                return@withContext OnChainBalance(Web3Chain.BITCOIN, "BTC", btcBalance)
            }
        } catch (_: Exception) {}

        null
    }

    private suspend fun fetchSolanaBalances(solAddress: String): List<OnChainBalance> = withContext(Dispatchers.IO) {
        val cleanAddr = solAddress.trim()
        if (!isSolanaAddress(cleanAddr)) return@withContext emptyList()
        val results = mutableListOf<OnChainBalance>()

        // 1. Native SOL Balance via getBalance
        val balanceReq = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", "getBalance")
            put("params", JsonArray(listOf(JsonPrimitive(cleanAddr))))
            put("id", 1)
        }.toString()

        for (url in Web3Chain.SOLANA.rpcUrls) {
            try {
                val responseText = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(balanceReq)
                }.bodyAsText()
                val root = json.parseToJsonElement(responseText) as? kotlinx.serialization.json.JsonObject
                val resultObj = root?.get("result") as? kotlinx.serialization.json.JsonObject
                val lamports = resultObj?.get("value")?.let { it as? JsonPrimitive }?.content?.toLongOrNull()
                if (lamports != null && lamports > 0L) {
                    val solBalance = BigDecimal(lamports).divide(BigDecimal(1_000_000_000), 9, RoundingMode.HALF_UP)
                    results.add(OnChainBalance(Web3Chain.SOLANA, "SOL", solBalance))
                    break
                }
            } catch (_: Exception) {}
        }

        // 2. SPL Tokens via getTokenAccountsByOwner
        val tokensReq = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", "getTokenAccountsByOwner")
            put("params", JsonArray(listOf(
                JsonPrimitive(cleanAddr),
                buildJsonObject {
                    put("programId", "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
                },
                buildJsonObject {
                    put("encoding", "jsonParsed")
                }
            )))
            put("id", 2)
        }.toString()

        for (url in Web3Chain.SOLANA.rpcUrls) {
            try {
                val responseText = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(tokensReq)
                }.bodyAsText()
                val root = json.parseToJsonElement(responseText) as? kotlinx.serialization.json.JsonObject
                val resultObj = root?.get("result") as? kotlinx.serialization.json.JsonObject
                val accountsArray = resultObj?.get("value") as? JsonArray ?: continue

                for (acc in accountsArray) {
                    try {
                        val accObj = acc as? kotlinx.serialization.json.JsonObject ?: continue
                        val dataObj = accObj["account"]?.let { it as? kotlinx.serialization.json.JsonObject }?.get("data") as? kotlinx.serialization.json.JsonObject
                        val parsedObj = dataObj?.get("parsed") as? kotlinx.serialization.json.JsonObject
                        val infoObj = parsedObj?.get("info") as? kotlinx.serialization.json.JsonObject
                        val mint = infoObj?.get("mint")?.let { (it as? JsonPrimitive)?.content }
                        val tokenAmount = infoObj?.get("tokenAmount") as? kotlinx.serialization.json.JsonObject
                        val rawAmount = tokenAmount?.get("amount")?.let { (it as? JsonPrimitive)?.content }
                        val decimals = tokenAmount?.get("decimals")?.let { (it as? JsonPrimitive)?.content?.toIntOrNull() } ?: 6

                        if (mint != null && rawAmount != null) {
                            val rawBigInt = BigInteger(rawAmount)
                            if (rawBigInt > BigInteger.ZERO) {
                                val tokenBalance = BigDecimal(rawBigInt).divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.HALF_UP)
                                val symbol = when (mint) {
                                    "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v" -> "USDC"
                                    "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB" -> "USDT"
                                    else -> null
                                }
                                if (symbol != null) {
                                    results.add(OnChainBalance(Web3Chain.SOLANA, symbol, tokenBalance, mint))
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                break
            } catch (_: Exception) {}
        }

        results
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
