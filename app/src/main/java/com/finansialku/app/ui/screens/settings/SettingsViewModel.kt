package com.finansialku.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansialku.app.data.entity.UserEntity
import com.finansialku.app.data.repository.AuthRepository
import com.finansialku.app.data.repository.RecurringBillRepository
import com.finansialku.app.data.repository.TransactionRepository
import com.finansialku.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: UserEntity? = null,
    val isLoading: Boolean = true,
    val logoutSuccess: Boolean = false,
    val deleteSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringBillRepository: RecurringBillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val userId: String? get() = authRepository.getCurrentUserId()

    init {
        loadUser()
    }

    private fun loadUser() {
        val uid = userId ?: return
        viewModelScope.launch {
            val user = userRepository.getUserById(uid)
            _uiState.value = SettingsUiState(user = user, isLoading = false)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = _uiState.value.copy(logoutSuccess = true)
        }
    }

    fun deleteAllLocalData() {
        val uid = userId ?: return
        viewModelScope.launch {
            transactionRepository.deleteAllTransactionsByUser(uid)
            recurringBillRepository.deleteAllBillsByUser(uid)
            authRepository.signOutAndClearData(uid)
            _uiState.value = _uiState.value.copy(deleteSuccess = true)
        }
    }
}
