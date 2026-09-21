package com.expensevault.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.CurrencyRupee
import com.expensevault.feature.accounts.BinanceSetupDialog
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.expensevault.core.domain.usecase.ExportFormat
import com.expensevault.platform.notification.TransactionNotificationListenerService
import com.expensevault.platform.notification.TransactionNotificationManager
import java.io.File
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

private const val PREFS_SETTINGS = "expense_vault_settings"
private const val PREF_AUTO_DETECT = "auto_detect_notifications"
const val PREF_REDUCE_MOTION = "pref_reduce_motion"

fun isSystemReduceMotionEnabled(context: Context): Boolean {
    val resolver = context.contentResolver
    val durationScale = android.provider.Settings.Global.getFloat(resolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transitionScale = android.provider.Settings.Global.getFloat(resolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    return durationScale == 0f || transitionScale == 0f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onVaultTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE) }

    var isNotificationAccessGranted by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        )
    }

    var isPostNotificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPostNotificationGranted = isGranted
        if (isGranted) {
            prefs.edit().putBoolean(PREF_AUTO_DETECT, true).apply()
            viewModel.setNotificationDetection(true)
            TransactionNotificationListenerService.requestRebindService(context)
            Toast.makeText(context, "Automated expense detection enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission is required to show detected expense alerts", Toast.LENGTH_LONG).show()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isNotificationAccessGranted = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isPostNotificationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
                if (isNotificationAccessGranted && isPostNotificationGranted && prefs.getBoolean(PREF_AUTO_DETECT, false)) {
                    TransactionNotificationListenerService.requestRebindService(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var isAutoDetectChecked by remember {
        mutableStateOf(prefs.getBoolean(PREF_AUTO_DETECT, false) && isNotificationAccessGranted)
    }

    var isReduceMotionChecked by remember {
        val initial = if (prefs.contains(PREF_REDUCE_MOTION)) {
            prefs.getBoolean(PREF_REDUCE_MOTION, false)
        } else {
            isSystemReduceMotionEnabled(context)
        }
        mutableStateOf(initial)
    }

    // Handle export share intent
    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { result ->
            try {
                val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val exportFile = File(cacheDir, result.fileName)
                exportFile.writeText(result.content)

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = result.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Kite Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share export file"))
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                viewModel.clearExportResult()
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    viewModel.importData(text)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        containerColor = NeutralBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // General Group
            SettingsGroupHeader(text = "Preferences")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SettingsItemRow(
                        icon = Icons.Default.CurrencyRupee,
                        title = "Base currency",
                        subtitle = uiState.baseCurrency,
                        onClick = { viewModel.showCurrencyDialog() }
                    )
                    HorizontalDivider(color = Hairline, thickness = 0.8.dp, modifier = Modifier.padding(start = 58.dp))
                    SettingsItemRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Default account",
                        subtitle = uiState.defaultAccountName,
                        onClick = { viewModel.showDefaultAccountDialog() }
                    )
                    HorizontalDivider(color = Hairline, thickness = 0.8.dp, modifier = Modifier.padding(start = 58.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeutralMuted, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Animation,
                                contentDescription = null,
                                tint = InkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reduce motion",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = InkPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Minimize interface animations and transitions",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isReduceMotionChecked,
                            onCheckedChange = { checked ->
                                isReduceMotionChecked = checked
                                prefs.edit().putBoolean(PREF_REDUCE_MOTION, checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeutralBg,
                                checkedTrackColor = HeroBlack,
                                uncheckedThumbColor = InkSecondary,
                                uncheckedTrackColor = NeutralMuted
                            )
                        )
                    }
                }
            }

            // Automated Tracking Group
            SettingsGroupHeader(text = "Automated tracking")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeutralMuted, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = InkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-detect UPI & bank spends",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = InkPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Parses incoming alerts locally to suggest 1-tap logging",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isAutoDetectChecked,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    isNotificationAccessGranted = NotificationManagerCompat
                                        .getEnabledListenerPackages(context)
                                        .contains(context.packageName)

                                    if (!isNotificationAccessGranted) {
                                        viewModel.showNotificationDisclosure()
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPostNotificationGranted) {
                                        postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        prefs.edit().putBoolean(PREF_AUTO_DETECT, true).apply()
                                        isAutoDetectChecked = true
                                        viewModel.setNotificationDetection(true)
                                        TransactionNotificationListenerService.requestRebindService(context)
                                    }
                                } else {
                                    prefs.edit().putBoolean(PREF_AUTO_DETECT, false).apply()
                                    isAutoDetectChecked = false
                                    viewModel.setNotificationDetection(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeutralBg,
                                checkedTrackColor = HeroBlack,
                                uncheckedThumbColor = InkSecondary,
                                uncheckedTrackColor = NeutralMuted
                            )
                        )
                    }

                    // Status Indicator & Test Detection Trigger
                    if (isAutoDetectChecked) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Hairline, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val isFullyActive = isNotificationAccessGranted && isPostNotificationGranted
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isFullyActive) Color(0xFFECFDF5) else Color(0xFFFEF3C7))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isFullyActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFullyActive) "Active & listening" else "Permission needed",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isFullyActive) Color(0xFF047857) else Color(0xFFB45309)
                                )
                            }

                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPostNotificationGranted) {
                                        postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        TransactionNotificationManager.showTestExpenseNotification(context)
                                        Toast.makeText(context, "Test alert sent! Check your notification bar.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeutralMuted,
                                    contentColor = InkPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "Test detection",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Security Group
            SettingsGroupHeader(text = "Security")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeutralMuted, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App lock",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isAppLockEnabled) "Biometric lock enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    Switch(
                        checked = uiState.isAppLockEnabled,
                        onCheckedChange = { targetState ->
                            val activity = context as? androidx.fragment.app.FragmentActivity
                            if (activity != null) {
                                viewModel.requestToggleAppLock(
                                    activity = activity,
                                    enable = targetState,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            if (targetState) "App lock enabled" else "App lock disabled",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Authentication unavailable", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeutralBg,
                            checkedTrackColor = HeroBlack,
                            uncheckedThumbColor = InkSecondary,
                            uncheckedTrackColor = NeutralMuted
                        )
                    )
                }
            }

            // Linked Accounts Group
            SettingsGroupHeader(text = "Linked accounts")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                SettingsItemRow(
                    icon = Icons.Default.CurrencyBitcoin,
                    title = "Binance wallet",
                    subtitle = if (uiState.binanceSyncState.isLinked) {
                        "Connected • Tap to manage or sync"
                    } else {
                        "Link read-only API credentials"
                    },
                    onClick = { viewModel.showBinanceDialog(true) }
                )
            }

            // Data Group
            SettingsGroupHeader(text = "Data & backup")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    SettingsItemRow(
                        icon = Icons.Default.FileDownload,
                        title = "Export data",
                        subtitle = "Export transactions to CSV or JSON",
                        onClick = { viewModel.showExportDialog() }
                    )
                    HorizontalDivider(color = Hairline, thickness = 0.8.dp, modifier = Modifier.padding(start = 58.dp))
                    SettingsItemRow(
                        icon = Icons.Default.FileUpload,
                        title = "Import data",
                        subtitle = "Import transactions from CSV or JSON",
                        onClick = { filePickerLauncher.launch("*/*") }
                    )
                    HorizontalDivider(color = Hairline, thickness = 0.8.dp, modifier = Modifier.padding(start = 58.dp))
                    SettingsItemRow(
                        icon = Icons.Default.DeleteOutline,
                        title = "Clear all data",
                        subtitle = "Permanently delete records",
                        titleColor = MutedClay,
                        onClick = { viewModel.showClearDataDialog() }
                    )
                }
            }

            // App & Updates Group
            SettingsGroupHeader(text = "App & updates")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                SettingsItemRow(
                    icon = Icons.Default.SystemUpdate,
                    title = "Check for updates",
                    subtitle = if (uiState.isCheckingForUpdates) "Checking GitHub releases..." else "Kite v0.2.0 • Tap to check for latest release",
                    onClick = { viewModel.checkForUpdates("0.2.0") }
                )
            }

            // Privacy Note Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NeutralMuted, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = InkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Encrypted with hardware-backed AES-256-GCM. Private records are strictly excluded from standard exports and system logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
            }

            // About Group with Stealth Trigger
            SettingsGroupHeader(text = "About")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralCard),
                border = BorderStroke(1.dp, Hairline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                StealthTriggerVersionRow(
                    version = "Kite v0.2.0 (Build 2)",
                    onTrigger = onVaultTrigger
                )
            }

            Spacer(modifier = Modifier.height(130.dp))
        }
    }

    // Google Play Compliance Disclosure Dialog for Notification Access
    if (uiState.showNotificationDisclosure) {
        AlertDialog(
            onDismissRequest = { viewModel.hideNotificationDisclosure() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = InkPrimary) },
            title = {
                Text(
                    text = "Payment spend auto-detection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Kite can detect incoming transaction alerts from UPI apps (Google Pay, PhonePe, Paytm, CRED) and bank notifications to save you from manual logging.\n",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkPrimary
                    )
                    Text(
                        text = "Privacy & safety guarantee:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkPrimary
                    )
                    Text(
                        text = "• 100% On-device: Notifications are processed completely offline.\n" +
                               "• Zero data sharing: Content never leaves your phone.\n" +
                               "• No SMS permissions: Raw SMS inbox is never read.\n" +
                               "• You can disable this at any time in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.hideNotificationDisclosure()
                        prefs.edit().putBoolean(PREF_AUTO_DETECT, true).apply()
                        isAutoDetectChecked = true
                        viewModel.setNotificationDetection(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPostNotificationGranted) {
                            postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Please enable notification access in Settings", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeroBlack, contentColor = NeutralBg),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Accept & continue", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideNotificationDisclosure() }) {
                    Text("Not now", color = InkSecondary)
                }
            }
        )
    }

    // Export Dialog (CSV / JSON)
    if (uiState.showExportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideExportDialog() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Export format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Select how you would like to export your transaction and account records:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.exportData(ExportFormat.CSV) },
                        colors = ButtonDefaults.buttonColors(containerColor = HeroBlack, contentColor = NeutralBg),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Export as CSV", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.exportData(ExportFormat.JSON) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeutralMuted, contentColor = InkPrimary),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Export as JSON", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.hideExportDialog() }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }

    // Currency Selection Dialog
    if (uiState.showCurrencyDialog) {
        val currencies = listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD")
        AlertDialog(
            onDismissRequest = { viewModel.hideCurrencyDialog() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Base currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Column {
                    currencies.forEach { currency ->
                        TextButton(
                            onClick = {
                                viewModel.setBaseCurrency(currency)
                                viewModel.hideCurrencyDialog()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currency, color = InkPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideCurrencyDialog() }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }

    // Default Account Selection Dialog
    if (uiState.showDefaultAccountDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDefaultAccountDialog() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Select default account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "New transactions and auto-detected expenses will use this account by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (uiState.accounts.isEmpty()) {
                        Text("No accounts found", color = InkSecondary, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        uiState.accounts.forEach { account ->
                            val isSelected = account.id == uiState.defaultAccountId
                            Surface(
                                onClick = { viewModel.setDefaultAccount(account.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) NeutralMuted else Color.Transparent,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = InkPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = InkPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.hideDefaultAccountDialog() }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }

    if (uiState.showBinanceDialog) {
        BinanceSetupDialog(
            syncState = uiState.binanceSyncState,
            onDismiss = { viewModel.showBinanceDialog(false) },
            onSaveAndSync = { key, secret -> viewModel.saveBinanceCredentials(key, secret) },
            onTestConnection = { key, secret, cb -> viewModel.testBinanceConnection(key, secret, cb) },
            onSyncNow = { viewModel.syncBinanceNow() },
            onUnlink = { viewModel.unlinkBinance() }
        )
    }

    // Device Security Required Dialog
    if (uiState.showNoSecurityDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoSecurityDialog() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Device Security Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Text(
                    text = "To enable App Lock, your device must have a screen lock configured (Fingerprint, Face, PIN, pattern, or password). Please set one up in system settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissNoSecurityDialog()
                        try {
                            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Please open Android Settings > Security to set a lock", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeroBlack,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open Settings", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNoSecurityDialog() }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (uiState.showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideClearDataDialog() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Clear all data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all data? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData { success ->
                            if (success) {
                                Toast.makeText(context, "All data has been reset to defaults", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to clear data", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !uiState.isClearingData
                ) {
                    if (uiState.isClearingData) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MutedClay)
                    } else {
                        Text("Clear", color = MutedClay, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideClearDataDialog() }) {
                    Text("Cancel", color = InkSecondary)
                }
            }
        )
    }

    // App Update Dialog
    if (uiState.showUpdateDialog) {
        val update = uiState.updateInfo
        AlertDialog(
            onDismissRequest = { viewModel.hideUpdateDialog() },
            containerColor = NeutralCard,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (update?.isUpdateAvailable == true) "New Update Available" else "Kite is Up to Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkPrimary
                )
            },
            text = {
                Column {
                    if (update?.isUpdateAvailable == true) {
                        Text(
                            text = "A new version of Kite (${update.latestVersion}) is available!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = InkPrimary
                        )
                        val notes = update.releaseNotes
                        if (!notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Release Notes:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = InkSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notes.take(350),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                    } else {
                        Text(
                            text = "You are currently running the latest version of Kite (${update?.currentVersion ?: "0.2.0"}).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    }
                }
            },
            confirmButton = {
                if (update?.isUpdateAvailable == true) {
                    val targetUrl = update.apkDownloadUrl ?: update.releasePageUrl
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            context.startActivity(intent)
                            viewModel.hideUpdateDialog()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HeroBlack),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (update.apkDownloadUrl != null) "Download APK" else "View on GitHub", color = NeutralBg)
                    }
                } else {
                    TextButton(onClick = { viewModel.hideUpdateDialog() }) {
                        Text("OK", color = InkPrimary)
                    }
                }
            },
            dismissButton = {
                if (update?.isUpdateAvailable == true) {
                    TextButton(onClick = { viewModel.hideUpdateDialog() }) {
                        Text("Later", color = InkSecondary)
                    }
                }
            }
        )
    }
}


@Composable
private fun SettingsGroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = InkSecondary,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = InkPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(NeutralMuted, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = InkTertiary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun StealthTriggerVersionRow(
    version: String,
    onTrigger: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastTapTime > 500) {
                    tapCount = 1
                } else {
                    tapCount++
                }
                lastTapTime = now

                if (tapCount == 7) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTrigger()
                    tapCount = 0
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(NeutralMuted, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = InkPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Version",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = version,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }
    }
}
