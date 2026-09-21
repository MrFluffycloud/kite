package com.expensevault.feature.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensevault.core.model.BinanceIntegrationType
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
    onSaveWeb3Config: ((address: String, chains: Set<String>) -> Unit)? = null,
    onSwitchIntegrationType: ((BinanceIntegrationType) -> Unit)? = null,
    onTestConnection: (apiKey: String, apiSecret: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onSyncNow: () -> Unit,
    onUnlink: () -> Unit,
    onUpdateWallets: ((Set<String>) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by remember(syncState.integrationType) {
        mutableStateOf(if (syncState.integrationType == BinanceIntegrationType.EXCHANGE) 1 else 0)
    }

    // Web3 form state
    val parsedAddresses = remember(syncState.web3Address) {
        com.expensevault.core.model.Web3AddressUtils.parseAddresses(syncState.web3Address)
    }
    var web3AddressInput by remember(parsedAddresses.first) {
        mutableStateOf(parsedAddresses.first ?: (if (syncState.web3Address?.startsWith("0x") == true) syncState.web3Address else "") ?: "")
    }
    var btcAddressInput by remember(parsedAddresses.second) {
        mutableStateOf(parsedAddresses.second ?: (if (syncState.web3Address != null && !syncState.web3Address!!.startsWith("0x")) syncState.web3Address else "") ?: "")
    }
    var selectedChains by remember(syncState.web3Chains) {
        mutableStateOf(if (syncState.web3Chains.isNotEmpty()) syncState.web3Chains else setOf(
            "BITCOIN", "BSC", "ETHEREUM", "ARBITRUM", "POLYGON", "BASE", "OPTIMISM", "AVALANCHE"
        ))
    }
    var isEditingWeb3 by remember { mutableStateOf(false) }
    var web3ErrorMessage by remember { mutableStateOf<String?>(null) }

    // Exchange form state
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
                .padding(horizontal = 24.dp, vertical = 12.dp)
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
                        text = if (selectedTab == 0) "Binance Web3 Wallet" else "Binance Exchange",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    Text(
                        text = if (syncState.isLinked) "Linked to Kite" else "Connect crypto account",
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

            Spacer(modifier = Modifier.height(16.dp))

            // Integration Type Selector (Web3 Wallet vs Exchange API)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NeutralBg,
                contentColor = InkPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onSwitchIntegrationType?.invoke(BinanceIntegrationType.WEB3_WALLET)
                    },
                    text = {
                        Text(
                            text = "Web3 Wallet",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onSwitchIntegrationType?.invoke(BinanceIntegrationType.EXCHANGE)
                    },
                    text = {
                        Text(
                            text = "Exchange API",
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------------------
            // TAB 0: WEB3 WALLET (On-Chain / Public Address)
            // ----------------------------------------------------
            if (selectedTab == 0) {
                val isWeb3Linked = syncState.isLinked && syncState.integrationType == BinanceIntegrationType.WEB3_WALLET

                if (!isWeb3Linked || isEditingWeb3) {
                    // Guidance Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralBg),
                        border = BorderStroke(1.dp, Hairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = InkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Track Non-Custodial Web3 Wallet",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• EVM Chains (BSC, Ethereum, Arbitrum, Polygon, Base, etc.): Paste your 0x... address.\n• Native Bitcoin: Paste your Bitcoin address (bc1..., 1..., or 3...) to track native BTC.\n• No private keys or API keys needed — 100% on-device read-only.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = InkSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // EVM Address Input
                    OutlinedTextField(
                        value = web3AddressInput,
                        onValueChange = {
                            web3AddressInput = it
                            web3ErrorMessage = null
                        },
                        label = { Text("EVM Wallet Address (0x...)") },
                        placeholder = { Text("0x... (BNB Chain, ETH, Arbitrum, Base, etc.)") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = InkSecondary
                            )
                        },
                        trailingIcon = {
                            if (web3AddressInput.isNotEmpty()) {
                                IconButton(onClick = { web3AddressInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = InkSecondary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeroBlack,
                            unfocusedBorderColor = Hairline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Native Bitcoin Address Input
                    OutlinedTextField(
                        value = btcAddressInput,
                        onValueChange = {
                            btcAddressInput = it
                            web3ErrorMessage = null
                        },
                        label = { Text("Bitcoin Address (bc1..., 1..., 3...) - Optional") },
                        placeholder = { Text("Native Bitcoin address from Binance Web3") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CurrencyBitcoin,
                                contentDescription = null,
                                tint = BinanceGold
                            )
                        },
                        trailingIcon = {
                            if (btcAddressInput.isNotEmpty()) {
                                IconButton(onClick = { btcAddressInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = InkSecondary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeroBlack,
                            unfocusedBorderColor = Hairline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Network selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Networks to scan (${selectedChains.size} active)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = InkSecondary
                        )
                        val allChainIds = listOf("BITCOIN", "BSC", "ETHEREUM", "ARBITRUM", "POLYGON", "BASE", "OPTIMISM", "AVALANCHE")
                        val isAllSelected = selectedChains.containsAll(allChainIds)
                        Text(
                            text = if (isAllSelected) "Deselect All" else "Select All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary,
                            modifier = Modifier.clickable {
                                selectedChains = if (isAllSelected) setOf("BITCOIN", "BSC") else allChainIds.toSet()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val availableChains = listOf(
                        "BITCOIN" to "Bitcoin",
                        "BSC" to "BNB Chain",
                        "ETHEREUM" to "Ethereum",
                        "ARBITRUM" to "Arbitrum",
                        "POLYGON" to "Polygon",
                        "BASE" to "Base",
                        "OPTIMISM" to "Optimism",
                        "AVALANCHE" to "Avalanche"
                    )

                    availableChains.chunked(2).forEach { rowChains ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowChains.forEach { (id, label) ->
                                val isSelected = selectedChains.contains(id)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val updated = selectedChains.toMutableSet()
                                        if (isSelected) {
                                            if (updated.size > 1) updated.remove(id)
                                        } else {
                                            updated.add(id)
                                        }
                                        selectedChains = updated
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HeroBlack,
                                        selectedLabelColor = NeutralBg
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowChains.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (web3ErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = web3ErrorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedClay
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isEditingWeb3) {
                            OutlinedButton(
                                onClick = { isEditingWeb3 = false },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Hairline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = InkSecondary)
                            }
                        }

                        Button(
                            onClick = {
                                val evm = web3AddressInput.trim()
                                val btc = btcAddressInput.trim()

                                val effectiveEvm = if (com.expensevault.core.model.Web3AddressUtils.isEvmAddress(evm)) evm else ""
                                val effectiveBtc = when {
                                    com.expensevault.core.model.Web3AddressUtils.isBitcoinAddress(btc) -> btc
                                    com.expensevault.core.model.Web3AddressUtils.isBitcoinAddress(evm) -> evm
                                    else -> ""
                                }

                                if (effectiveEvm.isEmpty() && effectiveBtc.isEmpty()) {
                                    web3ErrorMessage = "Please enter an EVM address (0x...) or a Bitcoin address (bc1..., 1..., 3...)"
                                } else if (evm.isNotEmpty() && !com.expensevault.core.model.Web3AddressUtils.isEvmAddress(evm) && !com.expensevault.core.model.Web3AddressUtils.isBitcoinAddress(evm)) {
                                    web3ErrorMessage = "Invalid EVM address (must start with 0x and be 42 characters)"
                                } else if (btc.isNotEmpty() && !com.expensevault.core.model.Web3AddressUtils.isBitcoinAddress(btc)) {
                                    web3ErrorMessage = "Invalid Bitcoin address (must start with bc1, 1, or 3)"
                                } else {
                                    val combined = com.expensevault.core.model.Web3AddressUtils.combineAddresses(effectiveEvm, effectiveBtc)
                                    onSaveWeb3Config?.invoke(combined, selectedChains)
                                    isEditingWeb3 = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HeroBlack),
                            modifier = Modifier.weight(if (isEditingWeb3) 1.4f else 1f),
                            enabled = !syncState.isSyncing
                        ) {
                            if (syncState.isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeutralBg)
                            } else {
                                Text(if (isEditingWeb3) "Save & Resync" else "Connect Web3 Wallet", color = NeutralBg, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    // Linked Web3 State
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
                                    text = "Wallet Address",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                                Text(
                                    text = syncState.web3Address?.let {
                                        if (it.length > 10) "${it.take(6)}...${it.takeLast(4)}" else it
                                    } ?: syncState.apiKeyMasked,
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
                                    text = "Tracked Networks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                                Text(
                                    text = syncState.web3Chains.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
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
                                    text = syncState.lastSyncedAt?.toString()?.take(19)?.replace("T", " ") ?: "Just now",
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

                    // Asset breakdown list
                    if (syncState.assets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "On-Chain Holdings (${syncState.assets.size} tokens)",
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
                                            text = "${asset.free} balance",
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

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { isEditingWeb3 = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Hairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure Networks & Addresses", color = InkPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                            Text("Disconnect", color = MutedClay)
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
                                Text("Refresh", color = NeutralBg)
                            }
                        }
                    }
                }
            } else {
                // ----------------------------------------------------
                // TAB 1: EXCHANGE API (Read-Only API Key & Secret)
                // ----------------------------------------------------
                val isExchangeLinked = syncState.isLinked && syncState.integrationType == BinanceIntegrationType.EXCHANGE

                if (!isExchangeLinked) {
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
                    // Linked Exchange State UI
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
                                    text = "Masked API Key",
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
                                Text("Sync Now", color = NeutralBg)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showUnlinkConfirm) {
        AlertDialog(
            onDismissRequest = { showUnlinkConfirm = false },
            title = {
                Text(
                    text = if (selectedTab == 0) "Disconnect Web3 Wallet?" else "Unlink Binance Account?",
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove the Binance connection and clear credentials from this device. Historical transactions already imported will not be deleted.",
                    color = InkSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkConfirm = false
                        onUnlink()
                    }
                ) {
                    Text("Disconnect", color = MutedClay, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkConfirm = false }) {
                    Text("Cancel", color = InkSecondary)
                }
            },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun WalletsToggleSection(
    enabledWallets: Set<String>,
    onToggleWallet: (String) -> Unit
) {
    val allWallets = listOf("Spot", "Funding", "Earn", "Futures", "Margin")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Exchange wallets to include",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = InkSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            allWallets.forEach { wallet ->
                val isSelected = enabledWallets.any { it.equals(wallet, ignoreCase = true) }
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleWallet(wallet) },
                    label = {
                        Text(
                            text = if (wallet == "Funding") "Pay/P2P" else wallet,
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HeroBlack,
                        selectedLabelColor = NeutralBg
                    )
                )
            }
        }
    }
}
