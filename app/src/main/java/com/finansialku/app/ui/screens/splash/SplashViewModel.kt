package com.finansialku.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansialku.app.data.repository.AuthRepository
import com.finansialku.app.data.repository.RecurringBillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    data object Loading : SplashDestination()
    data object Login : SplashDestination()
    data object Main : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val recurringBillRepository: RecurringBillRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkAuthAndGenerate()
    }

    private fun checkAuthAndGenerate() {
        viewModelScope.launch {
            if (authRepository.isUserSignedIn()) {
                // User is already signed in — run recurring bill generation (PRD 5.1)
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    recurringBillRepository.checkAndGenerateRecurringBills(userId)
                }
                _destination.value = SplashDestination.Main
            } else {
                _destination.value = SplashDestination.Login
            }
        }
    }
}
