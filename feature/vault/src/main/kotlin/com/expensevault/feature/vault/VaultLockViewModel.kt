package com.expensevault.feature.vault

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.platform.security.BiometricAuthManager
import com.expensevault.platform.security.BiometricAvailability
import com.expensevault.platform.security.DatabaseKeyManager
import com.expensevault.platform.security.VaultSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultLockUiState(
    val isInitialized: Boolean = false,
    val isAuthenticating: Boolean = false,
    val isUnlocked: Boolean = false,
    val errorMessage: String? = null,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.AVAILABLE
)

class VaultLockViewModel(
    private val databaseKeyManager: DatabaseKeyManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val vaultSessionManager: VaultSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VaultLockUiState(
            isInitialized = databaseKeyManager.isVaultInitialized(),
            biometricAvailability = biometricAuthManager.canAuthenticate()
        )
    )
    val uiState: StateFlow<VaultLockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            vaultSessionManager.isVaultUnlocked.collect { unlocked ->
                _uiState.update { it.copy(isUnlocked = unlocked) }
            }
        }
    }

    fun authenticateAndUnlock(activity: FragmentActivity) {
        val isInit = databaseKeyManager.isVaultInitialized()
        _uiState.update { it.copy(isAuthenticating = true, errorMessage = null) }

        try {
            if (!isInit) {
                // First-time setup: initialize cipher for encryption
                val cipher = databaseKeyManager.initCipherForEncryption()
                biometricAuthManager.authenticateWithCrypto(
                    activity = activity,
                    cipher = cipher,
                    title = "Set Up Private Vault",
                    subtitle = "Scan your fingerprint to encrypt your vault",
                    onSuccess = { authenticatedCipher ->
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                val rawKey = databaseKeyManager.initializeVaultKey(authenticatedCipher)
                                vaultSessionManager.unlock(rawKey)
                                _uiState.update {
                                    it.copy(
                                        isAuthenticating = false,
                                        isInitialized = true,
                                        isUnlocked = true
                                    )
                                }
                            } catch (e: Exception) {
                                _uiState.update {
                                    it.copy(
                                        isAuthenticating = false,
                                        errorMessage = "Initialization error: ${e.message}"
                                    )
                                }
                            }
                        }
                    },
                    onError = { error ->
                        _uiState.update {
                            it.copy(isAuthenticating = false, errorMessage = error)
                        }
                    }
                )
            } else {
                // Subsequent unlocks: initialize cipher for decryption
                val cipher = databaseKeyManager.initCipherForDecryption()
                biometricAuthManager.authenticateWithCrypto(
                    activity = activity,
                    cipher = cipher,
                    title = "Unlock Private Vault",
                    subtitle = "Scan biometric to decrypt private section",
                    onSuccess = { authenticatedCipher ->
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                val rawKey = databaseKeyManager.decryptVaultKey(authenticatedCipher)
                                vaultSessionManager.unlock(rawKey)
                                _uiState.update {
                                    it.copy(isAuthenticating = false, isUnlocked = true)
                                }
                            } catch (e: Exception) {
                                _uiState.update {
                                    it.copy(
                                        isAuthenticating = false,
                                        errorMessage = "Decryption error: ${e.message}"
                                    )
                                }
                            }
                        }
                    },
                    onError = { error ->
                        _uiState.update {
                            it.copy(isAuthenticating = false, errorMessage = error)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isAuthenticating = false, errorMessage = e.message ?: "Failed to start biometric auth")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
