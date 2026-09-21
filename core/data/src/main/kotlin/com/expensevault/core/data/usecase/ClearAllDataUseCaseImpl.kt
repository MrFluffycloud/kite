package com.expensevault.core.data.usecase

import android.content.Context
import com.expensevault.core.database.AppDatabase
import com.expensevault.core.database.entity.AccountEntity
import com.expensevault.core.database.seed.DefaultCategorySeeder
import com.expensevault.core.domain.repository.BinanceCredentialStore
import com.expensevault.core.domain.usecase.ClearAllDataUseCase
import com.expensevault.core.model.AccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ClearAllDataUseCaseImpl(
    private val appDatabase: AppDatabase,
    private val binanceCredentialStore: BinanceCredentialStore,
    private val context: Context
) : ClearAllDataUseCase {

    override suspend fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Wipe all tables in AppDatabase
            appDatabase.clearAllTables()

            // 2. Read base currency preference or default to INR
            val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
            val baseCurrency = prefs.getString("pref_base_currency", "INR") ?: "INR"

            // 3. Reseed clean default Main Wallet
            val now = System.currentTimeMillis()
            val defaultAccountId = appDatabase.accountDao().insert(
                AccountEntity(
                    name = "Main Wallet",
                    type = AccountType.BANK_ACCOUNT,
                    defaultCurrency = baseCurrency,
                    initialBalance = BigDecimal.ZERO,
                    currentBalance = BigDecimal.ZERO,
                    iconName = "account_balance",
                    colorHex = "#2196F3",
                    createdAt = now,
                    updatedAt = now
                )
            )

            // 4. Reseed default categories and subcategories
            DefaultCategorySeeder(appDatabase.categoryDao()).seedDefaultCategories()

            // 5. Unlink Binance credentials from KeyStore
            binanceCredentialStore.clearCredentials()

            // 6. Reset default account ID in preferences
            prefs.edit()
                .putLong("pref_default_account_id", defaultAccountId)
                .apply()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
