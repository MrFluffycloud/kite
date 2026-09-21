package com.expensevault.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.math.BigDecimal
import java.math.RoundingMode

object TransactionNotificationManager {

    private const val CHANNEL_ID = "detected_expenses_channel"
    private const val CHANNEL_NAME = "Detected Expenses"
    private const val CHANNEL_DESC = "Interactive notifications for auto-detected UPI and bank payments"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun getCurrencySymbol(context: Context): String {
        val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
        return when (prefs.getString("base_currency", "INR")) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "CAD" -> "CA$"
            "AUD" -> "AU$"
            else -> "₹"
        }
    }

    fun showDetectedExpenseNotification(context: Context, parsed: ParsedTransaction) {
        createNotificationChannel(context)

        val cleanMerchant = parsed.merchant.lowercase().replace(Regex("[^a-z0-9]"), "")
        val formattedAmount = parsed.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        val normalizedKey = "${formattedAmount}_$cleanMerchant"
        val notificationId = (normalizedKey.hashCode() and 0x7FFFFFFF)
        val symbol = getCurrencySymbol(context)

        // 1. Content Intent (Open App to Edit/Add)
        val openAppIntent = Intent().apply {
            component = ComponentName(context.packageName, "com.expensevault.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "add_transaction")
            putExtra("amount", formattedAmount)
            putExtra("merchant", parsed.merchant)
            putExtra("category", parsed.suggestedCategoryName)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action Intent: "Log Now"
        val logNowIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_LOG_EXPENSE
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_AMOUNT, formattedAmount)
            putExtra(NotificationActionReceiver.EXTRA_MERCHANT, parsed.merchant)
            putExtra(NotificationActionReceiver.EXTRA_CATEGORY, parsed.suggestedCategoryName)
            putExtra(NotificationActionReceiver.EXTRA_DEDUP_KEY, parsed.deduplicationKey)
        }
        val logNowPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            logNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action Intent: "Dismiss"
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_DEDUP_KEY, parsed.deduplicationKey)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$symbol$formattedAmount at ${parsed.merchant}")
            .setContentText("Suggested category: ${parsed.suggestedCategoryName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Expense of $symbol$formattedAmount detected for ${parsed.merchant}.\n" +
                (if (parsed.accountSuffix != null) "Account ending: ${parsed.accountSuffix}\n" else "") +
                "Tap 'Log Now' to record immediately or tap notification to edit."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "Log Now", logNowPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }

    /**
     * Sends an interactive test notification to let users verify automated expense detection immediately.
     */
    fun showTestExpenseNotification(context: Context) {
        val sample = ParsedTransaction(
            amount = BigDecimal("350.00"),
            merchant = "Starbucks",
            suggestedCategoryName = "Food",
            accountSuffix = "1234",
            deduplicationKey = "test_${System.currentTimeMillis()}",
            rawText = "Paid 350.00 to Starbucks using Card ending 1234"
        )
        showDetectedExpenseNotification(context, sample)
    }
}
