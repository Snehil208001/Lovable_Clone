package com.snehil.auracode.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Shared base for feature ViewModels: exposes a one-shot message channel
 * (for snackbars/toasts) and a small coroutine launch helper.
 */
abstract class BaseViewModel : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    protected fun emitMessage(message: String) {
        _messages.tryEmit(message)
    }

    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
