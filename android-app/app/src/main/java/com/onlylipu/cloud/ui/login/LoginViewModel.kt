package com.onlylipu.cloud.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlylipu.cloud.data.auth.AuthRepository
import com.onlylipu.cloud.data.auth.AuthResult
import com.onlylipu.cloud.data.auth.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val tokenStore = TokenStore(app)
    private val repo = AuthRepository(tokenStore)

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginUiState(error = "Enter username and password")
            return
        }
        _state.value = LoginUiState(loading = true)
        viewModelScope.launch {
            when (val result = repo.login(username.trim(), password)) {
                is AuthResult.Success -> _state.value = LoginUiState(loggedIn = true)
                is AuthResult.Failure -> _state.value = LoginUiState(error = result.message)
            }
        }
    }

    fun loginWithSavedSession() {
        if (tokenStore.isLoggedIn()) _state.value = LoginUiState(loggedIn = true)
    }

    fun biometricAvailable(): Boolean = tokenStore.isLoggedIn()
}
