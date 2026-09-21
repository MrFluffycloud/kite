package com.expensevault.feature.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.SavingsTip
import com.expensevault.core.model.Transaction
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import java.math.BigDecimal
import org.koin.androidx.compose.koinViewModel

// Strict Design Tokens
private val NeutralBg = Color(0xFFFAFAF9)
private val NeutralCard = Color(0xFFFFFFFF)
private val NeutralMuted = Color(0xFFF0F0EE)
private val InkPrimary = Color(0xFF111111)
private val InkSecondary = Color(0xFF6B6B68)
private val InkTertiary = Color(0xFFB8B8B5)
private val HeroBlack = Color(0xFF0F0F0F)
private val HeroBlackSubtext = Color(0xFF8A8A87)
private val Hairline = Color(0xFFE7E6E3)
private val MutedClay = Color(0xFFB5533C)
private val MutedSage = Color(0xFF5C6E5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = koinViewModel(),
    onTransactionClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = NeutralBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeutralBg,
                    titleContentColor = InkPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PeriodSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = viewModel::onPeriodSelected
                )
            }

            // Emphasis Card (Max 1 per screen)
            item {
                SpendOverviewCard(
                    totalSpend = uiState.totalSpend.toString(),
                    percentChange = uiState.percentChange,
                    currencySymbol = uiState.currencySymbol
                )
            }

            // Weekly Average & Pacing Analysis Card
            item {
                WeeklyAverageCard(
                    weeklyAverage = uiState.weeklyAverageSpend,
                    currentWeekSpend = uiState.currentWeekSpend,
                    currencySymbol = uiState.currencySymbol
                )
            }

            // Smart Spend Advisor & Ways to Reduce Spend
            item {
                SpendAdvisorCard(
                    tips = uiState.savingsTips,
                    isLoading = uiState.isAdvisorLoading,
                    onRefresh = { viewModel.refreshSavingsAdvice() }
                )
            }

            // Spending by Category Chart
            item {
                CategorySpendingChartCard(
                    hasSufficientData = uiState.hasSufficientCategoryData,
                    modelProducer = viewModel.barChartModelProducer
                )
            }

            // Daily Spending Trend Chart
            item {
                DailyTrendChartCard(
                    hasSufficientData = uiState.hasSufficientTrendData,
                    modelProducer = viewModel.lineChartModelProducer
                )
            }

            // Top / Biggest Transactions
            if (uiState.topTransactions.isNotEmpty()) {
                item {
                    Text(
                        text = "Biggest transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralCard),
                        border = BorderStroke(1.dp, Hairline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column {
                            uiState.topTransactions.forEachIndexed { index, transaction ->
                                TransactionRow(
                                    transaction = transaction,
                                    currencySymbol = uiState.currencySymbol,
                                    onClick = { onTransactionClick(transaction.id) }
                                )
                                if (index < uiState.topTransactions.size - 1) {
                                    HorizontalDivider(
                                        color = Hairline,
                                        thickness = 0.8.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: InsightsPeriod,
    onPeriodSelected: (InsightsPeriod) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(InsightsPeriod.values()) { period ->
            val isSelected = period == selectedPeriod
            val label = when (period) {
                InsightsPeriod.THIS_WEEK -> "This week"
                InsightsPeriod.THIS_MONTH -> "This month"
                InsightsPeriod.LAST_MONTH -> "Last month"
                InsightsPeriod.THIS_QUARTER -> "This quarter"
                InsightsPeriod.THIS_YEAR -> "This year"
            }
            FilterChip(
                selected = isSelected,
                onClick = { onPeriodSelected(period) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HeroBlack,
                    selectedLabelColor = NeutralBg,
                    containerColor = NeutralCard,
                    labelColor = InkSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) HeroBlack else Hairline,
                    borderWidth = 1.dp
                ),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFFAFAF9) else InkSecondary
                    )
                }
            )
        }
    }
}

@Composable
private fun SpendOverviewCard(
    totalSpend: String,
    percentChange: Double?,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HeroBlack),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Total spend",
                style = MaterialTheme.typography.labelMedium,
                color = HeroBlackSubtext
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$currencySymbol$totalSpend",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                fontWeight = FontWeight.SemiBold,
                color = NeutralBg
            )
            if (percentChange != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val prefix = if (percentChange > 0) "+" else ""
                val isIncrease = percentChange > 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (isIncrease) MutedClay else MutedSage,
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$prefix${String.format("%.1f", percentChange)}% vs last period",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isIncrease) MutedClay else MutedSage,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyAverageCard(
    weeklyAverage: BigDecimal,
    currentWeekSpend: BigDecimal,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Weekly Average & Pacing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Text(
                text = "Based on your 4-week rolling expenses",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "4-Week Avg / Week",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${weeklyAverage.toPlainString()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "This Week So Far",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${currentWeekSpend.toPlainString()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (weeklyAverage > BigDecimal.ZERO && currentWeekSpend > weeklyAverage) MutedClay else InkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Hairline, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Pacing comparison note
            if (weeklyAverage > BigDecimal.ZERO) {
                if (currentWeekSpend > weeklyAverage) {
                    val excess = currentWeekSpend.subtract(weeklyAverage)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MutedClay, RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pacing $currencySymbol$excess above your typical weekly burn rate.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MutedClay
                        )
                    }
                } else {
                    val remainingBuffer = weeklyAverage.subtract(currentWeekSpend)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MutedSage, RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pacing safely within average ($currencySymbol$remainingBuffer buffer remaining).",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MutedSage
                        )
                    }
                }
            } else {
                Text(
                    text = "Keep logging expenses to establish your 4-week average baseline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }
    }
}

@Composable
private fun SpendAdvisorCard(
    tips: List<SavingsTip>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NeutralMuted, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Spend Advisor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                        Text(
                            text = "Ways to reduce spend & save",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = InkPrimary
                    )
                } else {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Advice",
                            tint = InkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (tips.isEmpty()) {
                Text(
                    text = "Analyzing your recent transactions to formulate targeted ways to cut spend...",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tips.forEach { tip ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NeutralBg,
                            border = BorderStroke(0.8.dp, Hairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tip.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkPrimary,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFEBF3EA)
                                    ) {
                                        Text(
                                            text = tip.potentialWeeklySavings,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MutedSage,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tip.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = NeutralMuted, shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Category,
                    contentDescription = null,
                    tint = InkPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = transaction.merchant ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.transactionDate.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }
        Text(
            text = "$currencySymbol${transaction.baseAmount}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MutedClay
        )
    }
}

@Composable
private fun ChartEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = InkTertiary,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = InkPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun CategorySpendingChartCard(
    hasSufficientData: Boolean,
    modelProducer: CartesianChartModelProducer,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Spending by category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (!hasSufficientData) {
                ChartEmptyState(
                    icon = Icons.Outlined.BarChart,
                    title = "Not enough data yet",
                    subtitle = "Add transactions across categories to view spending breakdown"
                )
            } else {
                CartesianChartHost(
                    chart = rememberCartesianChart(rememberColumnCartesianLayer()),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .height(190.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DailyTrendChartCard(
    hasSufficientData: Boolean,
    modelProducer: CartesianChartModelProducer,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Daily spending trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (!hasSufficientData) {
                ChartEmptyState(
                    icon = Icons.Outlined.ShowChart,
                    title = "Not enough data yet",
                    subtitle = "Add a few more transactions across dates to see trends"
                )
            } else {
                CartesianChartHost(
                    chart = rememberCartesianChart(rememberLineCartesianLayer()),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .height(190.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}
