package com.expensevault.core.domain.repository

import com.expensevault.core.model.ExchangeRate
import kotlinx.datetime.LocalDate

interface ExchangeRateRepository {
    suspend fun getRate(baseCurrency: String, targetCurrency: String, date: LocalDate): ExchangeRate?
    suspend fun getLatestRate(baseCurrency: String, targetCurrency: String): ExchangeRate?
    suspend fun fetchAndCacheRates(baseCurrency: String): Result<List<ExchangeRate>>
}
