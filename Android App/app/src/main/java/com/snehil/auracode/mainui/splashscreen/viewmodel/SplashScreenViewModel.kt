package com.snehil.auracode.mainui.splashscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.usecase.GetMeUseCase
import com.snehil.auracode.domain.usecase.HasSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplashDestination { AUTHENTICATED, UNAUTHENTICATED }

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val hasSession: HasSessionUseCase,
    private val getMe: GetMeUseCase
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        decide()
    }

    private fun decide() {
        viewModelScope.launch {
            delay(600) // brief branding moment
            if (!hasSession()) {
                _destination.value = SplashDestination.UNAUTHENTICATED
                return@launch
            }
            _destination.value = when (getMe()) {
                is Resource.Success -> SplashDestination.AUTHENTICATED
                else -> SplashDestination.UNAUTHENTICATED
            }
        }
    }
}
