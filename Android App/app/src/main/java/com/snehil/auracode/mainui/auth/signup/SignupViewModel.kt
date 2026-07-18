package com.snehil.auracode.mainui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.usecase.SignupUseCase
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

data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && name.length <= 30 &&
            email.isNotBlank() && password.length >= 4 && !loading
}

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupUseCase: SignupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SignupUiState())
    val state: StateFlow<SignupUiState> = _state.asStateFlow()

    private val _signedUp = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signedUp: SharedFlow<Unit> = _signedUp.asSharedFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signup() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val res = signupUseCase(current.name, current.email, current.password)) {
                is Resource.Success -> {
                    _state.update { it.copy(loading = false) }
                    _signedUp.tryEmit(Unit)
                }
                is Resource.Error -> _state.update { it.copy(loading = false, error = res.message) }
                Resource.Loading -> Unit
            }
        }
    }
}
