package com.expensevault.feature.debts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.SplitMethod
import com.expensevault.feature.debts.components.AddPersonDialog
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
private val MutedSage = Color(0xFF5C6E5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplitExpenseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        try {
            currency = java.util.Currency.getInstance("INR")
        } catch (_: Exception) {}
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var showTxDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { viewModel.dismissAddPersonDialog() },
            onConfirm = { name, phone -> viewModel.addPerson(name, phone) }
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
                        text = "Split expense",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selected Transaction Card
            Card(
                onClick = { showTxDropdown = true },
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
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(NeutralMuted, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Receipt,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.selectedTransaction?.merchant
                                ?: uiState.selectedTransaction?.note
                                ?: "Select a transaction",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                        uiState.selectedTransaction?.let { tx ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${tx.transactionDate} • ${tx.originalCurrency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                    }
                    uiState.selectedTransaction?.let { tx ->
                        Text(
                            text = currencyFormat.format(tx.originalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                    }
                }

                DropdownMenu(
                    expanded = showTxDropdown,
                    onDismissRequest = { showTxDropdown = false }
                ) {
                    uiState.transactions.take(15).forEach { tx ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = tx.merchant ?: tx.note ?: "Expense",
                                        fontWeight = FontWeight.Medium,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = "${currencyFormat.format(tx.originalAmount)} • ${tx.transactionDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InkSecondary
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectTransaction(tx)
                                showTxDropdown = false
                            }
                        )
                    }
                }
            }

            // Split Amount Base: Full Amount vs Portion of it
            uiState.selectedTransaction?.let { tx ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Amount to split",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )

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
                                    .background(if (uiState.isFullAmount) HeroBlack else Color.Transparent)
                                    .clickable { viewModel.setIsFullAmount(true) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Full (${currencyFormat.format(tx.originalAmount)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (uiState.isFullAmount) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (uiState.isFullAmount) NeutralBg else InkSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (!uiState.isFullAmount) HeroBlack else Color.Transparent)
                                    .clickable { viewModel.setIsFullAmount(false) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Portion of it",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!uiState.isFullAmount) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (!uiState.isFullAmount) NeutralBg else InkSecondary
                                )
                            }
                        }
                    }

                    if (!uiState.isFullAmount) {
                        OutlinedTextField(
                            value = uiState.portionAmountString,
                            onValueChange = { viewModel.setPortionAmount(it) },
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
                }
            }

            // Participants Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Split with (${uiState.selectedPersonIds.size} friends + you)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.showAddPersonDialog() },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NeutralCard,
                                labelColor = InkPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = Hairline,
                                borderWidth = 1.dp
                            ),
                            label = { Text("+ Add person", style = MaterialTheme.typography.labelMedium) }
                        )
                    }

                    items(uiState.persons, key = { it.id }) { person ->
                        val isSelected = uiState.selectedPersonIds.contains(person.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.togglePerson(person.id) },
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
                                    text = person.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Split Method Selector (Pill segmented control)
            Surface(
                shape = RoundedCornerShape(50),
                color = NeutralMuted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val methods = listOf(
                        SplitMethod.EQUAL to "Equally",
                        SplitMethod.CUSTOM_AMOUNT to "Exact",
                        SplitMethod.PERCENTAGE to "%"
                    )
                    methods.forEach { (method, label) ->
                        val isSelected = uiState.splitMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) HeroBlack else Color.Transparent)
                            .clickable { viewModel.setSplitMethod(method) }
                            .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) NeutralBg else InkSecondary
                            )
                        }
                    }
                }
            }

            // Shares Breakdown
            if (uiState.selectedPersonIds.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralCard),
                    border = BorderStroke(1.dp, Hairline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Split breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )

                        uiState.selectedPersonIds.forEach { personId ->
                            val person = uiState.persons.find { it.id == personId } ?: return@forEach
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = InkPrimary,
                                    modifier = Modifier.weight(1f)
                                )

                                when (uiState.splitMethod) {
                                    SplitMethod.EQUAL -> {
                                        val share = viewModel.calculateShareForPerson(personId)
                                        Text(
                                            text = "owes " + currencyFormat.format(share),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MutedSage
                                        )
                                    }
                                    SplitMethod.CUSTOM_AMOUNT -> {
                                        OutlinedTextField(
                                            value = uiState.customAmounts[personId] ?: "",
                                            onValueChange = { viewModel.setCustomAmount(personId, it) },
                                            placeholder = { Text("0.00", color = InkTertiary) },
                                            prefix = { Text("₹ ", color = InkSecondary) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = NeutralCard,
                                                unfocusedContainerColor = NeutralMuted,
                                                focusedBorderColor = HeroBlack,
                                                unfocusedBorderColor = Color.Transparent
                                            ),
                                            modifier = Modifier.width(130.dp),
                                            singleLine = true
                                        )
                                    }
                                    SplitMethod.PERCENTAGE -> {
                                        OutlinedTextField(
                                            value = uiState.customPercentages[personId] ?: "",
                                            onValueChange = { viewModel.setCustomPercentage(personId, it) },
                                            placeholder = { Text("0", color = InkTertiary) },
                                            suffix = { Text("%", color = InkSecondary) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = NeutralCard,
                                                unfocusedContainerColor = NeutralMuted,
                                                focusedBorderColor = HeroBlack,
                                                unfocusedBorderColor = Color.Transparent
                                            ),
                                            modifier = Modifier.width(110.dp),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.saveSplit() },
                enabled = !uiState.isSaving && uiState.selectedTransaction != null && uiState.selectedPersonIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HeroBlack,
                    contentColor = NeutralBg,
                    disabledContainerColor = NeutralMuted,
                    disabledContentColor = InkTertiary
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NeutralBg)
                } else {
                    Text("Confirm split & create debts", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
