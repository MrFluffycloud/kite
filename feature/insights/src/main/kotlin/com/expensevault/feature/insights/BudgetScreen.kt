package com.expensevault.feature.insights

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.domain.usecase.BudgetStatus
import com.expensevault.core.model.FixedCommitment
import com.expensevault.core.model.WeeklyBudgetSummary
import org.koin.androidx.compose.koinViewModel

// Strict Design Tokens
private val NeutralBg = Color(0xFFFAFAF9)
private val NeutralCard = Color(0xFFFFFFFF)
private val NeutralMuted = Color(0xFFF0F0EE)
private val InkPrimary = Color(0xFF111111)
private val InkSecondary = Color(0xFF6B6B68)
private val HeroBlack = Color(0xFF0F0F0F)
private val Hairline = Color(0xFFE7E6E3)
private val MutedClay = Color(0xFFB5533C)
private val MutedSage = Color(0xFF5C6E5A)

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = uiState.weeklyConfig.currencySymbol

    Scaffold(
        containerColor = NeutralBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddFixedCommitmentSheet() },
                containerColor = HeroBlack,
                contentColor = NeutralBg,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Need", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Weekly Allowance Hero Card
            item {
                WeeklyAllowanceHeroCard(
                    summary = uiState.weeklySummary,
                    currencySymbol = currencySymbol,
                    onEditClick = { viewModel.showEditAllowanceSheet() }
                )
            }

            // 2. Fixed Commitments / Travel Section
            item {
                FixedNeedsSection(
                    commitments = uiState.weeklySummary?.fixedCommitments ?: uiState.weeklyConfig.fixedCommitments,
                    currencySymbol = currencySymbol,
                    onAddClick = { viewModel.showAddFixedCommitmentSheet() },
                    onToggleFulfilled = { viewModel.toggleCommitmentFulfilled(it) },
                    onDelete = { viewModel.removeFixedCommitment(it) }
                )
            }

            // 3. Category Budgets Header & Cards
            if (uiState.budgetStatuses.isNotEmpty()) {
                item {
                    Text(
                        text = "Category Budgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(uiState.budgetStatuses) { status ->
                    BudgetCard(status = status, currencySymbol = currencySymbol)
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        // Sheets
        if (uiState.showEditAllowanceSheet) {
            EditAllowanceBottomSheet(
                currentAllowance = uiState.weeklyConfig.totalAllowance,
                currentSymbol = currencySymbol,
                onDismiss = { viewModel.hideEditAllowanceSheet() },
                onSave = { amount, symbol ->
                    viewModel.updateWeeklyAllowance(amount, symbol)
                }
            )
        }

        if (uiState.showAddFixedCommitmentSheet) {
            AddFixedCommitmentBottomSheet(
                currencySymbol = currencySymbol,
                onDismiss = { viewModel.hideAddFixedCommitmentSheet() },
                onSave = { name, amount ->
                    viewModel.addFixedCommitment(name, amount)
                }
            )
        }
    }
}

@Composable
private fun WeeklyAllowanceHeroCard(
    summary: WeeklyBudgetSummary?,
    currencySymbol: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HeroBlack),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAFE TO SPEND TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8A8A87),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val safeAmount = summary?.safeToSpendToday?.toPlainString() ?: "0.00"
                    Text(
                        text = "$currencySymbol$safeAmount",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFAFAF9)
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF242424), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Allowance",
                        tint = Color(0xFFFAFAF9),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sub-metrics row: Days remaining badge & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val daysLeft = summary?.daysRemainingInWeek ?: 7
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E1E1E)
                ) {
                    Text(
                        text = "$daysLeft days left in week",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB8B8B5),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                val statusText = if (summary?.isOverBudget == true) "Over Budget" else "On Track"
                val statusColor = if (summary?.isOverBudget == true) MutedClay else MutedSage
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = "Total Allowance",
                    value = "$currencySymbol${summary?.totalAllowance?.toPlainString() ?: "0"}"
                )
                MetricColumn(
                    label = "Fixed Needs",
                    value = "$currencySymbol${summary?.totalFixedReserved?.toPlainString() ?: "0"}"
                )
                MetricColumn(
                    label = "Spent So Far",
                    value = "$currencySymbol${summary?.totalSpentThisWeek?.toPlainString() ?: "0"}"
                )
                MetricColumn(
                    label = "Remaining",
                    value = "$currencySymbol${summary?.discretionaryRemaining?.toPlainString() ?: "0"}"
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8A8A87)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFAFAF9)
        )
    }
}

@Composable
private fun FixedNeedsSection(
    commitments: List<FixedCommitment>,
    currencySymbol: String,
    onAddClick: () -> Unit,
    onToggleFulfilled: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Fixed Needs & Commitments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                Text(
                    text = "Reserved upfront (e.g. £55 train travel)",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
            TextButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Need", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (commitments.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsTransit,
                        contentDescription = null,
                        tint = InkSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "No fixed needs added yet. Tap \"Add Need\" to reserve money for train tickets, passes, or bills.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline)
            ) {
                Column {
                    commitments.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { onToggleFulfilled(item.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isFulfilled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (item.isFulfilled) "Fulfilled" else "Pending",
                                        tint = if (item.isFulfilled) MutedSage else InkSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (item.isFulfilled) InkSecondary else InkPrimary
                                    )
                                    Text(
                                        text = if (item.isFulfilled) "Paid this week" else "Reserved from budget",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.isFulfilled) MutedSage else InkSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$currencySymbol${item.amount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isFulfilled) InkSecondary else InkPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDelete(item.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = InkSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (index < commitments.size - 1) {
                            HorizontalDivider(color = Hairline, thickness = 0.8.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(status: BudgetStatus, currencySymbol: String) {
    val reduceMotion = rememberReduceMotion()
    val targetProgress = (status.spent.toFloat() / status.limit.toFloat()).coerceIn(0f, 1f)
    var startAnim by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) {
        startAnim = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnim) targetProgress else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "BudgetProgressAnim"
    )
    val progressColor = when {
        status.isExceeded -> MutedClay
        status.isWarning -> InkPrimary
        else -> HeroBlack
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                val statusText = when {
                    status.isExceeded -> "Exceeded"
                    status.isWarning -> "Near limit"
                    else -> "On track"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status.isExceeded) MutedClay else InkSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = progressColor,
                trackColor = NeutralMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            val percent = (targetProgress * 100).toInt()
            Text(
                text = "$currencySymbol${status.spent} / $currencySymbol${status.limit} ($percent%)",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAllowanceBottomSheet(
    currentAllowance: String,
    currentSymbol: String,
    onDismiss: () -> Unit,
    onSave: (amount: String, symbol: String) -> Unit
) {
    var amount by remember { mutableStateOf(if (currentAllowance == "0") "" else currentAllowance) }
    var selectedSymbol by remember { mutableStateOf(currentSymbol) }
    val symbols = remember(currentSymbol) {
        listOf(currentSymbol, "₹", "$", "€", "£").filter { it.isNotBlank() }.distinct()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeutralCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Weekly Allowance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = InkPrimary
            )
            Text(
                text = "Set your total weekly spending ceiling. Your daily safe-to-spend target and fixed commitments will adjust automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Currency Symbol row
            Text(
                text = "Currency Symbol",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                symbols.forEach { sym ->
                    FilterChip(
                        selected = sym == selectedSymbol,
                        onClick = { selectedSymbol = sym },
                        label = { Text(sym, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HeroBlack,
                            selectedLabelColor = NeutralBg,
                            containerColor = NeutralMuted,
                            labelColor = InkPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                label = { Text("Weekly Allowance Amount") },
                prefix = { Text("$selectedSymbol ", fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(amount.ifBlank { "0" }, selectedSymbol)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeroBlack)
            ) {
                Text("Save Allowance", fontWeight = FontWeight.SemiBold, color = NeutralBg)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFixedCommitmentBottomSheet(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val quickSuggestions = listOf("Train travel", "Bus pass", "Gym / Fitness", "Fuel", "Groceries run")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NeutralCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Add Fixed Need / Travel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = InkPrimary
            )
            Text(
                text = "Fixed needs (e.g. £55 train travel) are reserved upfront from your weekly allowance so you don't overspend your daily allowance.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Quick suggestions
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(quickSuggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { name = suggestion },
                        label = { Text(suggestion) },
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Commitment Description") },
                placeholder = { Text("e.g. Train there and back") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                label = { Text("Amount Needed") },
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.SemiBold) },
                placeholder = { Text("55.00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && amount.isNotBlank()) {
                        onSave(name.trim(), amount.trim())
                    }
                },
                enabled = name.isNotBlank() && amount.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeroBlack)
            ) {
                Text("Reserve Commitment", fontWeight = FontWeight.SemiBold, color = NeutralBg)
            }
        }
    }
}
