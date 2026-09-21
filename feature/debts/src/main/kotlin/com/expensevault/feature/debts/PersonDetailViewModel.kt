package com.expensevault.feature.debts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.DebtRepository
import com.expensevault.core.domain.repository.PersonRepository
import com.expensevault.core.domain.usecase.SettleDebtUseCase
import com.expensevault.core.model.DebtRecord
import com.expensevault.core.model.DebtStatus
import com.expensevault.core.model.Person
import com.expensevault.core.model.Settlement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PersonDetailUiState(
    val person: Person? = null,
    val netBalance: BigDecimal = BigDecimal.ZERO,
    val openDebts: List<DebtRecord> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val showSettleDialog: Boolean = false,
    val isSettling: Boolean = false,
    val errorMessage: String? = null
)

class PersonDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val personRepository: PersonRepository,
    private val debtRepository: DebtRepository,
    private val settleDebtUseCase: SettleDebtUseCase
) : ViewModel() {

    private val personId: Long = savedStateHandle.get<Long>("personId") ?: 0L

    private val _uiState = MutableStateFlow(PersonDetailUiState())
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    init {
        if (personId > 0) {
            observePersonData(personId)
        }
    }

    fun setPersonId(id: Long) {
        observePersonData(id)
    }

    private fun observePersonData(id: Long) {
        viewModelScope.launch {
            val person = personRepository.getPersonById(id)
            _uiState.update { it.copy(person = person) }

            combine(
                debtRepository.getDebtsByPerson(id),
                debtRepository.getSettlementsByPerson(id)
            ) { allDebts, settlements ->
                val open = allDebts.filter { it.status == DebtStatus.OPEN }
                val balance = debtRepository.getNetBalance(id)
                Pair(open, settlements) to balance
            }.collect { (debtsAndSettlements, balance) ->
                _uiState.update {
                    it.copy(
                        openDebts = debtsAndSettlements.first,
                        settlements = debtsAndSettlements.second,
                        netBalance = balance
                    )
                }
            }
        }
    }

    fun showSettleDialog() {
        _uiState.update { it.copy(showSettleDialog = true) }
    }

    fun dismissSettleDialog() {
        _uiState.update { it.copy(showSettleDialog = false) }
    }

    fun settleDebts(note: String? = null, amount: BigDecimal? = null) {
        val currentPerson = _uiState.value.person ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSettling = true, showSettleDialog = false) }
            val result = settleDebtUseCase(currentPerson.id, note, amount)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSettling = false) }
            } else {
                _uiState.update {
                    it.copy(
                        isSettling = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to settle debts"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
