package com.snehil.auracode.mainui.workspace.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.navigation.Routes
import com.snehil.auracode.data.network.PreviewRepairBus
import com.snehil.auracode.data.network.PreviewRepairEvent
import com.snehil.auracode.domain.model.ChatMessage
import com.snehil.auracode.domain.model.MessageRole
import com.snehil.auracode.domain.model.StreamEvent
import com.snehil.auracode.domain.usecase.GetMessagesUseCase
import com.snehil.auracode.domain.usecase.StreamChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val streamingText: String = "",
    /** Live file paths applied during the current stream (vibe activity feed). */
    val liveFiles: List<String> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessages: GetMessagesUseCase,
    private val streamChat: StreamChatUseCase,
    private val repairBus: PreviewRepairBus
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var tempId = -1L
    private var streamJob: Job? = null
    private var idleFinalizeJob: Job? = null

    init {
        load()
        observePreviewRepairs()
    }

    private fun observePreviewRepairs() {
        viewModelScope.launch {
            repairBus.events.collect { event ->
                when (event) {
                    is PreviewRepairEvent.FixStarted -> {
                        // Mirror auto-fix in chat so the user sees seamless repair activity.
                        if (_state.value.streaming) return@collect
                        val summary = event.errorSummary.lineSequence().firstOrNull()?.take(120).orEmpty()
                        _state.update {
                            it.copy(
                                messages = it.messages + ChatMessage(
                                    id = tempId--,
                                    content = "Auto-fix preview: $summary",
                                    role = MessageRole.USER,
                                    tokensUsed = null,
                                    createdAt = null
                                ) + ChatMessage(
                                    id = tempId--,
                                    content = "[Writing] preview fix…\n",
                                    role = MessageRole.ASSISTANT,
                                    tokensUsed = null,
                                    createdAt = null
                                )
                            )
                        }
                    }
                    PreviewRepairEvent.FixFinished -> {
                        _state.update { s ->
                            val last = s.messages.lastOrNull()
                            if (last?.role == MessageRole.ASSISTANT && last.content.contains("preview fix")) {
                                s.copy(
                                    messages = s.messages.dropLast(1) + last.copy(
                                        content = "[Wrote] preview fix\nReady — open Preview to see your app."
                                    )
                                )
                            } else {
                                s.copy(
                                    messages = s.messages + ChatMessage(
                                        id = tempId--,
                                        content = "[Wrote] preview fix\nReady — open Preview to see your app.",
                                        role = MessageRole.ASSISTANT,
                                        tokensUsed = null,
                                        createdAt = null
                                    )
                                )
                            }
                        }
                    }
                    PreviewRepairEvent.ChatBuildFinished -> Unit // handled by PreviewViewModel
                }
            }
        }
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
                liveFiles = emptyList(),
                error = null
            )
        }

        streamJob = viewModelScope.launch {
            streamChat(projectId, prompt).collect { event ->
                when (event) {
                    is StreamEvent.Chunk -> {
                        _state.update { it.copy(streamingText = it.streamingText + event.text) }
                        scheduleIdleFinalize()
                    }

                    is StreamEvent.FileReady -> {
                        _state.update {
                            val path = shortPath(event.path)
                            if (path in it.liveFiles) it
                            else it.copy(liveFiles = it.liveFiles + path)
                        }
                        scheduleIdleFinalize()
                    }

                    is StreamEvent.Failure -> {
                        finalizeStream()
                        _state.update { it.copy(error = event.message) }
                    }

                    StreamEvent.Completed -> finalizeStream()
                }
            }
        }
    }

    /**
     * If the SSE connection hangs after files are applied (no more Writing),
     * seal the message so chat shows Completed instead of forever "Styling…".
     */
    private fun scheduleIdleFinalize() {
        idleFinalizeJob?.cancel()
        idleFinalizeJob = viewModelScope.launch {
            delay(2_200)
            val s = _state.value
            if (!s.streaming) return@launch

            val actions = parseChatTimeline(s.streamingText, complete = false)
                .filterIsInstance<ChatTimelineItem.Action>()
            val unresolvedWriting = actions.any {
                it.action == ChatAction.WRITING && it.detail !in s.liveFiles
            }
            val hasWork = s.streamingText.isNotBlank() || s.liveFiles.isNotEmpty()
            if (!hasWork) return@launch

            if (!unresolvedWriting) {
                finalizeStream()
                return@launch
            }

            // Files already applied but one write never closed — seal after a short grace.
            if (s.liveFiles.isNotEmpty() || actions.any { it.action == ChatAction.WROTE }) {
                delay(3_500)
                if (_state.value.streaming) finalizeStream()
            }
        }
    }

    private fun finalizeStream() {
        idleFinalizeJob?.cancel()
        idleFinalizeJob = null
        streamJob?.cancel()
        streamJob = null
        var shouldNotifyPreview = false
        _state.update { s ->
            if (!s.streaming) return@update s
            val sealed = sealStreamingContent(s.streamingText, s.liveFiles)
            shouldNotifyPreview = sealed.isNotBlank() || s.liveFiles.isNotEmpty()
            val newMessages = if (sealed.isNotBlank()) {
                s.messages + ChatMessage(
                    id = tempId--,
                    content = sealed,
                    role = MessageRole.ASSISTANT,
                    tokensUsed = null,
                    createdAt = null
                )
            } else {
                s.messages
            }
            s.copy(
                messages = newMessages,
                streaming = false,
                streamingText = "",
                liveFiles = emptyList()
            )
        }
        if (shouldNotifyPreview) {
            repairBus.emit(PreviewRepairEvent.ChatBuildFinished)
        }
    }

    override fun onCleared() {
        idleFinalizeJob?.cancel()
        streamJob?.cancel()
        super.onCleared()
    }
}

/** Turn unfinished file payloads into [Wrote] markers; merge live FileReady paths. */
internal fun sealStreamingContent(raw: String, liveFiles: List<String>): String {
    // formatChatContent first so leftover <file> bodies become Writing, then promote to Wrote.
    val base = formatChatContent(raw)
        .replace(Regex("""\[Writing]\s+(.+?)(?:…|\.\.\.)?""", RegexOption.IGNORE_CASE), "[Wrote] $1")
        .ifBlank { "" }
    if (liveFiles.isEmpty()) return base
    val existing = parseChatTimeline(base, complete = true)
        .filterIsInstance<ChatTimelineItem.Action>()
        .map { it.detail }
        .toSet()
    val extras = liveFiles.filterNot { it in existing }.joinToString("\n") { "[Wrote] $it" }
    return listOf(base, extras).filter { it.isNotBlank() }.joinToString("\n")
}
