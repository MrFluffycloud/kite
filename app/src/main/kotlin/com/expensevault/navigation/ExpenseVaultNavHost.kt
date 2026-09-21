package com.expensevault.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.expensevault.ui.motion.rememberReduceMotion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.expensevault.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensevault.feature.home.HomeScreen
import com.expensevault.feature.transactions.TransactionListScreen
import com.expensevault.feature.transactions.AddTransactionScreen
import com.expensevault.feature.categories.CategoryScreen
import com.expensevault.feature.accounts.AccountScreen
import com.expensevault.feature.accounts.BinanceWeb3Screen
import com.expensevault.feature.settings.SettingsScreen
import com.expensevault.feature.insights.InsightsScreen
import com.expensevault.feature.insights.BudgetScreen
import com.expensevault.feature.debts.DebtDashboardScreen
import com.expensevault.feature.debts.PersonDetailScreen
import com.expensevault.feature.debts.AddDebtScreen
import com.expensevault.feature.debts.SplitExpenseScreen
import com.expensevault.feature.vault.VaultLockScreen
import com.expensevault.feature.vault.VaultDashboardScreen
import com.expensevault.feature.vault.AddVaultExpenseScreen
import com.expensevault.feature.recurring.RecurringListScreen
import com.expensevault.feature.recurring.AddEditRecurringRuleScreen
import androidx.navigation.toRoute
import androidx.compose.material.icons.filled.ShowChart

import com.expensevault.feature.settings.OnboardingScreen
import com.expensevault.feature.settings.AppLockScreen
import com.expensevault.platform.security.AppLockManager
import com.expensevault.platform.security.BiometricAuthManager
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.platform.LocalContext
import android.content.Context

@Composable
fun KiteNavHost(
    startDestinationOverride: NavRoute? = null,
    appLockManager: AppLockManager? = null,
    biometricAuthManager: BiometricAuthManager? = null
) {
    val isAppLockEnabled by (appLockManager?.isAppLockEnabled ?: MutableStateFlow(false)).collectAsState()
    val isUnlocked by (appLockManager?.isUnlocked ?: MutableStateFlow(true)).collectAsState()

    var isAppReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(650)
        isAppReady = true
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val context = LocalContext.current
    val isOnboardingDone = remember {
        val prefs = context.getSharedPreferences("expense_vault_settings", Context.MODE_PRIVATE)
        prefs.getBoolean("is_onboarding_completed", false)
    }
    val defaultStart = if (isOnboardingDone) NavRoute.Home else NavRoute.Onboarding
    val startRoute = startDestinationOverride ?: defaultStart

    val isFullscreenRoute = currentDestination?.hierarchy?.any {
        val r = it.route.orEmpty()
        r.contains("Onboarding") || r.contains("Vault") || r.contains("Add") || r.contains("Split") ||
        r.contains("Debt") || r.contains("Recurring") || r.contains("PersonDetail") || r.contains("Binance")
    } == true

    val navTabs = remember(currentDestination) {
        listOf(
            NavTabItem(
                icon = Icons.Outlined.Home,
                contentDescription = "Home",
                isSelected = currentDestination?.hierarchy?.any { it.route?.contains("Home") == true } == true,
                onClick = {
                    navController.navigate(NavRoute.Home) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ),
            NavTabItem(
                icon = Icons.Outlined.ReceiptLong,
                contentDescription = "Transactions",
                isSelected = currentDestination?.hierarchy?.any { it.route?.contains("Transactions") == true } == true,
                onClick = {
                    navController.navigate(NavRoute.Transactions) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ),
            NavTabItem(
                icon = Icons.Outlined.ShowChart,
                contentDescription = "Insights",
                isSelected = currentDestination?.hierarchy?.any { it.route?.contains("Insights") == true } == true,
                onClick = {
                    navController.navigate(NavRoute.Insights) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ),
            NavTabItem(
                icon = Icons.Outlined.AccountBalanceWallet,
                contentDescription = "Accounts",
                isSelected = currentDestination?.hierarchy?.any { it.route?.contains("Accounts") == true } == true,
                onClick = {
                    navController.navigate(NavRoute.Accounts) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ),
            NavTabItem(
                icon = Icons.Outlined.Settings,
                contentDescription = "Settings",
                isSelected = currentDestination?.hierarchy?.any { it.route?.contains("Settings") == true } == true,
                onClick = {
                    navController.navigate(NavRoute.Settings) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        )
    }

    val selectedTabIndex = navTabs.indexOfFirst { it.isSelected }.let { if (it >= 0) it else 0 }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFFFAFAF9),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isFullscreenRoute) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingHomebar(
                        tabs = navTabs,
                        selectedIndex = selectedTabIndex
                    )
                }
            }
        },
        floatingActionButton = {
            val isHomeOrTransactions = currentDestination?.hierarchy?.any {
                val r = it.route.orEmpty()
                r.contains("Home") || r.contains("Transactions")
            } == true
            if (isHomeOrTransactions) {
                FloatingActionButton(
                    onClick = { navController.navigate(NavRoute.AddTransaction()) },
                    containerColor = Color(0xFF0F0F0F),
                    contentColor = Color(0xFFFAFAF9),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp, end = 4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add transaction")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<NavRoute.Onboarding> {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(NavRoute.Home) {
                            popUpTo(NavRoute.Onboarding) { inclusive = true }
                        }
                    }
                )
            }
            composable<NavRoute.Home> {
                HomeScreen(
                    onNavigateToTransactions = {
                        navController.navigate(NavRoute.Transactions) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToDebts = {
                        navController.navigate(NavRoute.Debts)
                    },
                    onNavigateToRecurring = {
                        navController.navigate(NavRoute.RecurringList)
                    }
                )
            }
            composable<NavRoute.Transactions> { TransactionListScreen() }
            composable<NavRoute.Categories> { CategoryScreen() }
            composable<NavRoute.Accounts> {
                AccountScreen(
                    onNavigateToBinance = { navController.navigate(NavRoute.BinanceWeb3) }
                )
            }
            composable<NavRoute.Settings> {
                SettingsScreen(
                    onVaultTrigger = {
                        navController.navigate(NavRoute.VaultLock)
                    },
                    onNavigateToBinance = { navController.navigate(NavRoute.BinanceWeb3) }
                )
            }
            composable<NavRoute.AddTransaction> { backStackEntry ->
                val route = backStackEntry.toRoute<NavRoute.AddTransaction>()
                AddTransactionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    prefillAmount = route.prefillAmount,
                    prefillMerchant = route.prefillMerchant,
                    prefillCategory = route.prefillCategory
                )
            }
            composable<NavRoute.TransactionDetail> {
                // Placeholder for transaction detail/edit (Milestone 2)
            }
            composable<NavRoute.Insights> {
                InsightsScreen(onTransactionClick = {})
            }
            composable<NavRoute.Budgets> {
                BudgetScreen()
            }
            composable<NavRoute.Debts> {
                DebtDashboardScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPersonClick = { personId ->
                        navController.navigate(NavRoute.PersonDetail(personId))
                    },
                    onAddDebtClick = {
                        navController.navigate(NavRoute.AddDebt())
                    },
                    onSplitExpenseClick = {
                        navController.navigate(NavRoute.SplitExpense())
                    },
                    onDebtClick = { personId, debtId ->
                        navController.navigate(NavRoute.AddDebt(personId = personId, debtId = debtId))
                    }
                )
            }
            composable<NavRoute.PersonDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<NavRoute.PersonDetail>()
                PersonDetailScreen(
                    personId = route.personId,
                    onNavigateBack = { navController.popBackStack() },
                    onAddDebtForPerson = { id ->
                        navController.navigate(NavRoute.AddDebt(personId = id))
                    },
                    onEditDebt = { debtId ->
                        navController.navigate(NavRoute.AddDebt(personId = route.personId, debtId = debtId))
                    }
                )
            }
            composable<NavRoute.AddDebt> {
                AddDebtScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<NavRoute.SplitExpense> {
                SplitExpenseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<NavRoute.VaultLock> {
                VaultLockScreen(
                    onUnlocked = {
                        navController.navigate(NavRoute.VaultDashboard) {
                            popUpTo(NavRoute.VaultLock) { inclusive = true }
                        }
                    },
                    onDismiss = {
                        navController.popBackStack()
                    }
                )
            }
            composable<NavRoute.VaultDashboard> {
                VaultDashboardScreen(
                    onAddExpense = {
                        navController.navigate(NavRoute.AddVaultExpense)
                    },
                    onLocked = {
                        navController.popBackStack()
                    }
                )
            }
            composable<NavRoute.AddVaultExpense> {
                AddVaultExpenseScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable<NavRoute.RecurringList> {
                RecurringListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAddRule = { navController.navigate(NavRoute.AddEditRecurringRule()) }
                )
            }
            composable<NavRoute.AddEditRecurringRule> {
                AddEditRecurringRuleScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<NavRoute.BinanceWeb3> {
                BinanceWeb3Screen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (isAppLockEnabled && !isUnlocked && appLockManager != null && biometricAuthManager != null) {
        AppLockScreen(
            appLockManager = appLockManager,
            biometricAuthManager = biometricAuthManager
        )
    }

    AnimatedVisibility(
        visible = !isAppReady,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(400))
    ) {
        KiteStartupLoader()
    }
}
}

@Composable
private fun KiteStartupLoader() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAF9)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_kite_logo_black),
                contentDescription = "Kite",
                modifier = Modifier.size(92.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Kite",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFF0F0F0F)
            )
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.2.dp,
                color = Color(0xFF0F0F0F),
                trackColor = Color(0xFFE7E6E3)
            )
        }
    }
}

private data class NavTabItem(
    val icon: ImageVector,
    val contentDescription: String,
    val isSelected: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun FloatingHomebar(
    tabs: List<NavTabItem>,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, Color(0xFFE7E6E3)),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val count = tabs.size.coerceAtLeast(1)
            val tabWidth = maxWidth / count
            val pillHeight = 46.dp
            val pillWidth = minOf(tabWidth - 10.dp, 56.dp)
            val targetHighlightX = (tabWidth * selectedIndex) + (tabWidth - pillWidth) / 2

            val reduceMotion = rememberReduceMotion()
            val animatedHighlightX by animateDpAsState(
                targetValue = targetHighlightX,
                animationSpec = if (reduceMotion) snap() else spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "homebarHighlightX"
            )

            // 1. Sliding Active Near-Black Pill Highlight (#111111, fully contained within bounds)
            Box(
                modifier = Modifier
                    .offset(x = animatedHighlightX)
                    .align(Alignment.CenterStart)
                    .size(width = pillWidth, height = pillHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111111))
            )

            // 2. Tab Icons row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.contentDescription,
                            tint = if (isSelected) Color(0xFFFAFAF9) else Color(0xFF6B6B68),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 3. Full touch targets across each tab column
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                tabs.forEach { tab ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = tab.onClick
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseVaultNavHost(
    startDestinationOverride: NavRoute? = null,
    appLockManager: AppLockManager? = null,
    biometricAuthManager: BiometricAuthManager? = null
) {
    KiteNavHost(
        startDestinationOverride = startDestinationOverride,
        appLockManager = appLockManager,
        biometricAuthManager = biometricAuthManager
    )
}

