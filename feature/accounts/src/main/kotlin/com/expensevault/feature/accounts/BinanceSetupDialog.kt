package com.expensevault.feature.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.BinanceSyncState

private val NeutralBg = Color(0xFFFAFAF9)
private val NeutralCard = Color(0xFFFFFFFF)
private val NeutralMuted = Color(0xFFF0F0EE)
private val InkPrimary = Color(0xFF111111)
private val InkSecondary = Color(0xFF6B6B68)
private val HeroBlack = Color(0xFF0F0F0F)
private val Hairline = Color(0xFFE7E6E3)
private val MutedClay = Color(0xFFB5533C)
private val SuccessGreen = Color(0xFF10B981)
private val BinanceGold = Color(0xFFF0B90B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinanceSetupDialog(
    syncState: BinanceSyncState,
    onDismiss: () -> Unit,
    onSaveAndSync: (apiKey: String, apiSecret: String) -> Unit,
    onTestConnection: (apiKey: String, apiSecret: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onSyncNow: () -> Unit,
    onUnlink: () -> Unit,
    onUpdateWallets: ((Set<String>) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var apiKeyInput by remember { mutableStateOf("") }
    var apiSecretInput by remember { mutableStateOf("") }
    var isSecretVisible by remember { mutableStateOf(false) }

    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isTestSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showUnlinkConfirm by remember { mutableStateOf(false) }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(BinanceGold.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyBitcoin,
                        contentDescription = "Binance",
                        tint = InkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Binance Account Linking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    Text(
                        text = if (syncState.isLinked) "Connected to Binance" else "Link read-only API credentials",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
                if (syncState.isLinked) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFECFDF5)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SuccessGreen, RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!syncState.isLinked) {
                // Info banner for read-only guidance
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralBg),
                    border = BorderStroke(1.dp, Hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Kite calls Binance directly from your device. Your API secret is encrypted with Android KeyStore. Only enable 'Can Read' permission in Binance. Never enable trading or withdrawal permissions.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = InkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Key input
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("API Key") },
                    placeholder = { Text("Paste your Binance API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HeroBlack,
                        unfocusedBorderColor = Hairline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // API Secret input
                OutlinedTextField(
                    value = apiSecretInput,
                    onValueChange = { apiSecretInput = it },
                    label = { Text("API Secret") },
                    placeholder = { Text("Paste your Binance API Secret") },
                    singleLine = true,
                    visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                            Icon(
                                imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isSecretVisible) "Hide secret" else "Show secret",
                                tint = InkSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HeroBlack,
                        unfocusedBorderColor = Hairline
                    )
                )

                if (testStatusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = testStatusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTestSuccess == true) SuccessGreen else MutedClay
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (apiKeyInput.isNotBlank() && apiSecretInput.isNotBlank()) {
                                isTesting = true
                                testStatusMessage = null
                                onTestConnection(apiKeyInput, apiSecretInput) { success, err ->
                                    isTesting = false
                                    isTestSuccess = success
                                    testStatusMessage = if (success) {
                                        "Connection verified successfully!"
                                    } else {
                                        err ?: "Connection test failed."
                                    }
                                }
                            } else {
                                testStatusMessage = "Please enter both API key and secret"
                                isTestSuccess = false
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Test", color = InkPrimary)
                        }
                    }

                    Button(
                        onClick = {
                            if (apiKeyInput.isNotBlank() && apiSecretInput.isNotBlank()) {
                                onSaveAndSync(apiKeyInput, apiSecretInput)
                            } else {
                                testStatusMessage = "Please enter both API key and secret"
                                isTestSuccess = false
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HeroBlack),
                        modifier = Modifier.weight(1f),
                        enabled = !syncState.isSyncing
                    ) {
                        if (syncState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeutralBg)
                        } else {
                            Text("Link & Sync", color = NeutralBg)
                        }
                    }
                }
            } else {
                // Linked State UI
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralBg),
                    border = BorderStroke(1.dp, Hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Masked Key",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                            Text(
                                text = syncState.apiKeyMasked,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = InkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Last Synced",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                            Text(
                                text = syncState.lastSyncedAt?.toString()?.take(19)?.replace("T", " ") ?: "Never",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = InkPrimary
                            )
                        }

                        if (syncState.lastError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sync Error: ${syncState.lastError}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedClay
                            )
                        }
                    }
                }

                // Wallets to include toggle section
                Spacer(modifier = Modifier.height(14.dp))
                WalletsToggleSection(
                    enabledWallets = syncState.enabledWallets,
                    onToggleWallet = { toggled ->
                        val current = syncState.enabledWallets.toMutableSet()
                        if (current.any { it.equals(toggled, ignoreCase = true) }) {
                            if (current.size > 1) {
                                current.removeIf { it.equals(toggled, ignoreCase = true) }
                            }
                        } else {
                            current.add(toggled)
                        }
                        onUpdateWallets?.invoke(current)
                    }
                )

                // Asset breakdown list if available
                if (syncState.assets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Portfolio Holdings (${syncState.assets.size} assets)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(syncState.assets) { asset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NeutralBg, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = asset.asset,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = InkPrimary
                                        )
                                        val wallet = asset.walletName
                                        if (!wallet.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = NeutralMuted,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = wallet,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = InkSecondary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${asset.free} free",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = InkSecondary
                                    )
                                }
                                Text(
                                    text = asset.fiatValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = InkPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons for linked state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showUnlinkConfirm = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MutedClay.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unlink", color = MutedClay)
                    }

                    Button(
                        onClick = onSyncNow,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HeroBlack),
                        modifier = Modifier.weight(1f),
                        enabled = !syncState.isSyncing
                    ) {
                        if (syncState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeutralBg)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeutralBg)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", color = NeutralBg)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showUnlinkConfirm) {
        AlertDialog(
            onDismissRequest = { showUnlinkConfirm = false },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Unlink Binance Account?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove your encrypted API keys from the secure KeyStore. The Binance account ledger in Kite will remain, but automatic sync will stop.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkConfirm = false
                        onUnlink()
                    }
                ) {
                    Text("Unlink", color = MutedClay, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkConfirm = false }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }
}

@Composable
private fun WalletsToggleSection(
    enabledWallets: Set<String>,
    onToggleWallet: (String) -> Unit
) {
    val walletOptions = listOf(
        "Spot" to "Spot",
        "Funding" to "Funding (Pay/P2P)",
        "Earn" to "Earn",
        "Futures" to "Futures",
        "Margin" to "Margin"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Wallets to include in balance",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = InkSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(walletOptions) { (key, label) ->
                val isSelected = enabledWallets.any { it.equals(key, ignoreCase = true) }
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleWallet(key) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HeroBlack,
                        selectedLabelColor = NeutralBg,
                        containerColor = NeutralBg,
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
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}
