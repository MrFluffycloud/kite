package com.expensevault.feature.transactions

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.Account
import com.expensevault.core.model.Category
import com.expensevault.core.model.Transaction
import com.expensevault.core.model.TransactionType
import kotlinx.datetime.Clock
import java.math.BigDecimal

private val NeutralBg = Color(0xFFFAFAF9)
private val NeutralCard = Color(0xFFFFFFFF)
private val NeutralMuted = Color(0xFFF0F0EE)
private val InkPrimary = Color(0xFF111111)
private val InkSecondary = Color(0xFF6B6B68)
private val InkTertiary = Color(0xFFB8B8B5)
private val HeroBlack = Color(0xFF0F0F0F)
private val Hairline = Color(0xFFE7E6E3)
private val MutedClay = Color(0xFFB5533C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionBottomSheet(
    transaction: Transaction,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    var amountText by remember { mutableStateOf(transaction.originalAmount.toPlainString()) }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var merchant by remember { mutableStateOf(transaction.merchant ?: "") }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }
    var isError by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeutralCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Hairline, RoundedCornerShape(50))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = InkSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type Segment Toggle (Expense / Income)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeutralMuted, RoundedCornerShape(50))
                    .padding(4.dp)
            ) {
                listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Surface(
                        onClick = { selectedType = type },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) HeroBlack else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFFFAFAF9) else InkSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Amount Field
            Text("Amount", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = InkSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    isError = it.toBigDecimalOrNull() == null || it.toBigDecimalOrNull()!! <= BigDecimal.ZERO
                },
                placeholder = { Text("0.00", color = InkTertiary) },
                isError = isError,
                singleLine = true,
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
                modifier = Modifier.fillMaxWidth()
            )
            if (isError) {
                Text(
                    text = "Please enter a valid amount",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Merchant / Title Field
            Text("Merchant / Title", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = InkSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                placeholder = { Text("e.g. Starbucks, Uber, Rent", color = InkTertiary) },
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account / Wallet Selector
            if (accounts.isNotEmpty()) {
                Text("Account / Wallet", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = InkSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts) { acc ->
                        val isSelected = selectedAccountId == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccountId = acc.id },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HeroBlack,
                                selectedLabelColor = Color(0xFFFAFAF9),
                                containerColor = NeutralMuted,
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
                                    text = acc.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFFAFAF9) else InkPrimary
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Category Selector
            if (categories.isNotEmpty()) {
                val orderedCategories = remember(categories, selectedType) {
                    if (selectedType == TransactionType.INCOME) {
                        categories.sortedByDescending { it.isIncomeCategory }
                    } else {
                        categories.sortedBy { it.isIncomeCategory }
                    }
                }
                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = InkSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orderedCategories) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryId = if (isSelected) null else cat.id },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HeroBlack,
                                selectedLabelColor = Color(0xFFFAFAF9),
                                containerColor = NeutralMuted,
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
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFFAFAF9) else InkPrimary
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Note Field
            Text("Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = InkSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Add additional details (optional)", color = InkTertiary) },
                maxLines = 3,
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

            Spacer(modifier = Modifier.height(24.dp))

            // Save Changes Button
            Button(
                onClick = {
                    val amount = amountText.toBigDecimalOrNull()
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        isError = true
                    } else {
                        val updated = transaction.copy(
                            type = selectedType,
                            originalAmount = amount,
                            baseAmount = amount,
                            merchant = merchant.trim().takeIf { it.isNotEmpty() },
                            note = note.trim().takeIf { it.isNotEmpty() },
                            categoryId = selectedCategoryId,
                            accountId = selectedAccountId,
                            updatedAt = Clock.System.now()
                        )
                        onSave(updated)
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = HeroBlack, contentColor = Color(0xFFFAFAF9)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Save changes", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            if (onDelete != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onDelete(transaction.id) },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MutedClay.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedClay),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete transaction", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
