package com.expensevault.platform.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the Keystore Key Encryption Key (KEK) and the SQLCipher Database Encryption Key (DEK)
 * using envelope encryption.
 *
 * Pattern:
 * 1. Android Keystore holds AES-256-GCM KEK.
 * 2. A random 256-bit DEK is generated for SQLCipher.
 * 3. DEK is encrypted by KEK and stored in private shared preferences.
 * 4. On unlock, BiometricPrompt validates identity, KEK decrypts DEK.
 * 5. DEK is formatted as raw hex BLOB (x'<64 hex chars>') to bypass PBKDF2 (10ms open vs 400ms).
 */
class DatabaseKeyManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEK_ALIAS = "ExpenseVault_KEK_Alias"
        private const val PREFS_NAME = "vault_security_prefs"
        private const val KEY_ENCRYPTED_DEK = "encrypted_dek"
        private const val KEY_DEK_IV = "dek_iv"
        private const val GCM_TAG_LENGTH = 128
        private const val DEK_SIZE_BYTES = 32 // 256 bits
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Checks whether the Vault DEK has been initialized and stored.
     */
    fun isVaultInitialized(): Boolean {
        return prefs.contains(KEY_ENCRYPTED_DEK) && prefs.contains(KEY_DEK_IV)
    }

    /**
     * Gets or creates the Key Encryption Key (KEK) in Android Keystore.
     */
    fun getOrCreateKEK(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEK_ALIAS)) {
            val entry = keyStore.getEntry(KEK_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEK_ALIAS,
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

    /**
     * Prepares an encryption cipher for biometric enrollment or first initialization.
     */
    fun initCipherForEncryption(): Cipher {
        val kek = getOrCreateKEK()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, kek)
        return cipher
    }

    /**
     * Prepares a decryption cipher for biometric prompt unlock.
     */
    fun initCipherForDecryption(): Cipher {
        val kek = getOrCreateKEK()
        val ivBase64 = prefs.getString(KEY_DEK_IV, null) ?: throw IllegalStateException("Vault IV not found")
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, kek, spec)
        return cipher
    }

    /**
     * Generates a new random 256-bit DEK, encrypts it with the provided cipher, and persists it.
     * Returns the raw DEK bytes.
     */
    fun initializeVaultKey(cipher: Cipher): ByteArray {
        val dek = ByteArray(DEK_SIZE_BYTES)
        SecureRandom().nextBytes(dek)

        val encryptedDek = cipher.doFinal(dek)
        val iv = cipher.iv

        prefs.edit()
            .putString(KEY_ENCRYPTED_DEK, Base64.encodeToString(encryptedDek, Base64.NO_WRAP))
            .putString(KEY_DEK_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()

        return dek
    }

    /**
     * Decrypts the stored DEK using the authenticated cipher from BiometricPrompt.
     */
    fun decryptVaultKey(authenticatedCipher: Cipher): ByteArray {
        val encryptedDekBase64 = prefs.getString(KEY_ENCRYPTED_DEK, null)
            ?: throw IllegalStateException("No encrypted DEK found in storage")
        val encryptedDek = Base64.decode(encryptedDekBase64, Base64.NO_WRAP)
        return authenticatedCipher.doFinal(encryptedDek)
    }

    /**
     * Converts a raw 256-bit key to the SQLCipher raw key BLOB format:
     * x'<64 hex characters>'
     * This instructs SQLCipher to treat the key as a pre-derived key rather than passing it through PBKDF2.
     */
    fun formatSqlCipherBlob(rawKey: ByteArray): ByteArray {
        val hexChars = "0123456789abcdef".toCharArray()
        val hex = StringBuilder(rawKey.size * 2)
        for (b in rawKey) {
            val octet = b.toInt()
            hex.append(hexChars[(octet shr 4) and 0x0F])
            hex.append(hexChars[octet and 0x0F])
        }
        val blobString = "x'${hex}'"
        return blobString.toByteArray(Charsets.UTF_8)
    }

    /**
     * Securely wipes sensitive byte arrays from memory.
     */
    fun zeroize(bytes: ByteArray) {
        Arrays.fill(bytes, 0.toByte())
    }
}
