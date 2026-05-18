package com.finansialku.app.ui.screens.recurringbills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansialku.app.data.entity.RecurringBillEntity
import com.finansialku.app.data.repository.AuthRepository
import com.finansialku.app.data.repository.RecurringBillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringBillsUiState(
    val bills: List<RecurringBillEntity> = emptyList(),
    val isLoading: Boolean = true
)

data class BillFormState(
    val id: String? = null,
    val billName: String = "",
    val amount: String = "",
    val category: String = "Tagihan & Utilitas",
    val dueDay: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class RecurringBillsViewModel @Inject constructor(
    private val recurringBillRepository: RecurringBillRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringBillsUiState())
    val uiState: StateFlow<RecurringBillsUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(BillFormState())
    val formState: StateFlow<BillFormState> = _formState.asStateFlow()

    private val userId: String? get() = authRepository.getCurrentUserId()

    companion object {
        val BILL_CATEGORIES = listOf(
            "Tagihan & Utilitas",
            "Internet & WiFi",
            "Kost / Sewa",
            "Asuransi",
            "Cicilan",
            "Langganan",
            "Transportasi",
            "Lainnya"
        )
    }

    init {
        loadBills()
    }

    private fun loadBills() {
        val uid = userId ?: return
        viewModelScope.launch {
            recurringBillRepository.observeAllRecurringBills(uid).collect { bills ->
                _uiState.value = RecurringBillsUiState(
                    bills = bills,
                    isLoading = false
                )
            }
        }
    }

    fun toggleBillActive(bill: RecurringBillEntity) {
        viewModelScope.launch {
            recurringBillRepository.updateActiveStatus(bill.id, !bill.isActive)
        }
    }

    fun deleteBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            recurringBillRepository.deleteRecurringBill(bill)
        }
    }

    // ==================== FORM ====================

    fun initNewBill() {
        _formState.value = BillFormState()
    }

    fun initEditBill(bill: RecurringBillEntity) {
        _formState.value = BillFormState(
            id = bill.id,
            billName = bill.billName,
            amount = bill.amount.toLong().toString(),
            category = bill.category,
            dueDay = bill.dueDay.toString(),
            isEditing = true
        )
    }

    fun updateBillName(name: String) {
        _formState.value = _formState.value.copy(billName = name)
    }

    fun updateAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() }
        _formState.value = _formState.value.copy(amount = filtered)
    }

    fun updateCategory(category: String) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun updateDueDay(day: String) {
        val filtered = day.filter { it.isDigit() }.take(2)
        _formState.value = _formState.value.copy(dueDay = filtered)
    }

    fun clearFormError() {
        _formState.value = _formState.value.copy(error = null)
    }

    fun resetFormSuccess() {
        _formState.value = _formState.value.copy(saveSuccess = false)
    }

    fun saveBill() {
        val uid = userId ?: return
        val form = _formState.value

        // Validation
        if (form.billName.isBlank()) {
            _formState.value = form.copy(error = "Nama tagihan tidak boleh kosong")
            return
        }
        val amountValue = form.amount.toDoubleOrNull()
        if (amountValue == null || amountValue < 1) {
            _formState.value = form.copy(error = "Nominal harus minimal Rp 1")
            return
        }
        val dueDayValue = form.dueDay.toIntOrNull()
        if (dueDayValue == null || dueDayValue < 1 || dueDayValue > 31) {
            _formState.value = form.copy(error = "Tanggal jatuh tempo harus antara 1-31")
            return
        }
        if (form.category.isBlank()) {
            _formState.value = form.copy(error = "Kategori tidak boleh kosong")
            return
        }

        _formState.value = form.copy(isSaving = true)

        viewModelScope.launch {
            try {
                if (form.isEditing && form.id != null) {
                    val existing = recurringBillRepository.getRecurringBillById(form.id)
                    if (existing != null) {
                        val updated = existing.copy(
                            billName = form.billName,
                            amount = amountValue,
                            category = form.category,
                            dueDay = dueDayValue
                        )
                        recurringBillRepository.updateRecurringBill(updated)
                    }
                } else {
                    val newBill = recurringBillRepository.createNewRecurringBill(
                        userId = uid,
                        billName = form.billName,
                        amount = amountValue,
                        category = form.category,
                        dueDay = dueDayValue
                    )
                    recurringBillRepository.insertRecurringBill(newBill)
                }
                _formState.value = _formState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Gagal menyimpan tagihan"
                )
            }
        }
    }
}
