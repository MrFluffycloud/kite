package com.expensevault.di

import com.expensevault.core.data.impl.VaultRepositoryImpl
import com.expensevault.core.domain.repository.VaultRepository
import com.expensevault.platform.security.AppLockManager
import com.expensevault.platform.security.BiometricAuthManager
import com.expensevault.platform.security.DatabaseKeyManager
import com.expensevault.platform.security.VaultSessionManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    // Security
    single { BiometricAuthManager(androidContext()) }
    single { AppLockManager(androidContext()) }
    single { DatabaseKeyManager(androidContext()) }
    single { VaultSessionManager(androidContext(), get()) }

    // Vault Repository
    single<VaultRepository> {
        VaultRepositoryImpl {
            get<VaultSessionManager>().getVaultExpenseDao()
        }
    }
}
