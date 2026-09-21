package com.expensevault.feature.vault

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.VaultExpense
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
private val HeroBlackSubtext = Color(0xFF8A8A87)
private val Hairline = Color(0xFFE7E6E3)
private val MutedClay = Color(0xFFB5533C)
private val MutedSage = Color(0xFF5C6E5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    onAddExpense: () -> Unit,
    onLocked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaultDashboardViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        try {
            currency = java.util.Currency.getInstance("INR")
        } catch (_: Exception) {}
    }

    // SECURITY: FLAG_SECURE prevents screenshots & recent apps thumbnail previews
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(uiState.isLocked) {
        if (uiState.isLocked) {
            onLocked()
        }
    }

    // Export dialog
    uiState.exportData?.let { csvData ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissExport() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Export vault records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Encrypted vault records formatted as CSV:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = csvData.take(300) + if (csvData.length > 300) "\n..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = InkPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csvData)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share vault CSV"))
                        viewModel.dismissExport()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeroBlack, contentColor = NeutralBg),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Share export", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExport() }) {
                    Text("Close", color = InkSecondary)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = NeutralBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Private vault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.lockVault()
                        onLocked()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit",
                            tint = InkPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportCsv() }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Export", tint = InkPrimary)
                    }
                    IconButton(onClick = {
                        viewModel.lockVault()
                        onLocked()
                    }) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock now", tint = InkPrimary)
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
                onClick = onAddExpense,
                containerColor = HeroBlack,
                contentColor = NeutralBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add private expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Overview Hero card (Max 1 emphasis card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = HeroBlack),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Total private spend",
                        style = MaterialTheme.typography.labelMedium,
                        color = HeroBlackSubtext
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currencyFormat.format(uiState.totalSpend),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = NeutralBg
                    )

                    if (uiState.partnerTotalSpend > java.math.BigDecimal.ZERO) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFF2A2A28), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Shared with partner",
                                style = MaterialTheme.typography.bodySmall,
                                color = HeroBlackSubtext
                            )
                            Text(
                                text = currencyFormat.format(uiState.partnerTotalSpend),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MutedSage
                            )
                        }
                    }
                }
            }

            // Tabs / Filters (Pill segmented switcher)
            Surface(
                shape = RoundedCornerShape(50),
                color = NeutralMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isAll = uiState.filter == VaultFilter.ALL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isAll) HeroBlack else Color.Transparent)
                            .clickable { viewModel.setFilter(VaultFilter.ALL) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All (${uiState.expenses.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isAll) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isAll) NeutralBg else InkSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (!isAll) HeroBlack else Color.Transparent)
                            .clickable { viewModel.setFilter(VaultFilter.PARTNER_ONLY) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Partner shared",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (!isAll) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!isAll) NeutralBg else InkSecondary
                        )
                    }
                }
            }

            // Expenses List
            if (uiState.expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(NeutralMuted, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = InkSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No private expenses yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Expenses logged here are encrypted with SQLCipher and concealed from standard views.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.expenses, key = { it.id }) { expense ->
                        VaultExpenseCard(
                            expense = expense,
                            currencyFormat = currencyFormat,
                            onDelete = { viewModel.deleteExpense(expense.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
fun VaultExpenseCard(
    expense: VaultExpense,
    currencyFormat: NumberFormat,
    onDelete: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = NeutralMuted
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                val note = expense.note
                if (!note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.date.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkTertiary
                    )
                    if (expense.isSharedWithPartner) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• Partner: ${currencyFormat.format(expense.partnerShare ?: expense.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSage,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = currencyFormat.format(expense.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MutedClay
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = InkTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
