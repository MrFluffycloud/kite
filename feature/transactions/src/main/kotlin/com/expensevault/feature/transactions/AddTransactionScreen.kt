package com.expensevault.feature.transactions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.CategoryWithSubcategories
import com.expensevault.core.model.TransactionType
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
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    prefillAmount: String? = null,
    prefillMerchant: String? = null,
    prefillCategory: String? = null,
    viewModel: AddTransactionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(prefillAmount, prefillMerchant, prefillCategory, uiState.categories) {
        if (!prefillAmount.isNullOrBlank() || !prefillMerchant.isNullOrBlank() || !prefillCategory.isNullOrBlank()) {
            viewModel.prefill(prefillAmount, prefillMerchant, prefillCategory)
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            if (!reduceMotion) {
                delay(400)
            }
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorDismissed()
        }
    }

    Scaffold(
        containerColor = NeutralBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add transaction",
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper Section: Evenly composed with generous breathing room
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Type Segmented Switcher (Expense / Income)
                val isExpense = uiState.transactionType == TransactionType.EXPENSE
                Surface(
                    shape = RoundedCornerShape(50),
                    color = NeutralMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                ) {
                    Box(modifier = Modifier.padding(3.dp)) {
                        val tabWidth = 92.dp
                        val indicatorOffset by animateDpAsState(
                            targetValue = if (isExpense) 0.dp else tabWidth + 4.dp,
                            animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.82f, stiffness = 380f),
                            label = "TypeSwitchOffset"
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(tabWidth)
                                .height(36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(HeroBlack)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(tabWidth)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { viewModel.onTypeChange(TransactionType.EXPENSE) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Expense",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isExpense) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isExpense) NeutralBg else InkSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(tabWidth)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { viewModel.onTypeChange(TransactionType.INCOME) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!isExpense) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (!isExpense) NeutralBg else InkSecondary
                                )
                            }
                        }
                    }
                }

                // Prominent Centered Amount Display (Large 56sp with breathing room)
                val amountText = if (uiState.amountString.isEmpty()) "0" else uiState.amountString
                val currencySymbol = uiState.selectedCurrency.ifEmpty {
                    uiState.accounts.find { it.id == uiState.selectedAccountId }?.defaultCurrency ?: "₹"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currencySymbol,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = InkSecondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    AnimatedContent(
                        targetState = amountText,
                        transitionSpec = {
                            if (reduceMotion) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else if (targetState.length > initialState.length) {
                                (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.90f, animationSpec = tween(150)))
                                    .togetherWith(fadeOut(animationSpec = tween(90)))
                            } else {
                                fadeIn(animationSpec = tween(120)).togetherWith(fadeOut(animationSpec = tween(80)))
                            }
                        },
                        label = "AmountDigitAnimation"
                    ) { displayAmount ->
                        Text(
                            text = displayAmount,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                    }
                }

                // Horizontal Pill Category Selector
                CategorySelection(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    selectedParentCategoryId = uiState.selectedParentCategoryId,
                    onCategorySelect = viewModel::onCategorySelect
                )

                // Quick Select Amount Chips (Horizontal row)
                val quickAmounts = listOf(100, 200, 500, 1000, 2000, 5000)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickAmounts) { amt ->
                        Surface(
                            onClick = { viewModel.onQuickAmountSelect(amt) },
                            shape = RoundedCornerShape(50),
                            color = NeutralCard,
                            border = BorderStroke(1.dp, Hairline)
                        ) {
                            Text(
                                text = "+$amt",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = InkPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Expandable Details (Note, Account, Currency)
                MoreDetailsSection(
                    uiState = uiState,
                    onNoteChange = viewModel::onNoteChange,
                    onAccountSelect = viewModel::onAccountSelect,
                    onToggleMore = viewModel::onToggleMore,
                    onCurrencySelect = viewModel::onCurrencySelect,
                    onExchangeRateChange = viewModel::onExchangeRateChange
                )
            }

            // Bottom-Anchored Section: Save Action CTA + NumberPad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action Button (Filled Near-Black #111111 with Pure White Text)
                val isSaveEnabled = uiState.amountString.isNotEmpty() && uiState.selectedCategoryId != null && !uiState.isSaving
                Button(
                    onClick = viewModel::saveTransaction,
                    enabled = isSaveEnabled || uiState.savedSuccessfully,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111111),
                        contentColor = Color(0xFFFAFAF9),
                        disabledContainerColor = Color(0xFF111111),
                        disabledContentColor = Color(0xFFFAFAF9).copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    AnimatedContent(
                        targetState = when {
                            uiState.savedSuccessfully -> "saved"
                            uiState.isSaving -> "saving"
                            else -> "idle"
                        },
                        transitionSpec = {
                            if (reduceMotion) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                fadeIn(tween(160)).togetherWith(fadeOut(tween(100)))
                            }
                        },
                        label = "SaveButtonMorph"
                    ) { targetState ->
                        when (targetState) {
                            "saved" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Saved",
                                        tint = Color(0xFFFAFAF9),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Saved",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFAFAF9)
                                    )
                                }
                            }
                            "saving" -> {
                                CircularProgressIndicator(
                                    color = Color(0xFFFAFAF9),
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            else -> {
                                Text(
                                    text = "Save transaction",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSaveEnabled) Color(0xFFFAFAF9) else Color(0xFFFAFAF9).copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }

                // Borderless / Soft Number Pad anchored to bottom
                NumberPad(
                    onDigitPress = viewModel::onDigitPress,
                    onBackspace = viewModel::onBackspace,
                    onDecimalPress = viewModel::onDecimalPress
                )
            }
        }
    }
}

@Composable
fun CategorySelection(
    categories: List<CategoryWithSubcategories>,
    selectedCategoryId: Long?,
    selectedParentCategoryId: Long?,
    onCategorySelect: (Long, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { categoryWithSub ->
                val isSelected = categoryWithSub.category.id == selectedParentCategoryId || categoryWithSub.category.id == selectedCategoryId
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(categoryWithSub.category.id, true) },
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
                            text = categoryWithSub.category.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Color(0xFFFAFAF9) else InkSecondary
                        )
                    }
                )
            }
        }

        val selectedParent = categories.find { it.category.id == selectedParentCategoryId }
        if (selectedParent != null && selectedParent.subcategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(selectedParent.subcategories) { subcategory ->
                    val isSelected = subcategory.id == selectedCategoryId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(subcategory.id, false) },
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
                                text = subcategory.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFFAFAF9) else InkSecondary
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MoreDetailsSection(
    uiState: AddTransactionUiState,
    onNoteChange: (String) -> Unit,
    onAccountSelect: (Long) -> Unit,
    onToggleMore: () -> Unit,
    onCurrencySelect: (String) -> Unit,
    onExchangeRateChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleMore() }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "More details",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = InkSecondary
            )
            Icon(
                imageVector = if (uiState.isMoreExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle details",
                tint = InkSecondary
            )
        }

        AnimatedVisibility(visible = uiState.isMoreExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = onNoteChange,
                    placeholder = { Text("Add a note or payee...", style = MaterialTheme.typography.bodyMedium, color = InkTertiary) },
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

                Spacer(modifier = Modifier.height(10.dp))

                // Account Selection
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.accounts) { account ->
                        val isSelected = account.id == uiState.selectedAccountId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onAccountSelect(account.id) },
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

                Spacer(modifier = Modifier.height(10.dp))

                // Currency Selection
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                val commonCurrencies = listOf("INR", "USD", "EUR", "GBP", "JPY")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(commonCurrencies) { currency ->
                        val isSelected = currency == uiState.selectedCurrency
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCurrencySelect(currency) },
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
                            label = { Text(currency, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                val account = uiState.accounts.find { it.id == uiState.selectedAccountId }
                if (account != null && uiState.selectedCurrency.isNotEmpty() && uiState.selectedCurrency != account.defaultCurrency) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.exchangeRateInput,
                        onValueChange = onExchangeRateChange,
                        label = { Text("Exchange rate (${uiState.selectedCurrency} to ${account.defaultCurrency})") },
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
                }
            }
        }
    }
}

@Composable
fun NumberPad(
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onDecimalPress: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "backspace")
        )

        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NeutralMuted)
                            .clickable {
                                when (key) {
                                    "backspace" -> onBackspace()
                                    "." -> onDecimalPress()
                                    else -> onDigitPress(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "backspace") {
                            Icon(
                                Icons.Default.Backspace,
                                contentDescription = "Backspace",
                                tint = InkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = InkPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
