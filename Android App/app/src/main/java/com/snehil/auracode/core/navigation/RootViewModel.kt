package com.snehil.auracode.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.data.network.AuthEvent
import com.snehil.auracode.data.network.AuthEventBus
import com.snehil.auracode.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    authEventBus: AuthEventBus,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _forceLogout = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val forceLogout: SharedFlow<Unit> = _forceLogout.asSharedFlow()

    init {
        viewModelScope.launch {
            authEventBus.events.collect { event ->
                if (event is AuthEvent.Unauthorized) {
                    logoutUseCase()
                    _forceLogout.tryEmit(Unit)
                }
            }
        }
    }
}
