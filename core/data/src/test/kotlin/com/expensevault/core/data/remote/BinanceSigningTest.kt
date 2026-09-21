package com.expensevault.core.data.remote

import com.expensevault.core.model.BinanceAssetBalance
import java.math.BigDecimal
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BinanceSigningTest {

    private fun signHmacSha256(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testHmacSha256Determinism() {
        val secret = "my_binance_secret_key_123"
        val query = "timestamp=1600000000000&recvWindow=60000"

        val sig1 = signHmacSha256(query, secret)
        val sig2 = signHmacSha256(query, secret)

        assertEquals(sig1, sig2)
        assertEquals(64, sig1.length, "HMAC-SHA256 hex string should be 64 characters")
    }

    @Test
    fun testBinanceAssetBalanceMath() {
        val asset = BinanceAssetBalance(
            asset = "BTC",
            free = "1.50000000",
            locked = "0.25000000",
            fiatValue = "120000.50"
        )

        assertEquals(BigDecimal("1.75000000"), asset.totalAmount)
        assertEquals(BigDecimal("120000.50"), asset.fiatBigDecimal)
    }

    @Test
    fun testMaskedApiKeyLogic() {
        val key = "vmPUZE6mv9SD5VNHk4HlWFsOr6aKE2zvsw0MuIgwCIPy6utIco14y7Ju91duEh8A"
        val masked = if (key.length > 8) "${key.take(4)}...${key.takeLast(4)}" else "****"

        assertEquals("vmPU...Eh8A", masked)
    }
}
