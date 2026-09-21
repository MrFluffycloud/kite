package com.expensevault.platform.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.room.Room
import com.expensevault.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Data bundle for the Glance Widget presentation.
 */
data class WidgetData(
    val todaySpend: Double = 0.0,
    val monthSpend: Double = 0.0,
    val topCategories: List<Pair<String, Double>> = emptyList()
)

class ExpenseGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)

        provideContent {
            GlanceTheme {
                WidgetContent(context, data)
            }
        }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData = withContext(Dispatchers.IO) {
        try {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "expense_vault.db"
            ).fallbackToDestructiveMigration(false).build()

            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault()
            val localDate = now.toLocalDateTime(tz).date

            val startOfDay = localDate.toEpochDays().toLong()
            val endOfDay = startOfDay

            val startOfMonth = kotlinx.datetime.LocalDate(localDate.year, localDate.month, 1).toEpochDays().toLong()
            val endOfMonth = kotlinx.datetime.LocalDate(
                localDate.year,
                localDate.month,
                when (localDate.monthNumber) {
                    2 -> if (localDate.year % 4 == 0 && (localDate.year % 100 != 0 || localDate.year % 400 == 0)) 29 else 28
                    4, 6, 9, 11 -> 30
                    else -> 31
                }
            ).toEpochDays().toLong()

            val todayTotal = db.transactionDao().getTotalSpendByDateRange(startOfDay, endOfDay).firstOrNull() ?: 0.0
            val monthTotal = db.transactionDao().getTotalSpendByDateRange(startOfMonth, endOfMonth).firstOrNull() ?: 0.0

            val topSpends = db.transactionDao().getSpendByCategoryInRange(startOfMonth, endOfMonth).firstOrNull() ?: emptyList()
            val categories = db.categoryDao().getAll().firstOrNull() ?: emptyList()
            val categoryMap = categories.associate { it.id to it.name }

            val topCategories = topSpends.take(3).map { tuple ->
                val name = categoryMap[tuple.categoryId] ?: "Other"
                name to tuple.total
            }

            WidgetData(
                todaySpend = todayTotal,
                monthSpend = monthTotal,
                topCategories = topCategories
            )
        } catch (e: Exception) {
            WidgetData()
        }
    }

    private fun singleColor(color: Color) = ColorProvider(day = color, night = color)

    @Composable
    private fun WidgetContent(context: Context, data: WidgetData) {
        val mainComponent = ComponentName(context.packageName, "com.expensevault.MainActivity")

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1E1E2E))
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(mainComponent))
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kite",
                        style = TextStyle(
                            color = singleColor(Color(0xFF90CAF9)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Button(
                        text = "+ Add",
                        onClick = actionStartActivity(mainComponent),
                        modifier = GlanceModifier.height(28.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Monthly and Today Spend Summary
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "This Month",
                            style = TextStyle(
                                color = singleColor(Color(0xFFAAAAAA)),
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "₹${formatCurrency(data.monthSpend)}",
                            style = TextStyle(
                                color = singleColor(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Today",
                            style = TextStyle(
                                color = singleColor(Color(0xFFAAAAAA)),
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "₹${formatCurrency(data.todaySpend)}",
                            style = TextStyle(
                                color = singleColor(Color(0xFF81C784)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Top Categories breakdown
                if (data.topCategories.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "Top Spending",
                        style = TextStyle(
                            color = singleColor(Color(0xFF888888)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    data.topCategories.forEach { (categoryName, amount) ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = categoryName,
                                style = TextStyle(
                                    color = singleColor(Color.White),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "₹${formatCurrency(amount)}",
                                style = TextStyle(
                                    color = singleColor(Color(0xFFE0E0E0)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return if (amount >= 100000) {
            String.format(java.util.Locale.US, "%.1fL", amount / 100000)
        } else if (amount >= 1000) {
            String.format(java.util.Locale.US, "%.1fk", amount / 1000)
        } else {
            String.format(java.util.Locale.US, "%.0f", amount)
        }
    }
}
