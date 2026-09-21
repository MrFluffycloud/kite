package com.expensevault.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.domain.model.CategorySpendDisplay
import com.expensevault.feature.transactions.TransactionItem
import com.expensevault.feature.transactions.getCategoryOutlineIcon
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat

// Strict Neutral Design Tokens
private val NeutralBg = Color(0xFFFAFAF9)
private val NeutralCard = Color(0xFFFFFFFF)
private val NeutralMuted = Color(0xFFF0F0EE)
private val InkPrimary = Color(0xFF111111)
private val InkSecondary = Color(0xFF6B6B68)
private val InkTertiary = Color(0xFFB8B8B5)
private val HeroBlack = Color(0xFF0F0F0F)
private val HeroBlackSubtext = Color(0xFF8A8A87)
private val Hairline = Color(0xFFE7E6E3)
private val MutedSage = Color(0xFF5C6E5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToDebts: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formatter = remember {
        NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN")).apply {
            try {
                currency = java.util.Currency.getInstance("INR")
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeutralBg,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Surface(
                color = NeutralBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NeutralBg,
                        titleContentColor = InkPrimary
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = InkPrimary
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = HeroBlack)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .clipToBounds(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Emphasis Hero Card (Max 1 per screen)
                item {
                    HeaderCard(
                        monthlyTotal = uiState.monthlyTotal,
                        currentMonthLabel = uiState.currentMonthLabel,
                        dailySpends = uiState.dailySpendsLast7Days,
                        formatter = formatter
                    )
                }

                // Quick stats
                item {
                    QuickStatsRow(
                        todayTotal = uiState.todayTotal,
                        weeklyTotal = uiState.weeklyTotal,
                        monthCount = uiState.monthTransactionCount,
                        formatter = formatter
                    )
                }

                // Top Spending Categories
                if (uiState.topCategories.isNotEmpty()) {
                    item {
                        TopCategoriesSection(
                            categories = uiState.topCategories,
                            formatter = formatter
                        )
                    }
                }

                // Debts & Split Entry Card
                item {
                    Card(
                        onClick = onNavigateToDebts,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralCard),
                        border = BorderStroke(1.dp, Hairline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(NeutralMuted, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Group,
                                        contentDescription = null,
                                        tint = InkPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Debts & split expenses",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Track shared balances and settlements",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = InkTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Recurring Bills Card
                item {
                    Card(
                        onClick = onNavigateToRecurring,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralCard),
                        border = BorderStroke(1.dp, Hairline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(NeutralMuted, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Repeat,
                                        contentDescription = null,
                                        tint = InkPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Recurring expenses",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Subscriptions, rent, and scheduled bills",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = InkTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Recent Transactions
                item {
                    RecentTransactionsSection(
                        transactions = uiState.recentTransactions,
                        onSeeAllClick = onNavigateToTransactions
                    )
                }

                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }
    }
}

@Composable
fun HeaderCard(
    monthlyTotal: java.math.BigDecimal,
    currentMonthLabel: String,
    dailySpends: List<Float> = emptyList(),
    formatter: NumberFormat
) {
    val reduceMotion = rememberReduceMotion()
    var startAnim by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) {
        startAnim = true
    }
    val animatedTotal by animateFloatAsState(
        targetValue = if (startAnim) monthlyTotal.toFloat() else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "MonthlyTotalCountUp"
    )
    val displayAmount = if (reduceMotion) {
        formatter.format(monthlyTotal)
    } else {
        try {
            formatter.format(java.math.BigDecimal.valueOf(animatedTotal.toDouble()))
        } catch (e: Exception) {
            formatter.format(monthlyTotal)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HeroBlack),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Spent this month",
                style = MaterialTheme.typography.labelMedium,
                color = HeroBlackSubtext
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = displayAmount,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                fontWeight = FontWeight.SemiBold,
                color = NeutralBg
            )

            // Minimal 7-day spend sparkline
            Spacer(modifier = Modifier.height(14.dp))
            SpendSparkline(
                values = dailySpends,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MutedSage, shape = RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentMonthLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkTertiary
                )
            }
        }
    }
}

@Composable
fun SpendSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (values.isEmpty()) return@Canvas

        val maxVal = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val points = values.mapIndexed { index, value ->
            val x = (index.toFloat() / (values.size - 1).coerceAtLeast(1)) * w
            val normalized = (value / maxVal).coerceIn(0f, 1f)
            val y = h - (normalized * (h - 8.dp.toPx())) - 4.dp.toPx()
            Offset(x, y)
        }

        if (points.size > 1) {
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx = (prev.x + curr.x) / 2
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }

            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(points.last().x, h)
                lineTo(points.first().x, h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = strokePath,
                color = Color.White.copy(alpha = 0.70f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
fun QuickStatsRow(
    todayTotal: java.math.BigDecimal,
    weeklyTotal: java.math.BigDecimal,
    monthCount: Int,
    formatter: NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Today",
            value = formatter.format(todayTotal)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "This week",
            value = formatter.format(weeklyTotal)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Transactions",
            value = monthCount.toString()
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
        }
    }
}

@Composable
fun TopCategoriesSection(
    categories: List<CategorySpendDisplay>,
    formatter: NumberFormat
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
                text = "Top categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            categories.forEachIndexed { index, cat ->
                CategoryItemRow(cat = cat, formatter = formatter)
                if (index < categories.size - 1) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryItemRow(cat: CategorySpendDisplay, formatter: NumberFormat) {
    val reduceMotion = rememberReduceMotion()
    var startAnim by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) {
        startAnim = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnim) cat.percentage else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "CatProgressAnim"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(NeutralMuted, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryOutlineIcon(cat.name, null),
                        contentDescription = null,
                        tint = InkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = cat.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = InkPrimary
                )
            }
            Text(
                text = formatter.format(cat.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Monochrome progress meter
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = HeroBlack,
            trackColor = NeutralMuted
        )
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<com.expensevault.feature.transactions.TransactionDisplayItem>,
    onSeeAllClick: () -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary,
                modifier = Modifier
                    .clickable { onSeeAllClick() }
                    .padding(4.dp)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NeutralCard),
            border = BorderStroke(1.dp, Hairline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                if (transactions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadeInEntry(reduceMotion)
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = InkTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No recent transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    }
                } else {
                    transactions.forEachIndexed { index, item ->
                        TransactionItem(item = item, onClick = onSeeAllClick)
                        if (index < transactions.size - 1) {
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
}
