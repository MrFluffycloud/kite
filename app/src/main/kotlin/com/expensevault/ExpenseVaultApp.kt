package com.expensevault

import android.app.Application
import com.expensevault.di.appModule
import com.expensevault.di.databaseModule
import com.expensevault.di.viewModelModule
import com.expensevault.core.data.di.dataModule
import com.expensevault.core.data.di.networkModule
import com.expensevault.worker.ExchangeRateSyncWorker
import com.expensevault.worker.RecurringExpenseWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

import com.expensevault.core.database.dao.AccountDao
import com.expensevault.core.database.dao.CategoryDao
import com.expensevault.core.database.entity.AccountEntity
import com.expensevault.core.database.seed.DefaultCategorySeeder
import com.expensevault.core.model.AccountType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.math.BigDecimal

open class KiteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(org.koin.core.logger.Level.ERROR)
            androidContext(this@KiteApp)
            modules(
                appModule,
                databaseModule,
                dataModule,
                networkModule,
                viewModelModule
            )
        }
        ExchangeRateSyncWorker.enqueuePeriodicSync(this)
        RecurringExpenseWorker.enqueuePeriodicWork(this)

        val accountDao: AccountDao by inject()
        val categoryDao: CategoryDao by inject()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val accounts = accountDao.getAll().firstOrNull()
                if (accounts.isNullOrEmpty()) {
                    val now = System.currentTimeMillis()
                    accountDao.insert(
                        AccountEntity(
                            name = "Main Wallet",
                            type = AccountType.BANK_ACCOUNT,
                            defaultCurrency = "INR",
                            initialBalance = BigDecimal.ZERO,
                            currentBalance = BigDecimal.ZERO,
                            iconName = "account_balance",
                            colorHex = "#2196F3",
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
                DefaultCategorySeeder(categoryDao).seedDefaultCategories()
            } catch (_: Exception) {}
        }
    }
}

typealias ExpenseVaultApp = KiteApp

