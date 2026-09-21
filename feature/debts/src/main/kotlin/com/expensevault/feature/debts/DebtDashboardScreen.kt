package com.expensevault.feature.debts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.DebtDirection
import com.expensevault.core.model.PersonDebtSummary
import com.expensevault.feature.debts.components.AddPersonDialog
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// Strict Design Tokens
private val NeutralBg = Color(0xFFFAFAF9)
private val NeutralCard = Color(0xFFFFFFFF)
private val NeutralMuted = Color(0xFFF0F0EE)
private val InkPrimary = Color(0xFF111111)
private val InkSecondary = Color(0xFF6B6B68)
private val InkTertiary = Color(0xFFB8B8B5)
private val HeroBlack = Color(0xFF0F0F0F)
private val Hairline = Color(0xFFE7E6E3)
private val MutedClay = Color(0xFFB5533C)
private val MutedSage = Color(0xFF5C6E5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDashboardScreen(
    onNavigateBack: () -> Unit = {},
    onPersonClick: (Long) -> Unit,
    onAddDebtClick: () -> Unit,
    onSplitExpenseClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        try {
            currency = java.util.Currency.getInstance("INR")
        } catch (_: Exception) {}
    }

    Scaffold(
        modifier = modifier,
        containerColor = NeutralBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Debts & splits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeutralBg,
                    titleContentColor = InkPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.showAddPersonDialog() }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add person", tint = InkPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSplitExpenseClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeutralCard,
                        contentColor = InkPrimary
                    ),
                    border = BorderStroke(1.dp, Hairline),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Split expense",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FloatingActionButton(
                    onClick = onAddDebtClick,
                    containerColor = HeroBlack,
                    contentColor = NeutralBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add debt")
                }
            }
        }
    ) { innerPadding ->
        if (uiState.showAddPersonDialog) {
            AddPersonDialog(
                onDismiss = { viewModel.dismissAddPersonDialog() },
                onConfirm = { name, phone -> viewModel.addPerson(name, phone) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val reduceMotion = rememberReduceMotion()
            val animNet by animateFloatAsState(
                targetValue = uiState.netBalance.toFloat(),
                animationSpec = if (reduceMotion) snap() else tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "NetBalanceAnim"
            )
            val animOwedToYou by animateFloatAsState(
                targetValue = uiState.totalOwedToYou.toFloat(),
                animationSpec = if (reduceMotion) snap() else tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "OwedToYouAnim"
            )
            val animYouOwe by animateFloatAsState(
                targetValue = uiState.totalYouOwe.toFloat(),
                animationSpec = if (reduceMotion) snap() else tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "YouOweAnim"
            )
            val netDisplay = if (reduceMotion) uiState.netBalance else java.math.BigDecimal.valueOf(animNet.toDouble())
            val owedToYouDisplay = if (reduceMotion) uiState.totalOwedToYou else java.math.BigDecimal.valueOf(animOwedToYou.toDouble())
            val youOweDisplay = if (reduceMotion) uiState.totalYouOwe else java.math.BigDecimal.valueOf(animYouOwe.toDouble())

            // Net balance overview card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Net balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val netColor = when {
                        netDisplay > BigDecimal.ZERO -> MutedSage
                        netDisplay < BigDecimal.ZERO -> MutedClay
                        else -> InkPrimary
                    }
                    val netPrefix = if (netDisplay > BigDecimal.ZERO) "+" else ""
                    Text(
                        text = "$netPrefix${currencyFormat.format(netDisplay)}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = netColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Hairline, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Owed to you",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currencyFormat.format(owedToYouDisplay),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MutedSage
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "You owe",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currencyFormat.format(youOweDisplay),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MutedClay
                            )
                        }
                    }
                }
            }

            // Segmented pill tab switcher
            Surface(
                shape = RoundedCornerShape(50),
                color = NeutralMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.padding(3.dp)) {
                    val tabWidth = maxWidth / 2
                    val isPeople = uiState.selectedTab == 0
                    val indicatorOffset by animateDpAsState(
                        targetValue = if (isPeople) 0.dp else tabWidth,
                        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.82f, stiffness = 400f),
                        label = "DebtTabOffset"
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(HeroBlack)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.selectTab(0) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "People (${uiState.personSummaries.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isPeople) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isPeople) NeutralBg else InkSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.selectTab(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Open debts (${uiState.openDebts.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (!isPeople) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!isPeople) NeutralBg else InkSecondary
                            )
                        }
                    }
                }
            }

            // Tab contents
            when (uiState.selectedTab) {
                0 -> {
                    if (uiState.personSummaries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fadeInEntry(reduceMotion),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                                border = BorderStroke(1.dp, Hairline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp, horizontal = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PeopleOutline,
                                        contentDescription = null,
                                        tint = InkTertiary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No people added yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 130.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(uiState.personSummaries, key = { _, it -> it.person.id }) { index, summary ->
                                Box(modifier = Modifier.staggeredEntry(index = index, reduceMotion = reduceMotion)) {
                                    PersonSummaryCard(
                                        summary = summary,
                                        currencyFormat = currencyFormat,
                                        onClick = { onPersonClick(summary.person.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (uiState.openDebts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fadeInEntry(reduceMotion),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                                border = BorderStroke(1.dp, Hairline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp, horizontal = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = InkTertiary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "All balances settled",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 130.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(uiState.openDebts, key = { _, it -> it.debt.id }) { index, debtItem ->
                                Box(modifier = Modifier.staggeredEntry(index = index, reduceMotion = reduceMotion)) {
                                    OpenDebtCard(
                                        item = debtItem,
                                        currencyFormat = currencyFormat
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonSummaryCard(
    summary: PersonDebtSummary,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initial Avatar (Neutral circle #F0F0EE)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NeutralMuted),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = summary.person.name.take(1).uppercase(),
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                val statusText = when {
                    summary.netBalance > BigDecimal.ZERO -> "Owes you (${summary.openDebtsCount} debts)"
                    summary.netBalance < BigDecimal.ZERO -> "You owe (${summary.openDebtsCount} debts)"
                    else -> "Settled up"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }

            val amountColor = when {
                summary.netBalance > BigDecimal.ZERO -> MutedSage
                summary.netBalance < BigDecimal.ZERO -> MutedClay
                else -> InkTertiary
            }

            Text(
                text = currencyFormat.format(summary.netBalance.abs()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = InkTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun OpenDebtCard(
    item: OpenDebtItem,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.personName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.debt.note ?: "Direct debt",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }

            val isOwedToMe = item.debt.direction == DebtDirection.THEY_OWE_ME
            val color = if (isOwedToMe) MutedSage else MutedClay
            val prefix = if (isOwedToMe) "+ " else "- "

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = prefix + currencyFormat.format(item.debt.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Text(
                    text = if (isOwedToMe) "Owes you" else "You owe",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}
