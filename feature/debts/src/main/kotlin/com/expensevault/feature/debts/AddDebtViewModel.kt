package com.expensevault.feature.debts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.DebtRepository
import com.expensevault.core.domain.repository.PersonRepository
import com.expensevault.core.model.DebtDirection
import com.expensevault.core.model.DebtRecord
import com.expensevault.core.model.DebtStatus
import com.expensevault.core.model.Person
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.math.BigDecimal

data class AddDebtUiState(
    val persons: List<Person> = emptyList(),
    val selectedPersonId: Long? = null,
    val amountString: String = "",
    val currency: String = "INR",
    val direction: DebtDirection = DebtDirection.THEY_OWE_ME,
    val note: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showAddPersonDialog: Boolean = false
)

class AddDebtViewModel(
    savedStateHandle: SavedStateHandle,
    private val personRepository: PersonRepository,
    private val debtRepository: DebtRepository
) : ViewModel() {

    private val initialPersonId: Long? = savedStateHandle.get<Long>("personId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(
        AddDebtUiState(selectedPersonId = initialPersonId)
    )
    val uiState: StateFlow<AddDebtUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            personRepository.getPersons().collect { list ->
                _uiState.update { it.copy(persons = list) }
            }
        }
    }

    fun selectPerson(id: Long) {
        _uiState.update { it.copy(selectedPersonId = id) }
    }

    fun setAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(amountString = amount) }
        }
    }

    fun setDirection(direction: DebtDirection) {
        _uiState.update { it.copy(direction = direction) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun showAddPersonDialog() {
        _uiState.update { it.copy(showAddPersonDialog = true) }
    }

    fun dismissAddPersonDialog() {
        _uiState.update { it.copy(showAddPersonDialog = false) }
    }

    fun createAndSelectPerson(name: String, phone: String?) {
        viewModelScope.launch {
            val newPersonId = personRepository.addPerson(
                Person(
                    name = name,
                    phone = phone,
                    createdAt = Clock.System.now()
                )
            )
            _uiState.update {
                it.copy(
                    selectedPersonId = newPersonId,
                    showAddPersonDialog = false
                )
            }
        }
    }

    fun saveDebt() {
        val state = _uiState.value
        val personId = state.selectedPersonId
        if (personId == null) {
            _uiState.update { it.copy(errorMessage = "Please select a person") }
            return
        }
        val amount = state.amountString.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                debtRepository.addDebt(
                    DebtRecord(
                        id = 0,
                        personId = personId,
                        amount = amount,
                        currency = state.currency,
                        direction = state.direction,
                        status = DebtStatus.OPEN,
                        note = state.note.takeIf { it.isNotBlank() },
                        createdAt = Clock.System.now()
                    )
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save debt")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
