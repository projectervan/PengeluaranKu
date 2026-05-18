package com.finansialku.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansialku.app.data.entity.CategoryExpense
import com.finansialku.app.data.entity.TransactionEntity
import com.finansialku.app.data.repository.AuthRepository
import com.finansialku.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val expenseByCategory: List<CategoryExpense> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val userId: String? get() = authRepository.getCurrentUserId()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val uid = userId ?: return
        val state = _uiState.value

        viewModelScope.launch {
            combine(
                transactionRepository.observeTotalIncomeByMonth(uid, state.currentYear, state.currentMonth),
                transactionRepository.observeTotalExpenseByMonth(uid, state.currentYear, state.currentMonth),
                transactionRepository.observeExpenseByCategory(uid, state.currentYear, state.currentMonth),
                transactionRepository.observeRecentTransactions(uid, 5)
            ) { income, expense, categories, recent ->
                DashboardUiState(
                    currentMonth = state.currentMonth,
                    currentYear = state.currentYear,
                    totalIncome = income,
                    totalExpense = expense,
                    balance = income - expense,
                    expenseByCategory = categories,
                    recentTransactions = recent,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
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
        loadDashboardData()
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
        loadDashboardData()
    }
}
