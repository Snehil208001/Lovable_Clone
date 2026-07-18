package com.snehil.auracode.mainui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.length >= 4 && !loading
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _loggedIn = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loggedIn: SharedFlow<Unit> = _loggedIn.asSharedFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun login() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val res = loginUseCase(current.email, current.password)) {
                is Resource.Success -> {
                    _state.update { it.copy(loading = false) }
                    _loggedIn.tryEmit(Unit)
                }
                is Resource.Error -> _state.update { it.copy(loading = false, error = res.message) }
                Resource.Loading -> Unit
            }
        }
    }
}
