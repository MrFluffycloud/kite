package com.expensevault.feature.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.DebtRepository
import com.expensevault.core.domain.repository.PersonRepository
import com.expensevault.core.model.DebtDirection
import com.expensevault.core.model.DebtRecord
import com.expensevault.core.model.Person
import com.expensevault.core.model.PersonDebtSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.math.BigDecimal

data class OpenDebtItem(
    val debt: DebtRecord,
    val personName: String
)

data class DebtDashboardUiState(
    val personSummaries: List<PersonDebtSummary> = emptyList(),
    val openDebts: List<OpenDebtItem> = emptyList(),
    val totalOwedToYou: BigDecimal = BigDecimal.ZERO,
    val totalYouOwe: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO,
    val selectedTab: Int = 0,
    val showAddPersonDialog: Boolean = false,
    val isLoading: Boolean = false
)

class DebtDashboardViewModel(
    private val personRepository: PersonRepository,
    private val debtRepository: DebtRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    private val _showAddPersonDialog = MutableStateFlow(false)

    val uiState: StateFlow<DebtDashboardUiState> = combine(
        personRepository.getPersonDebtSummaries(),
        debtRepository.getOpenDebts(),
        personRepository.getPersons(),
        _selectedTab,
        _showAddPersonDialog
    ) { summaries, debts, persons, tab, showDialog ->
        val personMap = persons.associateBy { it.id }
        
        var owedToYou = BigDecimal.ZERO
        var youOwe = BigDecimal.ZERO
        
        summaries.forEach { summary ->
            if (summary.netBalance > BigDecimal.ZERO) {
                owedToYou = owedToYou.add(summary.netBalance)
            } else if (summary.netBalance < BigDecimal.ZERO) {
                youOwe = youOwe.add(summary.netBalance.abs())
            }
        }
        
        val net = owedToYou.subtract(youOwe)
        
        val openDebtItems = debts.map { debt ->
            OpenDebtItem(
                debt = debt,
                personName = personMap[debt.personId]?.name ?: "Unknown"
            )
        }

        DebtDashboardUiState(
            personSummaries = summaries.sortedByDescending { it.netBalance.abs() },
            openDebts = openDebtItems,
            totalOwedToYou = owedToYou,
            totalYouOwe = youOwe,
            netBalance = net,
            selectedTab = tab,
            showAddPersonDialog = showDialog,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtDashboardUiState(isLoading = true)
    )

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun showAddPersonDialog() {
        _showAddPersonDialog.value = true
    }

    fun dismissAddPersonDialog() {
        _showAddPersonDialog.value = false
    }

    fun addPerson(name: String, phone: String?) {
        viewModelScope.launch {
            personRepository.addPerson(
                Person(
                    name = name,
                    phone = phone,
                    createdAt = Clock.System.now()
                )
            )
            _showAddPersonDialog.value = false
        }
    }
}
