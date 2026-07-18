package com.snehil.auracode.mainui.workspace.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.navigation.Routes
import com.snehil.auracode.domain.model.ChatMessage
import com.snehil.auracode.domain.model.MessageRole
import com.snehil.auracode.domain.model.StreamEvent
import com.snehil.auracode.domain.usecase.GetMessagesUseCase
import com.snehil.auracode.domain.usecase.StreamChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val streaming: Boolean = false,
    val streamingText: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessages: GetMessagesUseCase,
    private val streamChat: StreamChatUseCase
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var tempId = -1L
    private var streamJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val res = getMessages(projectId)) {
                is Resource.Success -> _state.update { it.copy(loading = false, messages = res.data) }
                is Resource.Error -> _state.update { it.copy(loading = false, error = res.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun onInputChange(value: String) = _state.update { it.copy(input = value) }

    fun applySuggestion(value: String) = _state.update { it.copy(input = value) }

    fun send() {
        val current = _state.value
        val prompt = current.input.trim()
        if (prompt.isBlank() || current.streaming) return

        val userMessage = ChatMessage(
            id = tempId--,
            content = prompt,
            role = MessageRole.USER,
            tokensUsed = null,
            createdAt = null
        )
        _state.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                streaming = true,
                streamingText = "",
                error = null
            )
        }

        streamJob = viewModelScope.launch {
            streamChat(projectId, prompt).collect { event ->
                when (event) {
                    is StreamEvent.Chunk ->
                        _state.update { it.copy(streamingText = it.streamingText + event.text) }

                    is StreamEvent.FileReady -> Unit // handled by the Code screen on next load

                    is StreamEvent.Failure -> {
                        finalizeStream()
                        _state.update { it.copy(error = event.message) }
                    }

                    StreamEvent.Completed -> finalizeStream()
                }
            }
        }
    }

    private fun finalizeStream() {
        _state.update { s ->
            val finalText = s.streamingText
            val newMessages = if (finalText.isNotBlank()) {
                s.messages + ChatMessage(
                    id = tempId--,
                    content = finalText,
                    role = MessageRole.ASSISTANT,
                    tokensUsed = null,
                    createdAt = null
                )
            } else {
                s.messages
            }
            s.copy(messages = newMessages, streaming = false, streamingText = "")
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}
