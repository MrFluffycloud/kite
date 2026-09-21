package com.expensevault.feature.insights

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.expensevault.core.domain.usecase.BudgetStatus

// Strict Design Tokens
private val NeutralBg = Color(0xFFFAFAF9)
private val NeutralCard = Color(0xFFFFFFFF)
private val NeutralMuted = Color(0xFFF0F0EE)
private val InkPrimary = Color(0xFF111111)
private val InkSecondary = Color(0xFF6B6B68)
private val HeroBlack = Color(0xFF0F0F0F)
private val Hairline = Color(0xFFE7E6E3)
private val MutedClay = Color(0xFFB5533C)
private val MutedSage = Color(0xFF5C6E5A)

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = NeutralBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddSheet() },
                containerColor = HeroBlack,
                contentColor = NeutralBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add budget")
            }
        }
    ) { padding ->
        if (uiState.budgetStatuses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = "No budgets set. Tap + to add your first budget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.budgetStatuses) { status ->
                    BudgetCard(status = status)
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun BudgetCard(status: BudgetStatus) {
    val reduceMotion = rememberReduceMotion()
    val targetProgress = (status.spent.toFloat() / status.limit.toFloat()).coerceIn(0f, 1f)
    var startAnim by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) {
        startAnim = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnim) targetProgress else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "BudgetProgressAnim"
    )
    val progressColor = when {
        status.isExceeded -> MutedClay
        status.isWarning -> InkPrimary
        else -> HeroBlack
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                val statusText = when {
                    status.isExceeded -> "Exceeded"
                    status.isWarning -> "Near limit"
                    else -> "On track"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status.isExceeded) MutedClay else InkSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = progressColor,
                trackColor = NeutralMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            val percent = (targetProgress * 100).toInt()
            Text(
                text = "₹${status.spent} / ₹${status.limit} ($percent%)",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
