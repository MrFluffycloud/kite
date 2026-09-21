package com.expensevault.core.data.remote

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class Web3RpcParsingTest {

    @Test
    fun testParseHexBalance18Decimals() {
        // 1.5 BNB = 1500000000000000000 wei = 0x14d1120d7b160000
        val hex = "0x14d1120d7b160000"
        val balance = Web3RpcService.parseHexBalance(hex, 18)
        assertEquals(BigDecimal("1.50000000"), balance)
    }

    @Test
    fun testParseHexBalance6Decimals() {
        // 500 USDT (6 decimals) = 500000000 = 0x1dcd6500
        val hex = "0x1dcd6500"
        val balance = Web3RpcService.parseHexBalance(hex, 6)
        assertEquals(BigDecimal("500.00000000"), balance)
    }

    @Test
    fun testParseHexBalanceZeroAndNull() {
        assertEquals(BigDecimal.ZERO, Web3RpcService.parseHexBalance(null, 18))
        assertEquals(BigDecimal.ZERO, Web3RpcService.parseHexBalance("", 18))
        assertEquals(BigDecimal.ZERO, Web3RpcService.parseHexBalance("0x", 18))
        assertEquals(BigDecimal.ZERO, Web3RpcService.parseHexBalance("0x0", 18))
    }

    @Test
    fun testPaddedAddressEncoding() {
        val address = "0x8894e0a0c962cb723c1976a4421c95949be2d4e3"
        val clean = address.removePrefix("0x").padStart(64, '0')
        assertEquals(64, clean.length)
        assertEquals("0000000000000000000000008894e0a0c962cb723c1976a4421c95949be2d4e3", clean)
    }

    @Test
    fun testBitcoinAddressValidation() {
        val utils = com.expensevault.core.model.Web3AddressUtils
        // Legacy
        assertEquals(true, utils.isBitcoinAddress("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"))
        // SegWit
        assertEquals(true, utils.isBitcoinAddress("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"))
        // Taproot
        assertEquals(true, utils.isBitcoinAddress("bc1p5d7rjq7g6rdk2yhzks9s2uma66dn3x5aftyxnut7sp4aphmtqdgq93f6pq"))
        // Nested SegWit
        assertEquals(true, utils.isBitcoinAddress("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"))
        // Invalid
        assertEquals(false, utils.isBitcoinAddress("0x8894e0a0c962cb723c1976a4421c95949be2d4e3"))
        assertEquals(false, utils.isBitcoinAddress("invalid_btc_address"))
        assertEquals(false, utils.isBitcoinAddress(""))
    }

    @Test
    fun testEvmAddressValidation() {
        val utils = com.expensevault.core.model.Web3AddressUtils
        assertEquals(true, utils.isEvmAddress("0x8894e0a0c962cb723c1976a4421c95949be2d4e3"))
        assertEquals(false, utils.isEvmAddress("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"))
        assertEquals(false, utils.isEvmAddress("0x123"))
    }

    @Test
    fun testSolanaAddressValidation() {
        val utils = com.expensevault.core.model.Web3AddressUtils
        // Valid Solana addresses
        assertEquals(true, utils.isSolanaAddress("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"))
        assertEquals(true, utils.isSolanaAddress("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"))
        assertEquals(true, utils.isSolanaAddress("So11111111111111111111111111111111111111112"))
        // Invalid (contains 0, O, I, l which are invalid Base58, or wrong lengths)
        assertEquals(false, utils.isSolanaAddress("0x8894e0a0c962cb723c1976a4421c95949be2d4e3"))
        assertEquals(false, utils.isSolanaAddress("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"))
        assertEquals(false, utils.isSolanaAddress("short"))
        assertEquals(false, utils.isSolanaAddress(""))
    }

    @Test
    fun testCombinedAddressParsing() {
        val utils = com.expensevault.core.model.Web3AddressUtils
        val evm = "0x8894e0a0c962cb723c1976a4421c95949be2d4e3"
        val btc = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq"
        val sol = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"

        val combined = utils.combineAddresses(evm, btc, sol)
        val (parsedEvm, parsedBtc, parsedSol) = utils.parseAddresses(combined)

        assertEquals(evm, parsedEvm)
        assertEquals(btc, parsedBtc)
        assertEquals(sol, parsedSol)

        // Single EVM
        val (onlyEvm, noBtc, noSol) = utils.parseAddresses(evm)
        assertEquals(evm, onlyEvm)
        assertEquals(null, noBtc)
        assertEquals(null, noSol)

        // Single BTC
        val (noEvm1, onlyBtc, noSol1) = utils.parseAddresses(btc)
        assertEquals(null, noEvm1)
        assertEquals(btc, onlyBtc)
        assertEquals(null, noSol1)

        // Single SOL
        val (noEvm2, noBtc2, onlySol) = utils.parseAddresses(sol)
        assertEquals(null, noEvm2)
        assertEquals(null, noBtc2)
        assertEquals(sol, onlySol)
    }
}
