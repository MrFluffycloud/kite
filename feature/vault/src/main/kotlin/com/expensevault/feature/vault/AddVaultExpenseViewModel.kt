package com.expensevault.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensevault.core.domain.repository.VaultRepository
import com.expensevault.core.model.VaultExpense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode

data class AddVaultExpenseUiState(
    val amountString: String = "",
    val title: String = "",
    val category: String = "Personal",
    val note: String = "",
    val isSharedWithPartner: Boolean = false,
    val partnerShareString: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AddVaultExpenseViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddVaultExpenseUiState())
    val uiState: StateFlow<AddVaultExpenseUiState> = _uiState.asStateFlow()

    fun setAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { current ->
                val newPartnerShare = if (current.isSharedWithPartner && amount.isNotEmpty()) {
                    // Auto-default partner share to 50%
                    val dec = amount.toBigDecimalOrNull()
                    if (dec != null && dec > BigDecimal.ZERO) {
                        dec.divide(BigDecimal(2), 2, RoundingMode.HALF_UP).toString()
                    } else ""
                } else current.partnerShareString

                current.copy(
                    amountString = amount,
                    partnerShareString = newPartnerShare
                )
            }
        }
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun setSharedWithPartner(shared: Boolean) {
        _uiState.update { current ->
            val share = if (shared && current.amountString.isNotEmpty()) {
                val dec = current.amountString.toBigDecimalOrNull()
                if (dec != null && dec > BigDecimal.ZERO) {
                    dec.divide(BigDecimal(2), 2, RoundingMode.HALF_UP).toString()
                } else ""
            } else ""

            current.copy(
                isSharedWithPartner = shared,
                partnerShareString = share,
                category = if (shared) "Partner Shared" else current.category
            )
        }
    }

    fun setPartnerShare(share: String) {
        if (share.isEmpty() || share.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(partnerShareString = share) }
        }
    }

    fun save() {
        val state = _uiState.value
        val amount = state.amountString.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a title") }
            return
        }

        val partnerShare = if (state.isSharedWithPartner) {
            state.partnerShareString.toBigDecimalOrNull()
        } else null

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val now = Clock.System.now()
                val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
                vaultRepository.addVaultExpense(
                    VaultExpense(
                        id = 0,
                        amount = amount,
                        currency = "INR",
                        title = state.title.trim(),
                        note = state.note.trim().takeIf { it.isNotEmpty() },
                        category = state.category,
                        date = today,
                        isSharedWithPartner = state.isSharedWithPartner,
                        partnerShare = partnerShare,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save private expense")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
