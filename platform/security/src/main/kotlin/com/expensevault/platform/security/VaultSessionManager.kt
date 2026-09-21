package com.expensevault.platform.security

import android.content.Context
import com.expensevault.core.database.VaultDatabase
import com.expensevault.core.database.VaultDatabaseFactory
import com.expensevault.core.database.dao.VaultExpenseDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the in-memory lifecycle of the decrypted VaultDatabase.
 * When locked: closes database, zeroes keys, and emits isVaultUnlocked = false.
 */
class VaultSessionManager(
    private val context: Context,
    private val databaseKeyManager: DatabaseKeyManager
) {
    private var activeDatabase: VaultDatabase? = null
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    @Synchronized
    fun unlock(rawKey: ByteArray) {
        val passphraseBlob = databaseKeyManager.formatSqlCipherBlob(rawKey)
        try {
            activeDatabase = VaultDatabaseFactory.create(context, passphraseBlob)
            _isVaultUnlocked.value = true
        } finally {
            databaseKeyManager.zeroize(rawKey)
            databaseKeyManager.zeroize(passphraseBlob)
        }
    }

    @Synchronized
    fun lock() {
        activeDatabase?.close()
        activeDatabase = null
        _isVaultUnlocked.value = false
    }

    fun getVaultExpenseDao(): VaultExpenseDao {
        return activeDatabase?.vaultExpenseDao()
            ?: throw IllegalStateException("Vault is locked. Biometric authentication required.")
    }
}
