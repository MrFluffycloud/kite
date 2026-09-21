package com.expensevault.platform.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.room.Room
import com.expensevault.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap

class TransactionNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // 1. Exclude notifications posted by ExpenseVault itself
        if (sbn.packageName == packageName) return

        // 2. Check if user enabled auto-detection in preferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(PREF_KEY_AUTO_DETECT, false)
        if (!isEnabled) return

        // 3. Filter out non-clearable / ongoing system notifications
        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val now = System.currentTimeMillis()
        cleanOldDebounceEntries(now)

        // Check if this exact sbn key was processed recently
        val sbnKey = sbn.key
        if (sbnKey != null && isDebounced("sbn_$sbnKey", now)) {
            return
        }

        // 4. Safely extract text (handling CharSequence & MessagingStyle from SMS/UPI apps)
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        // Extract latest message from MessagingStyle (Google Messages, SMS apps, messaging apps)
        val messageBundles = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        val lastMessageText = if (!messageBundles.isNullOrEmpty()) {
            val last = messageBundles.lastOrNull() as? Bundle
            last?.getCharSequence("text")?.toString()
        } else null

        val fullBody = when {
            !bigText.isNullOrBlank() -> bigText
            !lastMessageText.isNullOrBlank() -> lastMessageText
            !text.isNullOrBlank() -> text
            else -> subText
        }

        Log.d(TAG, "Notification received from ${sbn.packageName} | Title: '$title' | Body: '$fullBody'")

        // 5. Parse transaction details
        val parsed = TransactionNotificationParser.parse(title, fullBody) ?: return

        Log.d(TAG, "Successfully parsed transaction: ₹${parsed.amount} at ${parsed.merchant} (${parsed.suggestedCategoryName})")

        // 6. Transaction signature debounce (handles SMS + UPI app duplicate within 3 mins)
        val cleanMerchant = parsed.merchant.lowercase().replace(Regex("[^a-z0-9]"), "")
        val formattedAmount = parsed.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        val sigKey = "sig_${formattedAmount}_$cleanMerchant"

        if (isDebounced(sigKey, now) || isDebounced(parsed.deduplicationKey, now)) {
            Log.d(TAG, "Skipping duplicate notification: sigKey=$sigKey")
            return
        }

        // Record in debounce map immediately
        recordDebounce(sigKey, now)
        recordDebounce(parsed.deduplicationKey, now)
        if (sbnKey != null) {
            recordDebounce("sbn_$sbnKey", now)
        }

        // 7. Asynchronously verify against Room DB to ensure it's not already logged
        serviceScope.launch {
            try {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "expense_vault.db"
                ).fallbackToDestructiveMigration(false).build()

                val existing = db.transactionDao().getByDeduplicationKey(parsed.deduplicationKey)
                if (existing != null) {
                    Log.d(TAG, "Transaction already in database, skipping alert: ${parsed.deduplicationKey}")
                    return@launch
                }

                // 8. Present interactive local notification
                TransactionNotificationManager.showDetectedExpenseNotification(applicationContext, parsed)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking database, showing notification anyway", e)
                TransactionNotificationManager.showDetectedExpenseNotification(applicationContext, parsed)
            }
        }
    }

    companion object {
        private const val TAG = "ExpenseListener"

        const val PREFS_NAME = "expense_vault_settings"
        const val PREF_KEY_AUTO_DETECT = "auto_detect_notifications"
        const val PREF_KEY_DEFAULT_ACCOUNT_ID = "pref_default_account_id"

        private const val DEBOUNCE_WINDOW_MS = 180_000L // 3 minutes
        private const val CLEANUP_WINDOW_MS = 600_000L  // 10 minutes

        private val debounceCache = ConcurrentHashMap<String, Long>()

        fun recordDebounce(key: String, timestamp: Long = System.currentTimeMillis()) {
            debounceCache[key] = timestamp
        }

        fun isDebounced(key: String, now: Long = System.currentTimeMillis()): Boolean {
            val lastTime = debounceCache[key] ?: return false
            return (now - lastTime) < DEBOUNCE_WINDOW_MS
        }

        private fun cleanOldDebounceEntries(now: Long) {
            if (debounceCache.size > 50) {
                val iterator = debounceCache.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (now - entry.value > CLEANUP_WINDOW_MS) {
                        iterator.remove()
                    }
                }
            }
        }

        /**
         * Re-binds the notification listener service if disconnected by Android system.
         */
        fun requestRebindService(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val componentName = ComponentName(context, TransactionNotificationListenerService::class.java)
                    requestRebind(componentName)
                    Log.d(TAG, "Requested rebind for TransactionNotificationListenerService")
                } catch (e: Exception) {
                    Log.w(TAG, "requestRebind error: ${e.message}")
                }
            }
        }
    }
}
