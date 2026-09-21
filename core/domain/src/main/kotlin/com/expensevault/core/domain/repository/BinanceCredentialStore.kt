package com.expensevault.core.domain.repository

import com.expensevault.core.model.BinanceCredentials

/**
 * Secure on-device credential store for Binance API keys.
 */
interface BinanceCredentialStore {
    fun saveCredentials(credentials: BinanceCredentials)
    fun getCredentials(): BinanceCredentials?
    fun clearCredentials()
    fun isLinked(): Boolean
    fun getMaskedApiKey(): String
    fun getEnabledWallets(): Set<String>
    fun saveEnabledWallets(wallets: Set<String>)
    fun getIntegrationType(): com.expensevault.core.model.BinanceIntegrationType
    fun saveIntegrationType(type: com.expensevault.core.model.BinanceIntegrationType)
    fun getWeb3Address(): String?
    fun saveWeb3Address(address: String)
    fun getWeb3Chains(): Set<String>
    fun saveWeb3Chains(chains: Set<String>)
}
