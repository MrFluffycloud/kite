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
}
