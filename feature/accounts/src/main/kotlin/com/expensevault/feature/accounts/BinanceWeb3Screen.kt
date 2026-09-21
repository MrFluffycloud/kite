package com.expensevault.feature.accounts

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensevault.core.model.BinanceAssetBalance
import com.expensevault.core.model.BinanceIntegrationType
import com.expensevault.core.model.Web3AddressUtils
import com.expensevault.core.model.Web3Chain
import org.koin.androidx.compose.koinViewModel

// Strict Design Tokens
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
fun BinanceWeb3Screen(
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncState = uiState.binanceSyncState
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember(syncState.integrationType) {
        mutableStateOf(if (syncState.integrationType == BinanceIntegrationType.EXCHANGE) 1 else 0)
    }

    // Web3 form state
    val parsedAddresses = remember(syncState.web3Address) {
        Web3AddressUtils.parseAddresses(syncState.web3Address)
    }
    var evmAddressInput by remember(parsedAddresses.evm) {
        mutableStateOf(parsedAddresses.evm ?: (if (syncState.web3Address?.startsWith("0x") == true) syncState.web3Address else "") ?: "")
    }
    var btcAddressInput by remember(parsedAddresses.btc) {
        mutableStateOf(parsedAddresses.btc ?: "")
    }
    var solAddressInput by remember(parsedAddresses.sol) {
        mutableStateOf(parsedAddresses.sol ?: "")
    }
    var selectedChains by remember(syncState.web3Chains) {
        mutableStateOf(if (syncState.web3Chains.isNotEmpty()) syncState.web3Chains else setOf(
            "BITCOIN", "SOLANA", "BSC", "ETHEREUM", "ARBITRUM", "POLYGON", "BASE", "OPTIMISM", "AVALANCHE"
        ))
    }
    var web3ErrorMessage by remember { mutableStateOf<String?>(null) }

    // Exchange form state
    var apiKeyInput by remember { mutableStateOf("") }
    var apiSecretInput by remember { mutableStateOf("") }
    var isSecretVisible by remember { mutableStateOf(false) }
    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isTestSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showUnlinkConfirm by remember { mutableStateOf(false) }

    // Sync rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "SyncSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing)
        ),
        label = "SpinAngle"
    )

    Scaffold(
        containerColor = NeutralBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Binance & Web3",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                        Text(
                            text = if (selectedTab == 0) "Decentralized On-Chain Tracking" else "Read-Only Exchange API",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = InkPrimary
                        )
                    }
                },
                actions = {
                    if (syncState.isLinked) {
                        IconButton(
                            onClick = {
                                viewModel.syncBinanceAccount()
                                Toast.makeText(context, "Syncing balances...", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !syncState.isSyncing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = InkPrimary,
                                modifier = Modifier.rotate(if (syncState.isSyncing) rotation else 0f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeutralBg,
                    titleContentColor = InkPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Portfolio & Connection Status Card
            item {
                PortfolioHeroCard(
                    syncState = syncState,
                    isSyncing = syncState.isSyncing,
                    onSyncNow = { viewModel.syncBinanceAccount() }
                )
            }

            // 2. Segmented Integration Type Switcher
            item {
                IntegrationModeTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { newTab ->
                        selectedTab = newTab
                        val newType = if (newTab == 0) BinanceIntegrationType.WEB3_WALLET else BinanceIntegrationType.EXCHANGE
                        viewModel.switchBinanceIntegrationType(newType)
                    }
                )
            }

            // 3. Tab-Specific Content
            if (selectedTab == 0) {
                // ================= WEB3 ON-CHAIN WALLET =================

                // Detected Assets Card (when linked)
                if (syncState.isLinked && syncState.assets.isNotEmpty()) {
                    item {
                        DetectedAssetsCard(assets = syncState.assets)
                    }
                }

                // Multi-Chain Address Input Card
                item {
                    Web3AddressesCard(
                        evmAddress = evmAddressInput,
                        btcAddress = btcAddressInput,
                        solAddress = solAddressInput,
                        onEvmChange = { evmAddressInput = it },
                        onBtcChange = { btcAddressInput = it },
                        onSolChange = { solAddressInput = it },
                        onPasteAutoRoute = { pasted ->
                            val clean = pasted.trim()
                            when {
                                Web3AddressUtils.isEvmAddress(clean) -> {
                                    evmAddressInput = clean
                                    Toast.makeText(context, "Routed to EVM address", Toast.LENGTH_SHORT).show()
                                }
                                Web3AddressUtils.isBitcoinAddress(clean) -> {
                                    btcAddressInput = clean
                                    Toast.makeText(context, "Routed to Bitcoin address", Toast.LENGTH_SHORT).show()
                                }
                                Web3AddressUtils.isSolanaAddress(clean) -> {
                                    solAddressInput = clean
                                    Toast.makeText(context, "Routed to Solana address", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    evmAddressInput = clean
                                    Toast.makeText(context, "Pasted address", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                // Networks to Scan Card
                item {
                    NetworksSelectorCard(
                        selectedChains = selectedChains,
                        onToggleChain = { chainName ->
                            selectedChains = if (selectedChains.contains(chainName)) {
                                selectedChains - chainName
                            } else {
                                selectedChains + chainName
                            }
                        },
                        onSelectAll = {
                            selectedChains = Web3Chain.entries.map { it.name }.toSet()
                        },
                        onDeselectAll = {
                            selectedChains = emptySet()
                        }
                    )
                }

                // Web3 Error Message if any
                if (web3ErrorMessage != null) {
                    item {
                        Text(
                            text = web3ErrorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedClay,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // Action Buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val hasEvm = evmAddressInput.isNotBlank()
                                val hasBtc = btcAddressInput.isNotBlank()
                                val hasSol = solAddressInput.isNotBlank()

                                if (!hasEvm && !hasBtc && !hasSol) {
                                    web3ErrorMessage = "Please enter at least one EVM, Solana, or Bitcoin address."
                                    return@Button
                                }
                                if (hasEvm && !Web3AddressUtils.isEvmAddress(evmAddressInput)) {
                                    web3ErrorMessage = "Invalid EVM address format (must be 0x followed by 40 hex chars)."
                                    return@Button
                                }
                                if (hasBtc && !Web3AddressUtils.isBitcoinAddress(btcAddressInput)) {
                                    web3ErrorMessage = "Invalid Bitcoin address format."
                                    return@Button
                                }
                                if (hasSol && !Web3AddressUtils.isSolanaAddress(solAddressInput)) {
                                    web3ErrorMessage = "Invalid Solana address format (must be 32-44 base58 chars)."
                                    return@Button
                                }
                                if (selectedChains.isEmpty()) {
                                    web3ErrorMessage = "Please select at least one network to scan."
                                    return@Button
                                }

                                web3ErrorMessage = null
                                val combined = Web3AddressUtils.combineAddresses(
                                    evm = evmAddressInput.trim().ifBlank { null },
                                    btc = btcAddressInput.trim().ifBlank { null },
                                    sol = solAddressInput.trim().ifBlank { null }
                                )
                                viewModel.saveBinanceWeb3Config(combined, selectedChains)
                                Toast.makeText(context, "Scanning on-chain balances...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HeroBlack)
                        ) {
                            Text(
                                text = if (syncState.isLinked) "Save & Re-Scan On-Chain" else "Save & Scan On-Chain Balances",
                                fontWeight = FontWeight.SemiBold,
                                color = NeutralBg,
                                fontSize = 16.sp
                            )
                        }

                        if (syncState.isLinked) {
                            OutlinedButton(
                                onClick = { showUnlinkConfirm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedClay),
                                border = BorderStroke(1.dp, MutedClay.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlink Web3 Wallet", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // ================= BINANCE EXCHANGE API =================

                // Security Guarantee Card
                item {
                    ExchangeSecurityCard()
                }

                // API Credentials Input Card
                item {
                    ExchangeCredentialsCard(
                        apiKey = apiKeyInput,
                        apiSecret = apiSecretInput,
                        isSecretVisible = isSecretVisible,
                        isLinked = syncState.isLinked,
                        apiKeyMasked = syncState.apiKeyMasked,
                        onApiKeyChange = { apiKeyInput = it },
                        onApiSecretChange = { apiSecretInput = it },
                        onToggleSecretVisibility = { isSecretVisible = !isSecretVisible }
                    )
                }

                // Test Status Alert
                if (testStatusMessage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isTestSuccess == true) SuccessGreen.copy(alpha = 0.12f) else MutedClay.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (isTestSuccess == true) SuccessGreen else MutedClay)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isTestSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isTestSuccess == true) SuccessGreen else MutedClay,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = testStatusMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isTestSuccess == true) InkPrimary else MutedClay,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Wallets to Track Selector
                item {
                    ExchangeWalletsCard(
                        enabledWallets = syncState.enabledWallets,
                        onWalletsChange = { viewModel.updateBinanceWallets(it) }
                    )
                }

                // Exchange Actions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (apiKeyInput.isBlank() || apiSecretInput.isBlank()) {
                                        testStatusMessage = "Please enter both API key and secret first"
                                        isTestSuccess = false
                                        return@OutlinedButton
                                    }
                                    isTesting = true
                                    testStatusMessage = "Testing connection to Binance..."
                                    isTestSuccess = null
                                    viewModel.testBinanceConnection(apiKeyInput.trim(), apiSecretInput.trim()) { success, err ->
                                        isTesting = false
                                        isTestSuccess = success
                                        testStatusMessage = if (success) "Connection successful! Read permissions verified." else "Connection failed: ${err ?: "Unknown error"}"
                                    }
                                },
                                enabled = !isTesting && (apiKeyInput.isNotBlank() && apiSecretInput.isNotBlank()),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Hairline)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Test Connection", fontWeight = FontWeight.SemiBold, color = InkPrimary)
                                }
                            }

                            Button(
                                onClick = {
                                    if (apiKeyInput.isBlank() || apiSecretInput.isBlank()) {
                                        Toast.makeText(context, "Please enter API Key & Secret", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.saveBinanceCredentials(apiKeyInput.trim(), apiSecretInput.trim())
                                    Toast.makeText(context, "Saving & syncing Binance...", Toast.LENGTH_SHORT).show()
                                },
                                enabled = apiKeyInput.isNotBlank() && apiSecretInput.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HeroBlack)
                            ) {
                                Text("Save & Sync", fontWeight = FontWeight.SemiBold, color = NeutralBg)
                            }
                        }

                        if (syncState.isLinked) {
                            OutlinedButton(
                                onClick = { showUnlinkConfirm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedClay),
                                border = BorderStroke(1.dp, MutedClay.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlink Binance Account", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }

        // Unlink Confirmation Dialog
        if (showUnlinkConfirm) {
            AlertDialog(
                onDismissRequest = { showUnlinkConfirm = false },
                containerColor = NeutralCard,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Unlink Binance", fontWeight = FontWeight.Bold, color = InkPrimary) },
                text = {
                    Text(
                        "Are you sure you want to unlink your Binance account? Stored credentials and cached crypto balances will be removed.",
                        color = InkSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.unlinkBinance()
                            showUnlinkConfirm = false
                            Toast.makeText(context, "Binance account unlinked", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MutedClay)
                    ) {
                        Text("Unlink", fontWeight = FontWeight.SemiBold)
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
}

@Composable
private fun PortfolioHeroCard(
    syncState: com.expensevault.core.model.BinanceSyncState,
    isSyncing: Boolean,
    onSyncNow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HeroBlack),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BinanceGold.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyBitcoin,
                            contentDescription = null,
                            tint = BinanceGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CRYPTO PORTFOLIO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8A8A87),
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (syncState.isLinked) Color(0xFF1E3A2F) else Color(0xFF262626)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (syncState.isLinked) SuccessGreen else Color(0xFF8A8A87),
                                    RoundedCornerShape(50)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (syncState.isLinked) {
                                if (syncState.integrationType == BinanceIntegrationType.WEB3_WALLET) "Active • Web3" else "Active • API"
                            } else "Not Linked",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (syncState.isLinked) SuccessGreen else Color(0xFFB8B8B5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (syncState.isLinked) {
                Text(
                    text = "Total Synced Value",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A8A87)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = syncState.totalFiatBalance.toPlainString(),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFAFAF9)
                )

                if (isSyncing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50)),
                        color = BinanceGold,
                        trackColor = Color(0xFF262626)
                    )
                }
            } else {
                Text(
                    text = "Track your crypto balances in real time alongside your fiat bank accounts. Supports on-chain Web3 scanning (EVM, Solana, BTC) and official read-only Binance API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB8B8B5),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun IntegrationModeTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NeutralCard,
        border = BorderStroke(1.dp, Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Surface(
                onClick = { onTabSelected(0) },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedTab == 0) HeroBlack else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (selectedTab == 0) NeutralBg else InkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Web3 Wallet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedTab == 0) NeutralBg else InkSecondary
                    )
                }
            }

            Surface(
                onClick = { onTabSelected(1) },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedTab == 1) HeroBlack else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyBitcoin,
                        contentDescription = null,
                        tint = if (selectedTab == 1) NeutralBg else InkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Exchange API",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedTab == 1) NeutralBg else InkSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectedAssetsCard(assets: List<BinanceAssetBalance>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Detected Crypto Holdings (${assets.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                assets.forEach { asset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeutralBg, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BinanceGold.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = asset.asset.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = asset.asset,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = InkPrimary
                                )
                                Text(
                                    text = asset.walletName ?: "On-Chain",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = asset.free,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = InkPrimary
                            )
                            if (asset.fiatValue.isNotBlank() && asset.fiatValue != "0") {
                                Text(
                                    text = "≈ ${asset.fiatValue}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Web3AddressesCard(
    evmAddress: String,
    btcAddress: String,
    solAddress: String,
    onEvmChange: (String) -> Unit,
    onBtcChange: (String) -> Unit,
    onSolChange: (String) -> Unit,
    onPasteAutoRoute: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Wallet Addresses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    Text(
                        text = "Public addresses only. Never enter private keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                TextButton(
                    onClick = {
                        val text = clipboardManager.getText()?.text
                        if (!text.isNullOrBlank()) {
                            onPasteAutoRoute(text)
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quick Paste", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Solana Address Input
            val isSolValid = Web3AddressUtils.isSolanaAddress(solAddress)
            OutlinedTextField(
                value = solAddress,
                onValueChange = onSolChange,
                label = { Text("Solana Address (SOL, USDC, USDT)") },
                placeholder = { Text("e.g. EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.GeneratingTokens,
                        contentDescription = null,
                        tint = if (isSolValid) SuccessGreen else InkSecondary
                    )
                },
                trailingIcon = {
                    if (solAddress.isNotBlank()) {
                        IconButton(onClick = { onSolChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = InkSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. EVM Address Input
            val isEvmValid = Web3AddressUtils.isEvmAddress(evmAddress)
            OutlinedTextField(
                value = evmAddress,
                onValueChange = onEvmChange,
                label = { Text("EVM Address (ETH, BNB, Polygon, Arbitrum)") },
                placeholder = { Text("0x...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (isEvmValid) SuccessGreen else InkSecondary
                    )
                },
                trailingIcon = {
                    if (evmAddress.isNotBlank()) {
                        IconButton(onClick = { onEvmChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = InkSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Bitcoin Address Input
            val isBtcValid = Web3AddressUtils.isBitcoinAddress(btcAddress)
            OutlinedTextField(
                value = btcAddress,
                onValueChange = onBtcChange,
                label = { Text("Bitcoin Address (Native BTC)") },
                placeholder = { Text("bc1q... or 1... or 3...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CurrencyBitcoin,
                        contentDescription = null,
                        tint = if (isBtcValid) SuccessGreen else InkSecondary
                    )
                },
                trailingIcon = {
                    if (btcAddress.isNotBlank()) {
                        IconButton(onClick = { onBtcChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = InkSecondary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun NetworksSelectorCard(
    selectedChains: Set<String>,
    onToggleChain: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Networks to Scan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    Text(
                        text = "${selectedChains.size} of ${Web3Chain.entries.size} networks active",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                Row {
                    TextButton(onClick = onSelectAll) {
                        Text("All", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = onDeselectAll) {
                        Text("None", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Web3Chain.entries.forEach { chain ->
                    val isChecked = selectedChains.contains(chain.name)
                    Surface(
                        onClick = { onToggleChain(chain.name) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChecked) NeutralBg else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isChecked) BinanceGold else Color(0xFFB8B8B5),
                                            RoundedCornerShape(50)
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = chain.chainName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = "Native: ${chain.nativeSymbol}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSecondary
                                    )
                                }
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleChain(chain.name) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = HeroBlack,
                                    checkmarkColor = NeutralBg
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExchangeSecurityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFEBF3EA), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Hardware-Backed Security",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "API credentials are encrypted via Android Keystore (AES-256-GCM). Only read-only permissions are accepted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ExchangeCredentialsCard(
    apiKey: String,
    apiSecret: String,
    isSecretVisible: Boolean,
    isLinked: Boolean,
    apiKeyMasked: String,
    onApiKeyChange: (String) -> Unit,
    onApiSecretChange: (String) -> Unit,
    onToggleSecretVisibility: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "API Credentials",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Text(
                text = "Create a Read-Only API Key in your Binance Account Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            if (isLinked && apiKey.isBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeutralBg,
                    border = BorderStroke(1.dp, Hairline),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Linked: $apiKeyMasked",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = InkPrimary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                placeholder = { Text(if (isLinked) "Enter new key to update" else "Paste API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiSecret,
                onValueChange = onApiSecretChange,
                label = { Text("API Secret") },
                placeholder = { Text(if (isLinked) "Enter new secret to update" else "Paste API Secret") },
                singleLine = true,
                visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onToggleSecretVisibility) {
                        Icon(
                            imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = InkSecondary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun ExchangeWalletsCard(
    enabledWallets: Set<String>,
    onWalletsChange: (Set<String>) -> Unit
) {
    val allWallets = listOf("Spot", "Funding", "Earn", "Futures", "Margin")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCard),
        border = BorderStroke(1.dp, Hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Wallets to Include",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allWallets.forEach { wallet ->
                    val isChecked = enabledWallets.contains(wallet)
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            val next = if (isChecked) enabledWallets - wallet else enabledWallets + wallet
                            onWalletsChange(next)
                        },
                        label = { Text(wallet, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HeroBlack,
                            selectedLabelColor = NeutralBg,
                            containerColor = NeutralBg,
                            labelColor = InkSecondary
                        ),
                        shape = RoundedCornerShape(50)
                    )
                }
            }
        }
    }
}
