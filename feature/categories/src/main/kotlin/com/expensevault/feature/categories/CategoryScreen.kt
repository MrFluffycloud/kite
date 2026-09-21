package com.expensevault.feature.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensevault.core.model.Category
import com.expensevault.core.model.CategoryWithSubcategories
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
private val MutedClay = Color(0xFFB5533C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()

    Scaffold(
        containerColor = NeutralBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Categories",
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
                onClick = { viewModel.showAddCategory(null) },
                containerColor = HeroBlack,
                contentColor = NeutralBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add category")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(uiState.categories, key = { _, it -> it.category.id }) { index, catWithSubs ->
                val isExpanded = uiState.expandedCategoryIds.contains(catWithSubs.category.id)
                Box(modifier = Modifier.staggeredEntry(index = index, reduceMotion = reduceMotion)) {
                    CategoryItem(
                        categoryWithSubs = catWithSubs,
                        isExpanded = isExpanded,
                        onToggleExpand = { viewModel.toggleExpand(catWithSubs.category.id) },
                        onEdit = { viewModel.showAddCategory(category = catWithSubs.category) },
                        onAddSub = { viewModel.showAddCategory(parentId = catWithSubs.category.id) },
                        onArchive = { viewModel.archiveCategory(it) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (uiState.showAddSheet) {
        CategoryBottomSheet(
            uiState = uiState,
            onDismiss = { viewModel.dismissAddCategory() },
            onSave = { viewModel.saveCategory() },
            onNameChange = { viewModel.updateFormName(it) },
            onParentChange = { viewModel.updateFormParentId(it) },
            onColorChange = { viewModel.updateFormColorHex(it) },
            onIconChange = { viewModel.updateFormIconName(it) }
        )
    }
}

@Composable
fun CategoryItem(
    categoryWithSubs: CategoryWithSubcategories,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onAddSub: () -> Unit,
    onArchive: (Category) -> Unit
) {
    val cat = categoryWithSubs.category
    val subs = categoryWithSubs.subcategories

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(NeutralMuted, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Category,
                        contentDescription = null,
                        tint = InkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = InkPrimary
                        )
                        if (cat.isArchived) {
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
                    if (subs.isNotEmpty() && !isExpanded) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${subs.size} subcategories",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                }

                IconButton(onClick = { onArchive(cat) }) {
                    Icon(
                        if (cat.isArchived) Icons.Filled.Restore else Icons.Filled.Delete,
                        contentDescription = "Archive/Restore",
                        tint = InkTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (subs.isNotEmpty()) {
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = InkSecondary
                        )
                    }
                } else {
                    IconButton(onClick = onAddSub) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add subcategory",
                            tint = InkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded && subs.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp, bottom = 12.dp, end = 16.dp)
                ) {
                    HorizontalDivider(color = Hairline, thickness = 0.8.dp)
                    subs.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(InkTertiary, RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (sub.isArchived) {
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
                            IconButton(onClick = { onArchive(sub) }) {
                                Icon(
                                    if (sub.isArchived) Icons.Filled.Restore else Icons.Filled.Delete,
                                    contentDescription = "Archive/Restore",
                                    tint = InkTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    TextButton(onClick = onAddSub, modifier = Modifier.padding(start = 12.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = InkPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add subcategory", color = InkPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBottomSheet(
    uiState: CategoryUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onParentChange: (Long?) -> Unit,
    onColorChange: (String) -> Unit,
    onIconChange: (String) -> Unit
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
                text = if (uiState.editingCategory == null) "Add category" else "Edit category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = uiState.formName,
                onValueChange = onNameChange,
                label = { Text("Category name", color = InkSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NeutralCard,
                    unfocusedContainerColor = NeutralMuted,
                    focusedBorderColor = HeroBlack,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                val parentName = if (uiState.formParentId == null) "None (Top level)" else {
                    uiState.categories.find { it.category.id == uiState.formParentId }?.category?.name ?: "Unknown"
                }
                OutlinedTextField(
                    value = parentName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Parent category", color = InkSecondary) },
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
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None (Top level)") },
                        onClick = {
                            onParentChange(null)
                            expanded = false
                        }
                    )
                    uiState.categories.forEach { catWithSubs ->
                        if (catWithSubs.category.id != uiState.editingCategory?.id) {
                            DropdownMenuItem(
                                text = { Text(catWithSubs.category.name) },
                                onClick = {
                                    onParentChange(catWithSubs.category.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = InkSecondary) }
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
