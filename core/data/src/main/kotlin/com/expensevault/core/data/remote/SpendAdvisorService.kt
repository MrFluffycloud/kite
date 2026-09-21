package com.expensevault.core.data.remote

import com.expensevault.core.domain.repository.SpendAdvisorRepository
import com.expensevault.core.model.SavingsTip
import com.expensevault.core.model.SpendSummary
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpendAdvisorService(
    private val httpClient: HttpClient
) : SpendAdvisorRepository {

    override suspend fun getSavingsAdvice(summary: SpendSummary): List<SavingsTip> = withContext(Dispatchers.IO) {
        // Attempt AI completion via Pollinations/g4f free text endpoint
        try {
            val breakdownStr = summary.topCategories.take(4).joinToString(", ") {
                "${it.first}: ${summary.currencySymbol}${it.second.setScale(2, RoundingMode.HALF_UP)}"
            }
            val prompt = "You are a concise financial advisor. User weekly average spend is ${summary.currencySymbol}${summary.weeklyAverageSpend}. This week so far: ${summary.currencySymbol}${summary.currentWeekSpend}. Top categories: $breakdownStr. Give 3 actionable, specific ways to reduce weekly spend in format: - Title | Description | Saves ~${summary.currencySymbol}X/week. Max 3 bullets."

            val encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString())
            val responseText = httpClient.get("https://text.pollinations.ai/$encoded").bodyAsText().trim()

            val lines = responseText.lines().filter { it.trim().startsWith("-") || it.trim().matches(Regex("^\\d+\\..*")) }
            if (lines.isNotEmpty()) {
                val parsed = lines.take(3).mapNotNull { line ->
                    val clean = line.removePrefix("-").replace(Regex("^\\d+\\."), "").trim()
                    val parts = clean.split("|")
                    if (parts.size >= 3) {
                        SavingsTip(
                            title = parts[0].trim(),
                            description = parts[1].trim(),
                            potentialWeeklySavings = parts[2].trim()
                        )
                    } else if (parts.size == 2) {
                        SavingsTip(
                            title = parts[0].trim(),
                            description = parts[1].trim(),
                            potentialWeeklySavings = "Save 10-15%"
                        )
                    } else {
                        val title = clean.take(40)
                        SavingsTip(
                            title = title,
                            description = clean,
                            potentialWeeklySavings = "Estimated ~${summary.currencySymbol}15-25/wk"
                        )
                    }
                }
                if (parsed.isNotEmpty()) return@withContext parsed
            }
        } catch (_: Exception) {
            // Fallback to offline rule-based heuristics
        }

        // Offline Rule-based Heuristic Advice Generator
        generateHeuristicAdvice(summary)
    }

    private fun generateHeuristicAdvice(summary: SpendSummary): List<SavingsTip> {
        val tips = mutableListOf<SavingsTip>()
        val sym = summary.currencySymbol
        val topCategory = summary.topCategories.firstOrNull()?.first?.lowercase() ?: ""

        when {
            topCategory.contains("food") || topCategory.contains("dining") || topCategory.contains("restaurant") || topCategory.contains("cafe") -> {
                tips.add(
                    SavingsTip(
                        title = "Cook 2 More Dinners at Home",
                        description = "Dining out and takeout represent your largest spend chunk. Swapping 2 meals for home cooking or packed lunches can make a noticeable impact.",
                        potentialWeeklySavings = "Save ~${sym}25 - ${sym}40/wk",
                        category = "Dining"
                    )
                )
            }
            topCategory.contains("grocer") || topCategory.contains("market") -> {
                tips.add(
                    SavingsTip(
                        title = "Plan Meals & Audit Pantry",
                        description = "Groceries are your top expense. Planning weekly dinners in advance and shopping with a strict checklist cuts food waste by up to 20%.",
                        potentialWeeklySavings = "Save ~${sym}15 - ${sym}30/wk",
                        category = "Groceries"
                    )
                )
            }
            topCategory.contains("commute") || topCategory.contains("transport") || topCategory.contains("train") || topCategory.contains("travel") -> {
                tips.add(
                    SavingsTip(
                        title = "Audit Fixed Travel & Commute Passes",
                        description = "Transport is a primary expense. Check if weekly season tickets, off-peak travel caps, or railcards apply to your regular routes.",
                        potentialWeeklySavings = "Save ~${sym}10 - ${sym}20/wk",
                        category = "Transport"
                    )
                )
            }
            topCategory.contains("shop") || topCategory.contains("cloth") || topCategory.contains("electronic") -> {
                tips.add(
                    SavingsTip(
                        title = "Implement a 48-Hour Wishlist Rule",
                        description = "Shopping takes up significant weekly funds. Placing non-essential items on a 48-hour delay eliminates impulsive purchases.",
                        potentialWeeklySavings = "Save ~${sym}20 - ${sym}50/wk",
                        category = "Shopping"
                    )
                )
            }
        }

        // Pacing Tip
        if (summary.weeklyAverageSpend > BigDecimal.ZERO) {
            if (summary.currentWeekSpend > summary.weeklyAverageSpend) {
                val excess = summary.currentWeekSpend.subtract(summary.weeklyAverageSpend).setScale(2, RoundingMode.HALF_UP)
                tips.add(
                    SavingsTip(
                        title = "Weekly Pacing Alert",
                        description = "You are currently $sym$excess above your typical weekly burn rate. Slowing down discretionary spending over the weekend will rebalance your month.",
                        potentialWeeklySavings = "Save ~$sym$excess",
                        category = "Pacing"
                    )
                )
            } else {
                val buffer = summary.weeklyAverageSpend.subtract(summary.currentWeekSpend).setScale(2, RoundingMode.HALF_UP)
                tips.add(
                    SavingsTip(
                        title = "Strong Weekly Pacing",
                        description = "You have maintained spending $sym$buffer below your 4-week average. Auto-sweeping this surplus into a rainy-day fund builds effortless savings.",
                        potentialWeeklySavings = "Banked ~$sym$buffer surplus",
                        category = "Savings"
                    )
                )
            }
        }

        // Universal Subscription / Micro-spend Tip
        tips.add(
            SavingsTip(
                title = "Audit Recurring Micro-Transactions",
                description = "Daily coffees, app subscriptions, and small treats accumulate silently. Replacing one £3.50 daily habit with an alternative frees up over £100 monthly.",
                potentialWeeklySavings = "Save ~${sym}15 - ${sym}25/wk",
                category = "Lifestyle"
            )
        )

        return tips.take(3)
    }
}
