package com.finansialku.app.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansialku.app.data.entity.UserEntity
import com.finansialku.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val user: UserEntity? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Initiate Google Sign-In flow.
     * @param context Activity context (required for Credential Manager)
     * @param webClientId OAuth 2.0 Web Client ID from Firebase Console
     */
    fun signInWithGoogle(context: Context, webClientId: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)

            val result = authRepository.signInWithGoogle(context, webClientId)

            result.fold(
                onSuccess = { user ->
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        isSuccess = true,
                        user = user
                    )
                },
                onFailure = { exception ->
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        error = exception.message ?: "Login gagal. Silakan coba lagi."
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
