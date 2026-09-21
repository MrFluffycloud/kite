package com.expensevault.di

import androidx.room.Room
import com.expensevault.core.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "expense_vault.db"
        ).fallbackToDestructiveMigration().build()
    }

    // DAOs
    single { get<AppDatabase>().accountDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().receiptPhotoDao() }
    single { get<AppDatabase>().personDao() }
    single { get<AppDatabase>().debtRecordDao() }
    single { get<AppDatabase>().settlementDao() }
    single { get<AppDatabase>().recurringRuleDao() }
    single { get<AppDatabase>().budgetCapDao() }
    single { get<AppDatabase>().exchangeRateDao() }
}
