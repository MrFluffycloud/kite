package com.expensevault.core.database.entity

import androidx.room.Entity
import java.math.BigDecimal

@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["baseCurrency", "targetCurrency", "rateDate"]
)
data class ExchangeRateEntity(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: BigDecimal,
    val rateDate: String,  // YYYY-MM-DD format
    val fetchedAt: Long
)
