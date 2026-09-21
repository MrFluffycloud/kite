package com.expensevault.feature.debts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.PersonRepository
import com.expensevault.core.domain.repository.TransactionRepository
import com.expensevault.core.domain.usecase.SplitExpenseUseCase
import com.expensevault.core.model.Person
import com.expensevault.core.model.SplitMethod
import com.expensevault.core.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.math.BigDecimal
import java.math.RoundingMode

data class SplitExpenseUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedTransaction: Transaction? = null,
    val persons: List<Person> = emptyList(),
    val selectedPersonIds: Set<Long> = emptySet(),
    val splitMethod: SplitMethod = SplitMethod.EQUAL,
    val customAmounts: Map<Long, String> = emptyMap(),
    val customPercentages: Map<Long, String> = emptyMap(),
    val isFullAmount: Boolean = true,
    val portionAmountString: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showAddPersonDialog: Boolean = false
)

class SplitExpenseViewModel(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val personRepository: PersonRepository,
    private val splitExpenseUseCase: SplitExpenseUseCase
) : ViewModel() {

    private val initialTransactionId: Long? = savedStateHandle.get<Long>("transactionId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(SplitExpenseUiState())
    val uiState: StateFlow<SplitExpenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            personRepository.getPersons().collect { list ->
                _uiState.update { it.copy(persons = list) }
            }
        }

        viewModelScope.launch {
            transactionRepository.getTransactions().collect { list ->
                _uiState.update { current ->
                    val preselected = if (initialTransactionId != null) {
                        list.find { it.id == initialTransactionId }
                    } else {
                        current.selectedTransaction ?: list.firstOrNull()
                    }
                    current.copy(
                        transactions = list,
                        selectedTransaction = preselected
                    )
                }
            }
        }
    }

    fun selectTransaction(tx: Transaction) {
        _uiState.update { it.copy(selectedTransaction = tx) }
    }

    fun togglePerson(personId: Long) {
        _uiState.update { current ->
            val set = current.selectedPersonIds.toMutableSet()
            if (set.contains(personId)) {
                set.remove(personId)
            } else {
                set.add(personId)
            }
            current.copy(selectedPersonIds = set)
        }
    }

    fun setSplitMethod(method: SplitMethod) {
        _uiState.update { it.copy(splitMethod = method) }
    }

    fun setCustomAmount(personId: Long, amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { current ->
                val map = current.customAmounts.toMutableMap()
                map[personId] = amount
                current.copy(customAmounts = map)
            }
        }
    }

    fun setCustomPercentage(personId: Long, pct: String) {
        if (pct.isEmpty() || pct.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { current ->
                val map = current.customPercentages.toMutableMap()
                map[personId] = pct
                current.copy(customPercentages = map)
            }
        }
    }

    fun showAddPersonDialog() {
        _uiState.update { it.copy(showAddPersonDialog = true) }
    }

    fun dismissAddPersonDialog() {
        _uiState.update { it.copy(showAddPersonDialog = false) }
    }

    fun addPerson(name: String, phone: String?) {
        viewModelScope.launch {
            val id = personRepository.addPerson(
                Person(name = name, phone = phone, createdAt = Clock.System.now())
            )
            _uiState.update { current ->
                val set = current.selectedPersonIds.toMutableSet()
                set.add(id)
                current.copy(
                    selectedPersonIds = set,
                    showAddPersonDialog = false
                )
            }
        }
    }

    fun setIsFullAmount(isFull: Boolean) {
        _uiState.update { current ->
            current.copy(
                isFullAmount = isFull,
                portionAmountString = if (!isFull && current.portionAmountString.isEmpty()) {
                    current.selectedTransaction?.originalAmount?.toPlainString() ?: ""
                } else current.portionAmountString
            )
        }
    }

    fun setPortionAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(portionAmountString = amount) }
        }
    }

    fun getEffectiveSplitAmount(): BigDecimal {
        val state = _uiState.value
        val tx = state.selectedTransaction ?: return BigDecimal.ZERO
        return if (state.isFullAmount) {
            tx.originalAmount
        } else {
            state.portionAmountString.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }
    }

    fun calculateShareForPerson(personId: Long): BigDecimal {
        val state = _uiState.value
        val total = getEffectiveSplitAmount()
        if (total <= BigDecimal.ZERO) return BigDecimal.ZERO

        return when (state.splitMethod) {
            SplitMethod.EQUAL -> {
                val count = (state.selectedPersonIds.size + 1).toBigDecimal()
                total.divide(count, 2, RoundingMode.HALF_UP)
            }
            SplitMethod.CUSTOM_AMOUNT -> {
                state.customAmounts[personId]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            }
            SplitMethod.PERCENTAGE -> {
                val pct = state.customPercentages[personId]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                total.multiply(pct).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            }
        }
    }

    fun saveSplit() {
        val state = _uiState.value
        val tx = state.selectedTransaction
        if (tx == null) {
            _uiState.update { it.copy(errorMessage = "Please select a transaction to split") }
            return
        }
        if (state.selectedPersonIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one person") }
            return
        }

        val effectiveAmount = getEffectiveSplitAmount()
        if (effectiveAmount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount to split") }
            return
        }
        if (!state.isFullAmount && effectiveAmount > tx.originalAmount) {
            _uiState.update { it.copy(errorMessage = "Portion to split cannot exceed transaction total (${tx.originalAmount})") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val customAmountsMap = state.customAmounts.mapValues { it.value.toBigDecimalOrNull() ?: BigDecimal.ZERO }
            val customPctsMap = state.customPercentages.mapValues { it.value.toBigDecimalOrNull() ?: BigDecimal.ZERO }

            val result = splitExpenseUseCase(
                transaction = tx,
                personIds = state.selectedPersonIds.toList(),
                splitMethod = state.splitMethod,
                customAmounts = customAmountsMap,
                customPercentages = customPctsMap,
                splitAmount = effectiveAmount
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to split expense"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
