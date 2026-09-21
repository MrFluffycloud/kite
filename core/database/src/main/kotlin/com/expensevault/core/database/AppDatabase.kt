package com.expensevault.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.expensevault.core.database.converter.Converters
import com.expensevault.core.database.dao.*
import com.expensevault.core.database.entity.*

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        ReceiptPhotoEntity::class,
        PersonEntity::class,
        DebtRecordEntity::class,
        SettlementEntity::class,
        RecurringRuleEntity::class,
        BudgetCapEntity::class,
        ExchangeRateEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun receiptPhotoDao(): ReceiptPhotoDao
    abstract fun personDao(): PersonDao
    abstract fun debtRecordDao(): DebtRecordDao
    abstract fun settlementDao(): SettlementDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun budgetCapDao(): BudgetCapDao
    abstract fun exchangeRateDao(): ExchangeRateDao
}
