package com.expensevault.core.database.dao

import androidx.room.*
import com.expensevault.core.database.entity.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE baseCurrency = :base AND targetCurrency = :target AND rateDate = :date LIMIT 1")
    suspend fun getRate(base: String, target: String, date: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates WHERE baseCurrency = :base AND targetCurrency = :target ORDER BY rateDate DESC LIMIT 1")
    suspend fun getLatestRate(base: String, target: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<ExchangeRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: ExchangeRateEntity)
}
