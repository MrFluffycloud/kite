package com.expensevault.core.data.impl

import com.expensevault.core.data.remote.SpendAdvisorService
import com.expensevault.core.model.FixedCommitment
import com.expensevault.core.model.SpendSummary
import com.expensevault.core.model.WeeklyAllowanceConfig
import io.ktor.client.HttpClient
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeeklyAllowanceTest {

    @Test
    fun testWeeklyAllowanceMathWithFixedNeeds() {
        // User sets £150 weekly allowance with £55 fixed commitment for train
        val trainCommitment = FixedCommitment(
            id = "c1",
            name = "Train there and back",
            amount = "55.00",
            isFulfilled = false
        )
        val config = WeeklyAllowanceConfig(
            totalAllowance = "150.00",
            currencySymbol = "£",
            fixedCommitments = listOf(trainCommitment)
        )

        assertEquals(BigDecimal("150.00"), config.totalAllowanceBigDecimal)
        assertEquals(BigDecimal("55.00"), config.totalFixedCommitmentsBigDecimal)
        assertEquals(BigDecimal("55.00"), config.reservedUnfulfilledFixedBigDecimal)
        assertEquals(BigDecimal("95.00"), config.discretionaryBudget)

        // If 5 days left in week, safe to spend per day = 95 / 5 = £19.00
        val daysLeft = 5
        val safeToSpend = config.discretionaryBudget.divide(BigDecimal(daysLeft), 2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("19.00"), safeToSpend)

        // Once train ticket is purchased (fulfilled = true)
        val fulfilledTrain = trainCommitment.copy(isFulfilled = true)
        val updatedConfig = config.copy(fixedCommitments = listOf(fulfilledTrain))

        assertEquals(BigDecimal.ZERO, updatedConfig.reservedUnfulfilledFixedBigDecimal)
        assertEquals(BigDecimal("150.00"), updatedConfig.discretionaryBudget)
    }

    @Test
    fun testSpendAdvisorHeuristicSuggestions() = runBlocking {
        // Test offline advice generation when transport/train is top spend
        val service = SpendAdvisorService(HttpClient())
        val summary = SpendSummary(
            weeklyAverageSpend = BigDecimal("120.00"),
            currentWeekSpend = BigDecimal("145.00"),
            topCategories = listOf(
                "Transport" to BigDecimal("55.00"),
                "Dining" to BigDecimal("40.00")
            ),
            currencySymbol = "£"
        )

        val tips = service.getSavingsAdvice(summary)
        assertTrue(tips.isNotEmpty())
        // Should contain transport advice or pacing advice
        val titles = tips.map { it.title }
        assertTrue(titles.any { it.contains("Transport") || it.contains("Travel") || it.contains("Pacing") || it.contains("Dinners") })
    }
}
