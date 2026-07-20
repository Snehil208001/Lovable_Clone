package com.snehil.auracode.data.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-tab workspace signals: preview auto-fix ↔ chat activity ↔ preview refresh.
 */
@Singleton
class PreviewRepairBus @Inject constructor() {
    private val _events = MutableSharedFlow<PreviewRepairEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<PreviewRepairEvent> = _events.asSharedFlow()

    fun emit(event: PreviewRepairEvent) {
        _events.tryEmit(event)
    }
}

sealed interface PreviewRepairEvent {
    data class FixStarted(val errorSummary: String) : PreviewRepairEvent
    data object FixFinished : PreviewRepairEvent
    /** Chat finished applying files — preview should soft-reload. */
    data object ChatBuildFinished : PreviewRepairEvent
}
