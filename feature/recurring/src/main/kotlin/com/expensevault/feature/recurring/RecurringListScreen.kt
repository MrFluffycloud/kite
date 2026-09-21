package com.expensevault.feature.recurring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.RecurringFrequency
import org.koin.androidx.compose.koinViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    onNavigateBack: () -> Unit,
    onAddRule: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        try {
            currency = java.util.Currency.getInstance("INR")
        } catch (_: Exception) {}
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(uiState.autoLoggedMessage) {
        uiState.autoLoggedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = NeutralBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recurring expenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = InkPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeutralBg,
                    titleContentColor = InkPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRule,
                containerColor = HeroBlack,
                contentColor = NeutralBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add recurring expense")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pending Confirmations Section
            if (uiState.pendingConfirmations.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralCard),
                        border = BorderStroke(1.dp, Hairline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(NeutralMuted, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = InkPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Requires confirmation (${uiState.pendingConfirmations.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            uiState.pendingConfirmations.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.rule.note ?: "Recurring payment",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = InkPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currencyFormat.format(item.rule.amount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkSecondary
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.skipPendingRule(item.rule) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            border = BorderStroke(1.dp, Hairline)
                                        ) {
                                            Text("Skip", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                                        }
                                        Button(
                                            onClick = { viewModel.confirmPendingRule(item.rule) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = HeroBlack,
                                                contentColor = NeutralBg
                                            )
                                        ) {
                                            Text("Log", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Active Rules Section
            item {
                Text(
                    text = "Active rules (${uiState.activeRules.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            }

            if (uiState.activeRules.isEmpty()) {
                item {
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
                                imageVector = Icons.Outlined.EventRepeat,
                                contentDescription = null,
                                tint = InkTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No recurring expenses",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(uiState.activeRules, key = { _, it -> it.rule.id }) { index, item ->
                    Box(modifier = Modifier.staggeredEntry(index = index, reduceMotion = reduceMotion)) {
                        RecurringRuleCard(
                            item = item,
                            currencyFormat = currencyFormat,
                            onToggleActive = { viewModel.toggleRuleActive(item.rule) },
                            onDelete = { viewModel.deleteRule(item.rule) }
                        )
                    }
                }
            }

            // Paused Rules Section
            if (uiState.pausedRules.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paused rules (${uiState.pausedRules.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkSecondary
                    )
                }

                itemsIndexed(uiState.pausedRules, key = { _, it -> it.rule.id }) { index, item ->
                    Box(modifier = Modifier.staggeredEntry(index = index, reduceMotion = reduceMotion)) {
                        RecurringRuleCard(
                            item = item,
                            currencyFormat = currencyFormat,
                            onToggleActive = { viewModel.toggleRuleActive(item.rule) },
                            onDelete = { viewModel.deleteRule(item.rule) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun RecurringRuleCard(
    item: RecurringRuleWithDetails,
    currencyFormat: NumberFormat,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.rule.note ?: "Recurring expense",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (item.rule.isActive) InkPrimary else InkSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val freqText = when (item.rule.frequency) {
                        RecurringFrequency.WEEKLY -> "Weekly"
                        RecurringFrequency.MONTHLY -> "Monthly"
                        RecurringFrequency.CUSTOM -> "Every ${item.rule.customIntervalDays ?: 30} days"
                    }
                    Text(
                        text = "$freqText • ${item.accountName ?: "Account"} • ${item.categoryName ?: "Category"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                Text(
                    text = currencyFormat.format(item.rule.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.rule.isActive) InkPrimary else InkTertiary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Hairline, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = InkTertiary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Next: ${item.rule.nextOccurrence}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = item.rule.isActive,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeutralBg,
                            checkedTrackColor = HeroBlack,
                            uncheckedThumbColor = InkSecondary,
                            uncheckedTrackColor = NeutralMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete rule",
                            tint = InkTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
