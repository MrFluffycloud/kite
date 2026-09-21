package com.expensevault.core.data.di

import com.expensevault.core.data.impl.*
import com.expensevault.core.domain.repository.*
import com.expensevault.core.domain.usecase.*
import org.koin.dsl.module

val dataModule = module {
    single<TransactionRepository> { TransactionRepositoryImpl(get(), get()) }
    single<AccountRepository> { AccountRepositoryImpl(get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get()) }
    single<DebtRepository> { DebtRepositoryImpl(get(), get()) }
    single<PersonRepository> { PersonRepositoryImpl(get(), get()) }
    single<ExchangeRateRepository> { ExchangeRateRepositoryImpl(get(), get()) }
    single<BudgetRepository> { BudgetRepositoryImpl(get()) }
    single<RecurringRepository> { RecurringRepositoryImpl(get()) }

    // Use cases
    single { AddTransactionUseCase(get(), get(), get()) }
    single { CheckBudgetUseCase(get()) }
    single { GetInsightsUseCase(get()) }
    single { SettleDebtUseCase(get()) }
    single { SplitExpenseUseCase(get()) }
    single { ProcessRecurringRulesUseCase(get(), get(), get()) }
    single<com.expensevault.core.domain.usecase.ExportDataUseCase> { 
        com.expensevault.core.data.usecase.ExportDataUseCaseImpl(get(), get(), get()) 
    }
    single<com.expensevault.core.domain.usecase.ImportDataUseCase> { 
        com.expensevault.core.data.usecase.ImportDataUseCaseImpl(get(), get(), get()) 
    }
}

