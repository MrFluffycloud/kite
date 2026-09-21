package com.expensevault.feature.recurring

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
import com.expensevault.core.model.RecurringFrequency
import org.koin.androidx.compose.koinViewModel

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
fun AddEditRecurringRuleScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditRecurringRuleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        modifier = modifier,
        containerColor = NeutralBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New recurring expense",
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
            // Amount
            OutlinedTextField(
                value = uiState.amountString,
                onValueChange = { viewModel.setAmount(it) },
                label = { Text("Amount", color = InkSecondary) },
                prefix = { Text("₹ ", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = InkSecondary) },
                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NeutralCard,
                    unfocusedContainerColor = NeutralCard,
                    focusedBorderColor = HeroBlack,
                    unfocusedBorderColor = Hairline
                )
            )

            // Note / Description
            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.setNote(it) },
                label = { Text("Description", color = InkSecondary) },
                placeholder = { Text("e.g. Netflix, Rent, Wifi, Gym", color = InkTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NeutralCard,
                    unfocusedContainerColor = NeutralCard,
                    focusedBorderColor = HeroBlack,
                    unfocusedBorderColor = Hairline,
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary
                )
            )

            // Account selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Charge to account",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.accounts, key = { it.id }) { account ->
                        val isSelected = uiState.selectedAccountId == account.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectAccount(account.id) },
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
                            label = { Text(account.name, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            // Category selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.categories, key = { it.id }) { category ->
                        val isSelected = uiState.selectedCategoryId == category.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(category.id) },
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
                            label = { Text(category.name, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            // Frequency
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Frequency",
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
                        val frequencies = listOf(
                            RecurringFrequency.WEEKLY to "Weekly",
                            RecurringFrequency.MONTHLY to "Monthly",
                            RecurringFrequency.CUSTOM to "Custom"
                        )
                        frequencies.forEach { (freq, label) ->
                            val isSelected = uiState.frequency == freq
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) HeroBlack else Color.Transparent)
                                    .clickable { viewModel.setFrequency(freq) }
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
            }

            // Frequency-specific parameters
            when (uiState.frequency) {
                RecurringFrequency.WEEKLY -> {
                    val days = listOf("Mon" to 1, "Tue" to 2, "Wed" to 3, "Thu" to 4, "Fri" to 5, "Sat" to 6, "Sun" to 7)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Repeats every",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = InkSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(days) { (name, num) ->
                                val isSelected = uiState.dayOfWeek == num
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setDayOfWeek(num) },
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
                                    label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
                RecurringFrequency.MONTHLY -> {
                    OutlinedTextField(
                        value = uiState.dayOfMonth.toString(),
                        onValueChange = { str ->
                            val num = str.toIntOrNull()
                            if (num != null && num in 1..31) {
                                viewModel.setDayOfMonth(num)
                            }
                        },
                        label = { Text("Day of the month (1-31)", color = InkSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NeutralCard,
                            unfocusedContainerColor = NeutralCard,
                            focusedBorderColor = HeroBlack,
                            unfocusedBorderColor = Hairline
                        )
                    )
                }
                RecurringFrequency.CUSTOM -> {
                    OutlinedTextField(
                        value = uiState.customIntervalDays,
                        onValueChange = { viewModel.setCustomIntervalDays(it) },
                        label = { Text("Interval (in days)", color = InkSecondary) },
                        placeholder = { Text("e.g. 14, 30, 90", color = InkTertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NeutralCard,
                            unfocusedContainerColor = NeutralCard,
                            focusedBorderColor = HeroBlack,
                            unfocusedBorderColor = Hairline
                        )
                    )
                }
            }

            // Require Confirmation Switch
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
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Require confirmation",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.requireConfirmation)
                                "App will ask for approval before logging"
                            else
                                "App will auto-log silently on schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    Switch(
                        checked = uiState.requireConfirmation,
                        onCheckedChange = { viewModel.setRequireConfirmation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeutralBg,
                            checkedTrackColor = HeroBlack,
                            uncheckedThumbColor = InkSecondary,
                            uncheckedTrackColor = NeutralMuted
                        )
                    )
                }
            }

            Button(
                onClick = { viewModel.saveRule() },
                enabled = !uiState.isSaving && uiState.amountString.isNotBlank(),
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = NeutralBg
                    )
                } else {
                    Text("Save recurring rule", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
