package com.expensevault.feature.debts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.DebtDirection
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
fun PersonDetailScreen(
    personId: Long,
    onNavigateBack: () -> Unit,
    onAddDebtForPerson: (Long) -> Unit,
    onEditDebt: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PersonDetailViewModel = koinViewModel()
) {
    LaunchedEffect(personId) {
        viewModel.setPersonId(personId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        try {
            currency = java.util.Currency.getInstance("INR")
        } catch (_: Exception) {}
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Settle dialog
    if (uiState.showSettleDialog) {
        var isFullSettle by remember { mutableStateOf(true) }
        var settleAmountString by remember { mutableStateOf(uiState.netBalance.abs().toPlainString()) }
        var settleNote by remember { mutableStateOf("Settled in cash/UPI") }
        val maxAmount = uiState.netBalance.abs()
        val parsedAmount = settleAmountString.toBigDecimalOrNull()
        val isAmountValid = isFullSettle || (parsedAmount != null && parsedAmount > BigDecimal.ZERO && parsedAmount <= maxAmount)

        AlertDialog(
            onDismissRequest = { viewModel.dismissSettleDialog() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Settle debts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose whether to settle the entire balance with ${uiState.person?.name ?: "this person"} or just a portion of it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )

                    // Full vs Portion toggle
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = NeutralMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isFullSettle) HeroBlack else Color.Transparent)
                                    .clickable { isFullSettle = true }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Full (${currencyFormat.format(maxAmount)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isFullSettle) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isFullSettle) NeutralBg else InkSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (!isFullSettle) HeroBlack else Color.Transparent)
                                    .clickable { isFullSettle = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Portion of it",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!isFullSettle) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (!isFullSettle) NeutralBg else InkSecondary
                                )
                            }
                        }
                    }

                    if (!isFullSettle) {
                        OutlinedTextField(
                            value = settleAmountString,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    settleAmountString = it
                                }
                            },
                            placeholder = { Text("Enter portion amount", color = InkTertiary) },
                            prefix = { Text("₹ ", color = InkSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NeutralCard,
                                unfocusedContainerColor = NeutralMuted,
                                focusedBorderColor = HeroBlack,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = InkPrimary,
                                unfocusedTextColor = InkPrimary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = settleNote,
                        onValueChange = { settleNote = it },
                        placeholder = { Text("Note (optional)", color = InkTertiary) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NeutralCard,
                            unfocusedContainerColor = NeutralMuted,
                            focusedBorderColor = HeroBlack,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountToSettle = if (isFullSettle) null else parsedAmount
                        viewModel.settleDebts(settleNote.takeIf { it.isNotBlank() }, amountToSettle)
                    },
                    enabled = isAmountValid,
                    colors = ButtonDefaults.buttonColors(containerColor = HeroBlack, contentColor = NeutralBg),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Confirm settle", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSettleDialog() }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }

    if (uiState.showEditPersonDialog && uiState.person != null) {
        val person = uiState.person!!
        AddPersonDialog(
            initialName = person.name,
            initialPhone = person.phone ?: "",
            title = "Edit Person",
            confirmText = "Update",
            onDismiss = { viewModel.dismissEditPersonDialog() },
            onConfirm = { name, phone -> viewModel.updatePerson(name, phone) }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = NeutralBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.person?.name ?: "Person detail",
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
                actions = {
                    IconButton(onClick = { viewModel.showEditPersonDialog() }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit person",
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
                onClick = { onAddDebtForPerson(personId) },
                containerColor = HeroBlack,
                contentColor = NeutralBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add debt")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Person & Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralCard),
                    border = BorderStroke(1.dp, Hairline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(NeutralMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.person?.name?.take(1)?.uppercase() ?: "?",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = uiState.person?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )

                        uiState.person?.phone?.let { phone ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Hairline, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        val balance = uiState.netBalance
                        val (statusText, statusColor) = when {
                            balance > BigDecimal.ZERO -> "Owed to you" to MutedSage
                            balance < BigDecimal.ZERO -> "You owe" to MutedClay
                            else -> "All settled up" to InkSecondary
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(balance.abs()),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )

                        if (uiState.openDebts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { viewModel.showSettleDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = HeroBlack, contentColor = NeutralBg),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Settle up (${currencyFormat.format(balance.abs())})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Open Debts Section
            item {
                Text(
                    text = "Open debts (${uiState.openDebts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            }

            if (uiState.openDebts.isEmpty()) {
                item {
                    Text(
                        text = "No open debts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            } else {
                items(uiState.openDebts, key = { it.id }) { debt ->
                    Card(
                        onClick = { onEditDebt(debt.id) },
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
                                    text = debt.note ?: "Direct debt",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = InkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = debt.createdAt.toString().take(10),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }

                            val isOwed = debt.direction == DebtDirection.THEY_OWE_ME
                            val color = if (isOwed) MutedSage else MutedClay
                            val prefix = if (isOwed) "+ " else "- "

                            Text(
                                text = prefix + currencyFormat.format(debt.amount),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = color
                            )
                        }
                    }
                }
            }

            // Settlement History Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Settlement history (${uiState.settlements.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            }

            if (uiState.settlements.isEmpty()) {
                item {
                    Text(
                        text = "No past settlements recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            } else {
                items(uiState.settlements, key = { it.id }) { settlement ->
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
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MutedSage,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settlement.note ?: "Settlement",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = InkPrimary
                                )
                                Text(
                                    text = settlement.settledAt.toString().take(10),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                            Text(
                                text = currencyFormat.format(settlement.settledAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = InkPrimary
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}
