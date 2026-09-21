package com.expensevault.platform.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.expensevault.core.database.AppDatabase
import com.expensevault.core.database.entity.TransactionEntity
import com.expensevault.core.model.TransactionSource
import com.expensevault.core.model.TransactionType
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)

        val dedupKey = intent.getStringExtra(EXTRA_DEDUP_KEY)
        if (dedupKey != null) {
            TransactionNotificationListenerService.recordDebounce(dedupKey)
        }

        if (intent.action == ACTION_LOG_EXPENSE) {
            val amountStr = intent.getStringExtra(EXTRA_AMOUNT) ?: return
            val merchant = intent.getStringExtra(EXTRA_MERCHANT) ?: "Unknown"
            val categoryName = intent.getStringExtra(EXTRA_CATEGORY) ?: "General"
            val amount = try { BigDecimal(amountStr) } catch (e: Exception) { return }

            val cleanMerchant = merchant.lowercase().replace(Regex("[^a-z0-9]"), "")
            val formattedAmount = amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            TransactionNotificationListenerService.recordDebounce("sig_${formattedAmount}_$cleanMerchant")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    android.util.Log.d("NotificationAction", "Opening Room DB...")
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "expense_vault.db"
                    ).fallbackToDestructiveMigration().build()

                    val prefs = context.getSharedPreferences(
                        TransactionNotificationListenerService.PREFS_NAME,
                        Context.MODE_PRIVATE
                    )
                    val defaultAccountId = prefs.getLong(TransactionNotificationListenerService.PREF_KEY_DEFAULT_ACCOUNT_ID, -1L)

                    val accounts = db.accountDao().getAll().firstOrNull() ?: emptyList()
                    android.util.Log.d("NotificationAction", "Found ${accounts.size} accounts, defaultAccountId=$defaultAccountId")
                    val targetAccount = (if (defaultAccountId != -1L) accounts.find { it.id == defaultAccountId } else null)
                        ?: accounts.firstOrNull()
                    if (targetAccount == null) {
                        android.util.Log.e("NotificationAction", "No accounts found in DB! Cannot log transaction.")
                        return@launch
                    }

                    val categories = db.categoryDao().getAll().firstOrNull() ?: emptyList()
                    val targetCategory = categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }

                    val now = Clock.System.now()
                    val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

                    val transaction = TransactionEntity(
                        accountId = targetAccount.id,
                        categoryId = targetCategory?.id,
                        type = TransactionType.EXPENSE,
                        originalAmount = amount,
                        originalCurrency = targetAccount.defaultCurrency,
                        exchangeRate = BigDecimal.ONE,
                        baseAmount = amount,
                        note = "Auto-detected from payment notification",
                        merchant = merchant,
                        transactionDate = localDate.toEpochDays().toLong(),
                        createdAt = now.toEpochMilliseconds(),
                        updatedAt = now.toEpochMilliseconds(),
                        deduplicationKey = dedupKey,
                        source = TransactionSource.NOTIFICATION_DETECTED
                    )

                    val insertedId = db.transactionDao().insert(transaction)
                    android.util.Log.d("NotificationAction", "Successfully inserted transaction id=$insertedId")

                    // Deduct from account balance
                    val newBalance = targetAccount.currentBalance.subtract(amount)
                    db.accountDao().update(targetAccount.copy(currentBalance = newBalance, updatedAt = now.toEpochMilliseconds()))
                } catch (e: Exception) {
                    android.util.Log.e("NotificationAction", "Failed to log detected expense", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_LOG_EXPENSE = "com.expensevault.action.LOG_EXPENSE"
        const val ACTION_DISMISS = "com.expensevault.action.DISMISS"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_DEDUP_KEY = "extra_dedup_key"
    }
}
