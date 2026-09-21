package com.expensevault.feature.debts

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
import com.expensevault.core.model.DebtDirection
import com.expensevault.feature.debts.components.AddPersonDialog
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
fun AddDebtScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddDebtViewModel = koinViewModel()
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

    if (uiState.showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { viewModel.dismissAddPersonDialog() },
            onConfirm = { name, phone -> viewModel.createAndSelectPerson(name, phone) }
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
                        text = "Record debt",
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
            // Direction Selector (Pill Segmented Switcher)
            Surface(
                shape = RoundedCornerShape(50),
                color = NeutralMuted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isTheyOweMe = uiState.direction == DebtDirection.THEY_OWE_ME
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isTheyOweMe) HeroBlack else Color.Transparent)
                            .clickable { viewModel.setDirection(DebtDirection.THEY_OWE_ME) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "They owe me",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isTheyOweMe) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isTheyOweMe) NeutralBg else InkSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (!isTheyOweMe) HeroBlack else Color.Transparent)
                            .clickable { viewModel.setDirection(DebtDirection.I_OWE_THEM) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "I owe them",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (!isTheyOweMe) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!isTheyOweMe) NeutralBg else InkSecondary
                        )
                    }
                }
            }

            // Person Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Person",
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
                            label = { Text("+ New person", style = MaterialTheme.typography.labelMedium) }
                        )
                    }

                    items(uiState.persons, key = { it.id }) { person ->
                        val isSelected = uiState.selectedPersonId == person.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectPerson(person.id) },
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

            // Amount Input
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

            // Note Input
            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.setNote(it) },
                label = { Text("Note / Description (optional)", color = InkSecondary) },
                placeholder = { Text("e.g. Lunch, Groceries, Movie", color = InkTertiary) },
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

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveDebt() },
                enabled = !uiState.isSaving && uiState.selectedPersonId != null && uiState.amountString.isNotBlank(),
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
                    Text("Save debt", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
