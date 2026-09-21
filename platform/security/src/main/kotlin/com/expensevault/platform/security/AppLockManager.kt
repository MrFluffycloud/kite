package com.expensevault.platform.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isUnlocked = MutableStateFlow(!_isAppLockEnabled.value)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        if (_isAppLockEnabled.value) {
            _isUnlocked.value = false
        }
    }

    fun isLockRequired(): Boolean {
        return _isAppLockEnabled.value && !_isUnlocked.value
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
        _isAppLockEnabled.value = enabled
        if (!enabled) {
            _isUnlocked.value = true
        }
    }

    companion object {
        const val PREFS_NAME = "expense_vault_security"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    }
}
