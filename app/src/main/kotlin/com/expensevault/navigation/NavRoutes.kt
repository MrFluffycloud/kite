package com.expensevault.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {
    @Serializable data object Home : NavRoute
    @Serializable data object Transactions : NavRoute
    @Serializable data object Categories : NavRoute
    @Serializable data object Accounts : NavRoute
    @Serializable data object Settings : NavRoute
    @Serializable data class TransactionDetail(val transactionId: Long) : NavRoute
    @Serializable data class AddTransaction(
        val prefillAmount: String? = null,
        val prefillMerchant: String? = null,
        val prefillCategory: String? = null
    ) : NavRoute
    @Serializable data object Insights : NavRoute
    @Serializable data object Budgets : NavRoute
    @Serializable data object Debts : NavRoute
    @Serializable data class PersonDetail(val personId: Long) : NavRoute
    @Serializable data class AddDebt(val personId: Long = 0L, val debtId: Long = 0L) : NavRoute
    @Serializable data class SplitExpense(val transactionId: Long = 0L) : NavRoute
    @Serializable data object VaultLock : NavRoute
    @Serializable data object VaultDashboard : NavRoute
    @Serializable data object AddVaultExpense : NavRoute
    @Serializable data object RecurringList : NavRoute
    @Serializable data class AddEditRecurringRule(val ruleId: Long = 0L) : NavRoute
    @Serializable data object Onboarding : NavRoute
}
