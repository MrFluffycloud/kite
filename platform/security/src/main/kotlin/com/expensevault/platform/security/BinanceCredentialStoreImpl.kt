package com.expensevault.platform.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.expensevault.core.domain.repository.BinanceCredentialStore
import com.expensevault.core.model.BinanceCredentials
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed implementation of BinanceCredentialStore.
 * The API Secret is encrypted using AES-256-GCM before storage.
 */
class BinanceCredentialStoreImpl(private val context: Context) : BinanceCredentialStore {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "Binance_Credential_KEK"
        private const val PREFS_NAME = "binance_secure_prefs"
        private const val PREF_API_KEY = "binance_api_key"
        private const val PREF_ENCRYPTED_SECRET = "binance_encrypted_secret"
        private const val PREF_SECRET_IV = "binance_secret_iv"
        private const val PREF_ENABLED_WALLETS = "binance_enabled_wallets"
        private val DEFAULT_WALLETS = setOf("Spot", "Funding", "Earn", "Futures", "Margin")
        private const val GCM_TAG_LENGTH = 128
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun saveCredentials(credentials: BinanceCredentials) {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encryptedSecret = cipher.doFinal(credentials.apiSecret.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        prefs.edit()
            .putString(PREF_API_KEY, credentials.apiKey.trim())
            .putString(PREF_ENCRYPTED_SECRET, Base64.encodeToString(encryptedSecret, Base64.NO_WRAP))
            .putString(PREF_SECRET_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    override fun getCredentials(): BinanceCredentials? {
        val apiKey = prefs.getString(PREF_API_KEY, null) ?: return null
        val encryptedSecretBase64 = prefs.getString(PREF_ENCRYPTED_SECRET, null) ?: return null
        val ivBase64 = prefs.getString(PREF_SECRET_IV, null) ?: return null

        return try {
            val secretKey = getOrCreateKey()
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val encryptedSecret = Base64.decode(encryptedSecretBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedSecret)
            val secret = String(decryptedBytes, Charsets.UTF_8)
            BinanceCredentials(apiKey = apiKey, apiSecret = secret)
        } catch (_: Exception) {
            null
        }
    }

    override fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    override fun isLinked(): Boolean {
        return prefs.contains(PREF_API_KEY) &&
                prefs.contains(PREF_ENCRYPTED_SECRET) &&
                prefs.contains(PREF_SECRET_IV)
    }

    override fun getMaskedApiKey(): String {
        val key = prefs.getString(PREF_API_KEY, null) ?: return ""
        return if (key.length > 8) {
            "${key.take(4)}...${key.takeLast(4)}"
        } else {
            "****"
        }
    }

    override fun getEnabledWallets(): Set<String> {
        val stored = prefs.getStringSet(PREF_ENABLED_WALLETS, null)
        return stored ?: DEFAULT_WALLETS
    }

    override fun saveEnabledWallets(wallets: Set<String>) {
        prefs.edit().putStringSet(PREF_ENABLED_WALLETS, wallets).apply()
    }
}
