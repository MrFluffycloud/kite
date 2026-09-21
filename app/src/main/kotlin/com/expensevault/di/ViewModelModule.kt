package com.expensevault.di

import com.expensevault.feature.home.HomeViewModel
import com.expensevault.feature.transactions.TransactionListViewModel
import com.expensevault.feature.transactions.AddTransactionViewModel
import com.expensevault.feature.categories.CategoryViewModel
import com.expensevault.feature.accounts.AccountViewModel
import com.expensevault.feature.settings.SettingsViewModel
import com.expensevault.feature.insights.InsightsViewModel
import com.expensevault.feature.insights.BudgetViewModel
import com.expensevault.feature.debts.DebtDashboardViewModel
import com.expensevault.feature.debts.PersonDetailViewModel
import com.expensevault.feature.debts.AddDebtViewModel
import com.expensevault.feature.debts.SplitExpenseViewModel
import com.expensevault.feature.vault.VaultLockViewModel
import com.expensevault.feature.vault.VaultDashboardViewModel
import com.expensevault.feature.vault.AddVaultExpenseViewModel
import com.expensevault.feature.recurring.RecurringListViewModel
import com.expensevault.feature.recurring.AddEditRecurringRuleViewModel
import com.expensevault.feature.settings.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::TransactionListViewModel)
    viewModelOf(::AddTransactionViewModel)
    viewModelOf(::CategoryViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::InsightsViewModel)
    viewModelOf(::BudgetViewModel)
    viewModelOf(::DebtDashboardViewModel)
    viewModelOf(::PersonDetailViewModel)
    viewModelOf(::AddDebtViewModel)
    viewModelOf(::SplitExpenseViewModel)
    viewModelOf(::VaultLockViewModel)
    viewModelOf(::VaultDashboardViewModel)
    viewModelOf(::AddVaultExpenseViewModel)
    viewModelOf(::RecurringListViewModel)
    viewModelOf(::AddEditRecurringRuleViewModel)
    viewModelOf(::OnboardingViewModel)
}
