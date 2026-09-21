package com.expensevault.feature.transactions

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionType
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

// Design Tokens
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val reduceMotion = rememberReduceMotion()
    val context = LocalContext.current
    var isSearchExpanded by remember { mutableStateOf(false) }
    var expandedTransactionId by remember { mutableStateOf<Long?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var detailsTransaction by remember { mutableStateOf<TransactionDisplayItem?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (content != null) {
                    viewModel.importData(content)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = NeutralBg,
        topBar = {
            Column(modifier = Modifier.background(NeutralBg)) {
                TopAppBar(
                    title = {
                        if (isSearchExpanded) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = {
                                    Text(
                                        "Search merchant, note, amount...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkTertiary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = NeutralCard,
                                    unfocusedContainerColor = NeutralMuted,
                                    focusedBorderColor = HeroBlack,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = InkPrimary,
                                    unfocusedTextColor = InkPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            viewModel.onSearchQueryChange("")
                                        } else {
                                            isSearchExpanded = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = InkSecondary)
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = "Transactions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = InkPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NeutralBg,
                        titleContentColor = InkPrimary
                    ),
                    actions = {
                        if (!isSearchExpanded) {
                            IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Import statements", tint = InkPrimary)
                            }
                            IconButton(onClick = { viewModel.refresh() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = InkPrimary)
                            }
                            IconButton(onClick = { isSearchExpanded = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = InkPrimary)
                            }
                        }
                    }
                )

                // Period Chips Row (Pill shaped)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TimePeriod.values()) { period ->
                        val isSelected = uiState.selectedPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onPeriodChange(period) },
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
                                    text = period.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFFAFAF9) else InkSecondary
                                )
                            }
                        )
                    }
                }

                // Category Filter Chips
                if (uiState.categories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val isAllSelected = uiState.selectedCategoryId == null
                            FilterChip(
                                selected = isAllSelected,
                                onClick = { viewModel.onSelectCategory(null) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HeroBlack,
                                    selectedLabelColor = NeutralBg,
                                    containerColor = NeutralCard,
                                    labelColor = InkSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isAllSelected,
                                    borderColor = if (isAllSelected) HeroBlack else Hairline,
                                    borderWidth = 1.dp
                                ),
                                label = {
                                    Text(
                                        text = "All categories",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isAllSelected) FontWeight.Medium else FontWeight.Normal,
                                        color = if (isAllSelected) Color(0xFFFAFAF9) else InkSecondary
                                    )
                                }
                            )
                        }
                        items(uiState.categories) { cat ->
                            val isCatSelected = uiState.selectedCategoryId == cat.id
                            FilterChip(
                                selected = isCatSelected,
                                onClick = { viewModel.onSelectCategory(cat.id) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HeroBlack,
                                    selectedLabelColor = NeutralBg,
                                    containerColor = NeutralCard,
                                    labelColor = InkSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isCatSelected,
                                    borderColor = if (isCatSelected) HeroBlack else Hairline,
                                    borderWidth = 1.dp
                                ),
                                label = {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isCatSelected) FontWeight.Medium else FontWeight.Normal,
                                        color = if (isCatSelected) Color(0xFFFAFAF9) else InkSecondary
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = HeroBlack
                )
            } else if (uiState.isEmpty) {
                EmptyTransactionsState(modifier = Modifier.align(Alignment.Center).fadeInEntry(reduceMotion))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 130.dp)
                ) {
                    uiState.transactions.forEach { (date, transactions) ->
                        stickyHeader(key = date.toString()) {
                            val total = transactions.sumOf {
                                if (it.transaction.type == TransactionType.EXPENSE)
                                    -it.transaction.baseAmount
                                else it.transaction.baseAmount
                            }
                            DateHeader(date = date.toString(), total = total)
                        }

                        itemsIndexed(
                            items = transactions,
                            key = { _, item -> item.transaction.id }
                        ) { index, item ->
                            var isDismissed by remember { mutableStateOf(false) }
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    when (value) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            // Swipe right -> Edit action (Samsung style)
                                            editingTransaction = item.transaction
                                            false // Snap back into place
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            // Swipe left -> Delete action (Samsung style)
                                            isDismissed = true
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            )

                            LaunchedEffect(isDismissed) {
                                if (isDismissed) {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Transaction deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        isDismissed = false
                                        dismissState.reset()
                                    } else {
                                        viewModel.onDeleteTransaction(item.transaction.id)
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = !isDismissed,
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true,
                                    backgroundContent = { DismissBackground(dismissState) }
                                ) {
                                    val isExpanded = expandedTransactionId == item.transaction.id
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .staggeredEntry(index = index, reduceMotion = reduceMotion)
                                            .background(NeutralCard)
                                            .clickable {
                                                expandedTransactionId = if (isExpanded) null else item.transaction.id
                                            }
                                    ) {
                                        // Tap item to expand inline (Samsung Contact style)
                                        TransactionItem(
                                            item = item,
                                            onClick = null
                                        )

                                        // Inline expanded details & actions
                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            ExpandedTransactionPanel(
                                                item = item,
                                                onEdit = { editingTransaction = item.transaction },
                                                onDuplicate = {
                                                    viewModel.duplicateTransaction(item.transaction) {
                                                        Toast.makeText(context, "Transaction duplicated", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onDetails = { detailsTransaction = item },
                                                onDelete = { isDismissed = true }
                                            )
                                        }

                                        HorizontalDivider(
                                            color = Hairline,
                                            thickness = 0.8.dp,
                                            modifier = Modifier.padding(start = 72.dp, end = 20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (editingTransaction != null) {
        EditTransactionBottomSheet(
            transaction = editingTransaction!!,
            categories = uiState.categories,
            accounts = uiState.accounts,
            onDismiss = { editingTransaction = null },
            onSave = { updated ->
                viewModel.updateTransaction(updated)
                editingTransaction = null
            },
            onDelete = { id ->
                viewModel.onDeleteTransaction(id)
                editingTransaction = null
            }
        )
    }

    if (detailsTransaction != null) {
        TransactionDetailsDialog(
            item = detailsTransaction!!,
            onDismiss = { detailsTransaction = null },
            onEdit = { tx ->
                editingTransaction = tx
                detailsTransaction = null
            }
        )
    }
}

@Composable
fun DateHeader(date: String, total: java.math.BigDecimal) {
    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            try {
                currency = java.util.Currency.getInstance("INR")
            } catch (_: Exception) {}
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeutralBg)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = InkSecondary
        )
        Text(
            text = formatter.format(total),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (total < java.math.BigDecimal.ZERO) MutedClay else MutedSage
        )
    }
}

@Composable
fun TransactionItem(
    item: TransactionDisplayItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            try {
                currency = java.util.Currency.getInstance("INR")
            } catch (_: Exception) {}
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading category icon box
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color = NeutralMuted, shape = RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryOutlineIcon(item.category?.name, item.category?.iconName),
                contentDescription = null,
                tint = InkPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center info: Title and Subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.transaction.merchant ?: item.transaction.note ?: "Expense",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.category?.name ?: "General",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    maxLines = 1
                )

                if (!item.transaction.note.isNullOrBlank() && item.transaction.merchant != null) {
                    Text(
                        text = " • ${item.transaction.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (item.transaction.recurringRuleId != null || item.transaction.source == com.expensevault.core.model.TransactionSource.AUTO_RECURRING) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = NeutralMuted
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Repeat,
                                contentDescription = "Recurring",
                                tint = InkSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "recurring",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = InkSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Trailing amount
        val amountStr = formatter.format(item.transaction.baseAmount)
        Text(
            text = if (item.transaction.type == TransactionType.EXPENSE) "-$amountStr" else "+$amountStr",
            color = if (item.transaction.type == TransactionType.EXPENSE) MutedClay else MutedSage,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun ExpandedTransactionPanel(
    item: TransactionDisplayItem,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
    ) {
        // Metadata row (Account and Note)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = InkSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.account?.name ?: "Main wallet",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = InkPrimary
                )
            }
            Text(
                text = item.transaction.transactionDate.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }

        val note = item.transaction.note
        if (!note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Source: ${item.transaction.source.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = InkTertiary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Samsung One UI Style: 4 Dark Circular Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SamsungActionCircle(
                icon = Icons.Default.Edit,
                label = "Edit",
                onClick = onEdit
            )
            SamsungActionCircle(
                icon = Icons.Default.ContentCopy,
                label = "Duplicate",
                onClick = onDuplicate
            )
            SamsungActionCircle(
                icon = Icons.Default.Info,
                label = "Details",
                onClick = onDetails
            )
            SamsungActionCircle(
                icon = Icons.Default.Delete,
                label = "Delete",
                isDestructive = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun SamsungActionCircle(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isDestructive) Color(0xFF2A1515) else HeroBlack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isDestructive) Color(0xFFF87171) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Medium,
            color = if (isDestructive) MutedClay else InkSecondary
        )
    }
}

@Composable
fun TransactionDetailsDialog(
    item: TransactionDisplayItem,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit
) {
    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            try {
                currency = java.util.Currency.getInstance("INR")
            } catch (_: Exception) {}
        }
    }
    val isExpense = item.transaction.type == TransactionType.EXPENSE
    val amountFormatted = formatter.format(item.transaction.baseAmount)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeutralCard,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Transaction details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Amount & Type
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeutralMuted, RoundedCornerShape(16.dp))
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isExpense) "-$amountFormatted" else "+$amountFormatted",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) MutedClay else MutedSage
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.transaction.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = InkSecondary
                    )
                }

                DetailItemRow(label = "Title", value = item.transaction.merchant ?: item.transaction.note ?: "Expense")
                if (item.category != null) {
                    DetailItemRow(label = "Category", value = item.category.name)
                }
                DetailItemRow(label = "Account / Wallet", value = item.account?.name ?: "Main wallet")
                DetailItemRow(label = "Date", value = item.transaction.transactionDate.toString())
                val dialogNote = item.transaction.note
                if (!dialogNote.isNullOrBlank()) {
                    DetailItemRow(label = "Note", value = dialogNote)
                }
                DetailItemRow(
                    label = "Source",
                    value = item.transaction.source.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onEdit(item.transaction) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HeroBlack,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = InkSecondary)
            }
        }
    )
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = InkPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val isEdit = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
    val isDelete = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart

    val bgColor = when {
        isEdit -> Color(0xFFE8ECE7)
        isDelete -> Color(0xFFFDF3F1)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 24.dp)
    ) {
        if (isEdit) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = InkPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            }
        } else if (isDelete) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MutedClay
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MutedClay,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyTransactionsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(NeutralMuted, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = InkSecondary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No transactions found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = InkPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Try adjusting your filters or search query",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            textAlign = TextAlign.Center
        )
    }
}
