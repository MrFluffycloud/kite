package com.expensevault

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import com.expensevault.navigation.KiteNavHost
import com.expensevault.navigation.NavRoute
import com.expensevault.platform.notification.TransactionNotificationListenerService
import com.expensevault.platform.notification.TransactionNotificationManager
import com.expensevault.platform.security.AppLockManager
import com.expensevault.platform.security.BiometricAuthManager
import com.expensevault.platform.widget.WidgetManager
import com.expensevault.ui.theme.KiteTheme
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {

    private val appLockManager: AppLockManager by inject()
    private val biometricAuthManager: BiometricAuthManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        TransactionNotificationManager.createNotificationChannel(applicationContext)

        val navigateTo = intent?.getStringExtra("navigate_to")
        val startOverride: NavRoute? = if (navigateTo == "add_transaction") {
            NavRoute.AddTransaction(
                prefillAmount = intent?.getStringExtra("amount"),
                prefillMerchant = intent?.getStringExtra("merchant"),
                prefillCategory = intent?.getStringExtra("category")
            )
        } else {
            null
        }

        setContent {
            KiteTheme {
                KiteNavHost(
                    startDestinationOverride = startOverride,
                    appLockManager = appLockManager,
                    biometricAuthManager = biometricAuthManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-bind notification listener if enabled and granted
        val prefs = getSharedPreferences(TransactionNotificationListenerService.PREFS_NAME, Context.MODE_PRIVATE)
        val isAutoDetectEnabled = prefs.getBoolean(TransactionNotificationListenerService.PREF_KEY_AUTO_DETECT, false)
        val isAccessGranted = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (isAutoDetectEnabled && isAccessGranted) {
            TransactionNotificationListenerService.requestRebindService(this)
        }
    }

    override fun onStop() {
        super.onStop()
        appLockManager.lock()
        // Refresh home screen Glance widgets whenever user backgrounds the app
        WidgetManager.updateAllWidgets(applicationContext)
    }
}
