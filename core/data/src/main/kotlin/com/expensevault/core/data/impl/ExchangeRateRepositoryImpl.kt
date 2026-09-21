package com.expensevault.core.data.impl

import com.expensevault.core.database.dao.ExchangeRateDao
import com.expensevault.core.database.mapper.toDomain
import com.expensevault.core.database.mapper.toEntity
import com.expensevault.core.domain.repository.ExchangeRateRepository
import com.expensevault.core.model.ExchangeRate
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class FrankfurterResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

class ExchangeRateRepositoryImpl(
    private val exchangeRateDao: ExchangeRateDao,
    private val httpClient: HttpClient
) : ExchangeRateRepository {

    override suspend fun getRate(baseCurrency: String, targetCurrency: String, date: LocalDate): ExchangeRate? {
        val dateString = date.toString()
        return exchangeRateDao.getRate(baseCurrency, targetCurrency, dateString)?.toDomain()
    }

    override suspend fun getLatestRate(baseCurrency: String, targetCurrency: String): ExchangeRate? {
        return exchangeRateDao.getLatestRate(baseCurrency, targetCurrency)?.toDomain()
    }

    override suspend fun fetchAndCacheRates(baseCurrency: String): Result<List<ExchangeRate>> = withContext(Dispatchers.IO) {
        try {
            val response: FrankfurterResponse = httpClient.get("https://api.frankfurter.app/latest?from=$baseCurrency").body()
            
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val rates = response.rates.map { (target, rateDouble) ->
                ExchangeRate(
                    baseCurrency = baseCurrency,
                    targetCurrency = target,
                    rate = BigDecimal(rateDouble.toString()),
                    rateDate = currentDate,
                    fetchedAt = Clock.System.now()
                )
            }
            
            exchangeRateDao.insertAll(rates.map { it.toEntity() })
            Result.success(rates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
