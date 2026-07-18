package com.snehil.auracode.data.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Broadcasts global auth events (e.g. a 401 from any request) so the app can
 * force a logout and route back to the login screen.
 */
@Singleton
class AuthEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun notifyUnauthorized() {
        _events.tryEmit(AuthEvent.Unauthorized)
    }
}

sealed interface AuthEvent {
    data object Unauthorized : AuthEvent
}
