package com.finansialku.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansialku.app.data.entity.TransactionEntity
import com.finansialku.app.data.repository.AuthRepository
import com.finansialku.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TransactionsUiState(
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true
)

data class TransactionFormState(
    val id: String? = null,
    val type: String = "EXPENSE",
    val amount: String = "",
    val category: String = "",
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val userId: String? get() = authRepository.getCurrentUserId()

    companion object {
        val EXPENSE_CATEGORIES = listOf(
            "Makanan & Minuman",
            "Transportasi",
            "Belanja",
            "Tagihan & Utilitas",
            "Hiburan",
            "Kesehatan",
            "Pendidikan",
            "Investasi",
            "Lainnya"
        )

        val INCOME_CATEGORIES = listOf(
            "Gaji",
            "Freelance",
            "Investasi",
            "Hadiah",
            "Penjualan",
            "Lainnya"
        )
    }

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        val uid = userId ?: return
        val state = _uiState.value

        viewModelScope.launch {
            transactionRepository.observeTransactionsByMonth(
                uid, state.currentYear, state.currentMonth
            ).collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    transactions = transactions,
                    isLoading = false
                )
            }
        }
    }

    fun goToPreviousMonth() {
        val state = _uiState.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, state.currentYear)
            set(Calendar.MONTH, state.currentMonth - 1)
            add(Calendar.MONTH, -1)
        }
        _uiState.value = state.copy(
            currentMonth = cal.get(Calendar.MONTH) + 1,
            currentYear = cal.get(Calendar.YEAR),
            isLoading = true
        )
        loadTransactions()
    }

    fun goToNextMonth() {
        val state = _uiState.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, state.currentYear)
            set(Calendar.MONTH, state.currentMonth - 1)
            add(Calendar.MONTH, 1)
        }
        _uiState.value = state.copy(
            currentMonth = cal.get(Calendar.MONTH) + 1,
            currentYear = cal.get(Calendar.YEAR),
            isLoading = true
        )
        loadTransactions()
    }

    // ==================== FORM ====================

    fun initNewTransaction() {
        _formState.value = TransactionFormState()
    }

    fun initEditTransaction(transaction: TransactionEntity) {
        _formState.value = TransactionFormState(
            id = transaction.id,
            type = transaction.type,
            amount = transaction.amount.toLong().toString(),
            category = transaction.category,
            date = transaction.date,
            note = transaction.note ?: "",
            isEditing = true
        )
    }

    fun updateFormType(type: String) {
        _formState.value = _formState.value.copy(type = type, category = "")
    }

    fun updateFormAmount(amount: String) {
        // Only allow digits
        val filtered = amount.filter { it.isDigit() }
        _formState.value = _formState.value.copy(amount = filtered)
    }

    fun updateFormCategory(category: String) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun updateFormDate(date: Long) {
        _formState.value = _formState.value.copy(date = date)
    }

    fun updateFormNote(note: String) {
        _formState.value = _formState.value.copy(note = note)
    }

    fun clearFormError() {
        _formState.value = _formState.value.copy(error = null)
    }

    fun saveTransaction() {
        val uid = userId ?: return
        val form = _formState.value

        // Validation
        val amountValue = form.amount.toDoubleOrNull()
        if (amountValue == null || amountValue < 1) {
            _formState.value = form.copy(error = "Nominal harus minimal Rp 1")
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
                    // Update existing
                    val entity = TransactionEntity(
                        id = form.id,
                        userId = uid,
                        type = form.type,
                        amount = amountValue,
                        category = form.category,
                        date = form.date,
                        note = form.note.ifBlank { null }
                    )
                    transactionRepository.updateTransaction(entity)
                } else {
                    // Create new
                    val entity = transactionRepository.createNewTransaction(
                        userId = uid,
                        type = form.type,
                        amount = amountValue,
                        category = form.category,
                        date = form.date,
                        note = form.note.ifBlank { null }
                    )
                    transactionRepository.insertTransaction(entity)
                }
                _formState.value = _formState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Gagal menyimpan transaksi"
                )
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
