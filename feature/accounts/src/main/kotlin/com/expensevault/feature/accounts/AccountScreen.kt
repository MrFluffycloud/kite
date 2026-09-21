package com.expensevault.feature.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensevault.core.model.Account
import com.expensevault.core.model.AccountType
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Currency
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            try {
                currency = Currency.getInstance("INR")
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        containerColor = NeutralBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Accounts",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showDialog() },
                containerColor = HeroBlack,
                contentColor = NeutralBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 76.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add account")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Total Balance Hero Card (Max 1 Emphasis Card)
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
                        text = "Total balance across accounts",
                        style = MaterialTheme.typography.labelMedium,
                        color = HeroBlackSubtext
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val totalFormatted = remember(uiState.totalBalance) {
                        try {
                            formatter.format(uiState.totalBalance)
                        } catch (e: Exception) {
                            uiState.totalBalance.toString()
                        }
                    }
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = NeutralBg
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.accounts, key = { it.id }) { account ->
                    var showAccountOptions by remember { mutableStateOf(false) }
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    val isDefault = account.id == uiState.defaultAccountId

                    AccountCard(
                        account = account,
                        isDefault = isDefault,
                        formatter = formatter,
                        onClick = { showAccountOptions = true },
                        onLongClick = { showAccountOptions = true }
                    )

                    if (showAccountOptions) {
                        AlertDialog(
                            onDismissRequest = { showAccountOptions = false },
                            containerColor = NeutralCard,
                            shape = RoundedCornerShape(24.dp),
                            title = {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkPrimary
                                )
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (!isDefault) {
                                        Surface(
                                            onClick = {
                                                viewModel.setDefaultAccount(account.id)
                                                showAccountOptions = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = NeutralMuted,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Set as default wallet",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = InkPrimary
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            showAccountOptions = false
                                            viewModel.showDialog(account)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Edit account",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = InkPrimary
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            showAccountOptions = false
                                            showDeleteConfirm = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Delete account",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MutedClay
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { showAccountOptions = false }) {
                                    Text("Close", color = InkSecondary)
                                }
                            }
                        )
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            containerColor = NeutralCard,
                            shape = RoundedCornerShape(24.dp),
                            title = {
                                Text(
                                    text = "Delete account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkPrimary
                                )
                            },
                            text = {
                                Text(
                                    text = "Are you sure you want to delete ${account.name}? This action cannot be undone.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteAccount(account.id)
                                        showDeleteConfirm = false
                                    }
                                ) {
                                    Text("Delete", color = MutedClay, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel", color = InkSecondary)
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (uiState.showAddDialog) {
        AccountBottomSheet(
            uiState = uiState,
            onDismiss = { viewModel.dismissDialog() },
            onSave = { viewModel.saveAccount() },
            onNameChange = { viewModel.updateFormName(it) },
            onTypeChange = { viewModel.updateFormType(it) },
            onCurrencyChange = { viewModel.updateFormCurrency(it) },
            onBalanceChange = { viewModel.updateFormInitialBalance(it) },
            onColorChange = { viewModel.updateFormColorHex(it) }
        )
    }

    if (uiState.deleteError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteError() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Error", color = InkPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text(uiState.deleteError!!, color = InkSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDeleteError() }) {
                    Text("OK", color = HeroBlack, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
fun AccountCard(
    account: Account,
    isDefault: Boolean = false,
    formatter: NumberFormat,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val accountIcon: ImageVector = when (account.type) {
                AccountType.BANK_ACCOUNT -> Icons.Default.AccountBalance
                AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                AccountType.DEBIT_CARD -> Icons.Default.CreditCard
                AccountType.CASH -> Icons.Default.Payments
                AccountType.WALLET -> Icons.Default.Wallet
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(NeutralMuted, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = accountIcon,
                    contentDescription = null,
                    tint = InkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = InkPrimary
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = HeroBlack
                        ) {
                            Text(
                                text = "default",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFFFAFAF9),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (account.isArchived) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = NeutralMuted
                        ) {
                            Text(
                                text = "archived",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = InkSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = account.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }

            val balanceStr = try {
                formatter.currency = Currency.getInstance(account.defaultCurrency)
                formatter.format(account.currentBalance)
            } catch (e: Exception) {
                "${account.currentBalance} ${account.defaultCurrency}"
            }
            Text(
                text = balanceStr,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBottomSheet(
    uiState: AccountUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onColorChange: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeutralCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (uiState.editingAccount == null) "Add account" else "Edit account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = uiState.formName,
                onValueChange = onNameChange,
                label = { Text("Account name", color = InkSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
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
            Spacer(modifier = Modifier.height(14.dp))

            Text("Account type", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.formType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeutralCard,
                        unfocusedContainerColor = NeutralMuted,
                        focusedBorderColor = HeroBlack,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AccountType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onTypeChange(type)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = uiState.formCurrency,
                    onValueChange = onCurrencyChange,
                    label = { Text("Currency", color = InkSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeutralCard,
                        unfocusedContainerColor = NeutralMuted,
                        focusedBorderColor = HeroBlack,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.formInitialBalance,
                    onValueChange = onBalanceChange,
                    label = { Text("Initial balance", color = InkSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeutralCard,
                        unfocusedContainerColor = NeutralMuted,
                        focusedBorderColor = HeroBlack,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = uiState.editingAccount == null
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = InkSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSave,
                    enabled = uiState.formName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeroBlack,
                        contentColor = NeutralBg,
                        disabledContainerColor = NeutralMuted,
                        disabledContentColor = InkTertiary
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
