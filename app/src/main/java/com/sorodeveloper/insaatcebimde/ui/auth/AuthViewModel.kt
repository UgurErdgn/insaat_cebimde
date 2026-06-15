package com.sorodeveloper.insaatcebimde.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val nameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(nameInput = name, errorMessage = null) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(emailInput = email, errorMessage = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.emailInput.isBlank() || state.passwordInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "E-posta ve şifre boş olamaz.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            authRepository.loginUser(state.emailInput, state.passwordInput).onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Giriş başarısız oldu.") }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.nameInput.isBlank() || state.emailInput.isBlank() || state.passwordInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Tüm alanları doldurunuz.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            authRepository.registerUser(state.nameInput, state.emailInput, state.passwordInput).onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Kayıt başarısız oldu.") }
            }
        }
    }
}
