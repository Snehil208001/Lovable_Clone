package com.snehil.auracode.mainui.workspace.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.navigation.Routes
import com.snehil.auracode.data.network.PreviewRepairBus
import com.snehil.auracode.data.network.PreviewRepairEvent
import com.snehil.auracode.domain.model.StreamEvent
import com.snehil.auracode.domain.usecase.GetFileContentUseCase
import com.snehil.auracode.domain.usecase.GetFilesUseCase
import com.snehil.auracode.domain.usecase.StreamChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class PreviewUiState(
    val loading: Boolean = true,
    val status: String = "Preparing preview…",
    val error: String? = null,
    val payloadJson: String? = null,
    val fileCount: Int = 0,
    val payloadBytes: Int = 0,
    val projectId: Long = 0L,
    val renderKey: Int = 0,
    val autoFixing: Boolean = false,
    val autoFixStatus: String = "",
    val repairStuck: Boolean = false,
    /** True while refreshing under an existing preview (no blank flash). */
    val refreshing: Boolean = false
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFiles: GetFilesUseCase,
    private val getFileContent: GetFileContentUseCase,
    private val streamChat: StreamChatUseCase,
    private val repairBus: PreviewRepairBus
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L
    private val json = Json { encodeDefaults = true }

    private val _state = MutableStateFlow(PreviewUiState(projectId = projectId))
    val state: StateFlow<PreviewUiState> = _state.asStateFlow()

    private var fixJob: Job? = null
    private var loadJob: Job? = null
    private var autoFixAttempts = 0
    private var lastErrorKey: String? = null
    private var lastFullError: String? = null
    private var cachedPaths: List<String> = emptyList()

    init {
        PreviewLog.i("PreviewViewModel init projectId=$projectId")
        observeChatBuilds()
        softOrHardLoad(forceHard = true)
    }

    private fun observeChatBuilds() {
        viewModelScope.launch {
            repairBus.events.collect { event ->
                if (event is PreviewRepairEvent.ChatBuildFinished) {
                    PreviewLog.i("ChatBuildFinished → soft reload")
                    softReload()
                }
            }
        }
    }

    fun onAppear() {
        if (_state.value.autoFixing || _state.value.loading || _state.value.refreshing) return
        if (_state.value.payloadJson == null) softOrHardLoad(forceHard = true)
    }

    fun load() = softOrHardLoad(forceHard = true)

    fun softReload() {
        if (_state.value.autoFixing) return
        softOrHardLoad(forceHard = false)
    }

    private fun softOrHardLoad(forceHard: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val keepPayload = !forceHard && _state.value.payloadJson != null
            if (forceHard) {
                autoFixAttempts = 0
                lastErrorKey = null
            }
            PreviewLog.i("load soft=$keepPayload projectId=$projectId")
            _state.update {
                it.copy(
                    loading = !keepPayload,
                    refreshing = keepPayload,
                    error = null,
                    status = if (keepPayload) "Refreshing preview…" else "Fetching project files…",
                    payloadJson = if (keepPayload) it.payloadJson else null,
                    payloadBytes = if (keepPayload) it.payloadBytes else 0,
                    autoFixing = false,
                    autoFixStatus = "",
                    repairStuck = if (keepPayload) it.repairStuck else false
                )
            }

            if (projectId <= 0L) {
                _state.update { it.copy(loading = false, refreshing = false, error = "Missing project id.") }
                return@launch
            }

            when (val filesRes = getFiles(projectId)) {
                is Resource.Success -> {
                    cachedPaths = filesRes.data.map { it.path }
                    buildPreview(cachedPaths)
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = if (it.payloadJson == null) {
                                "Failed to load file tree: ${filesRes.message}"
                            } else null
                        )
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun retryAutoFix() {
        val err = lastFullError ?: return
        autoFixAttempts = 0
        lastErrorKey = null
        _state.update { it.copy(repairStuck = false, error = null) }
        onRuntimeError(err)
    }

    fun dismissRepairStuck() {
        _state.update { it.copy(repairStuck = false) }
    }

    fun onRuntimeError(raw: String) {
        val message = raw.trim()
        if (message.isBlank() || !looksLikeAppError(message)) return
        if (_state.value.autoFixing || fixJob?.isActive == true) return

        lastFullError = message
        if (autoFixAttempts >= MAX_AUTO_FIXES) {
            PreviewLog.w("auto-fix limit — soft banner")
            _state.update {
                it.copy(
                    autoFixing = false,
                    autoFixStatus = "",
                    repairStuck = true,
                    error = if (it.payloadJson == null) "Still broken after auto-fix. Tap Fix again." else null
                )
            }
            return
        }

        val key = message.take(160)
        if (key == lastErrorKey) return
        lastErrorKey = key

        PreviewLog.i("auto-fix trigger: ${message.take(200)}")
        autoFixAttempts++
        fixJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    autoFixing = true,
                    repairStuck = false,
                    error = null,
                    autoFixStatus = "AuraCode is fixing the preview…"
                )
            }
            repairBus.emit(PreviewRepairEvent.FixStarted(message.take(240)))

            // Refresh file list for suspect matching when empty.
            if (cachedPaths.isEmpty()) {
                (getFiles(projectId) as? Resource.Success)?.let { cachedPaths = it.data.map { n -> n.path } }
            }
            val prompt = PreviewErrorAnalyzer.buildFixPrompt(message, cachedPaths)
            var streamFailed = false
            try {
                streamChat(projectId, prompt).collect { event ->
                    when (event) {
                        is StreamEvent.Chunk -> Unit
                        is StreamEvent.FileReady -> {
                            _state.update {
                                it.copy(autoFixStatus = "Updating ${event.path.substringAfterLast('/')}…")
                            }
                        }
                        is StreamEvent.Failure -> {
                            streamFailed = true
                            PreviewLog.e("auto-fix stream failed: ${event.message}")
                            _state.update {
                                it.copy(
                                    autoFixing = false,
                                    autoFixStatus = "",
                                    repairStuck = true,
                                    error = if (it.payloadJson == null) event.message else null
                                )
                            }
                        }
                        StreamEvent.Completed -> Unit
                    }
                }
            } catch (t: Throwable) {
                PreviewLog.e("auto-fix exception", t)
                _state.update {
                    it.copy(
                        autoFixing = false,
                        autoFixStatus = "",
                        repairStuck = true,
                        error = if (it.payloadJson == null) t.message else null
                    )
                }
                return@launch
            }

            if (streamFailed) return@launch
            repairBus.emit(PreviewRepairEvent.FixFinished)
            _state.update {
                it.copy(
                    autoFixing = false,
                    refreshing = true,
                    autoFixStatus = "Reloading preview…"
                )
            }
            delay(250)
            softOrHardLoad(forceHard = false)
        }
    }

    private suspend fun buildPreview(allPaths: List<String>) {
        val leaves = SandpackBuilder.leafPaths(allPaths)
        PreviewLog.i("leafPaths=${leaves.size} (from ${allPaths.size})")
        if (leaves.isEmpty()) {
            _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    autoFixing = false,
                    error = "No files yet. Chat with the AI first to generate an app."
                )
            }
            return
        }

        _state.update {
            it.copy(
                status = "Downloading ${leaves.size} files…",
                fileCount = leaves.size,
                autoFixStatus = if (it.autoFixing) "Applying fix…" else it.autoFixStatus
            )
        }

        val fetched: List<Pair<String, String>> = coroutineScope {
            leaves.map { path ->
                async {
                    when (val res = getFileContent(projectId, path)) {
                        is Resource.Success -> path to res.data.content
                        else -> null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (fetched.isEmpty()) {
            _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    autoFixing = false,
                    error = if (it.payloadJson == null) {
                        "Preview could not load any file contents from the API."
                    } else null
                )
            }
            return
        }

        _state.update { it.copy(status = "Building Sandpack bundle (${fetched.size} files)…") }

        try {
            val payload = SandpackBuilder.build(fetched)
            val encoded = json.encodeToString(payload)
            lastErrorKey = null
            _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    error = null,
                    status = "Ready",
                    payloadJson = encoded,
                    payloadBytes = encoded.length,
                    fileCount = fetched.size,
                    renderKey = it.renderKey + 1,
                    autoFixing = false,
                    autoFixStatus = "",
                    repairStuck = false
                )
            }
        } catch (t: Throwable) {
            PreviewLog.e("buildSandpackFiles failed", t)
            _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    autoFixing = false,
                    error = if (it.payloadJson == null) {
                        "Failed to build preview: ${t.message ?: t::class.java.simpleName}"
                    } else null
                )
            }
        }
    }

    companion object {
        private const val MAX_AUTO_FIXES = 3

        fun looksLikeAppError(message: String): Boolean {
            val m = message.lowercase()
            if (m.contains("cdn.tailwindcss.com") || m.contains("favicon")) return false
            return m.contains("element type is invalid") ||
                m.contains("is not defined") ||
                m.contains("cannot read") ||
                m.contains("cannot find") ||
                m.contains("failed to compile") ||
                m.contains("module not found") ||
                m.contains("unexpected token") ||
                m.contains("typeerror") ||
                m.contains("referenceerror") ||
                m.contains("syntaxerror") ||
                (m.contains("export") && m.contains("undefined")) ||
                m.contains("check the render method") ||
                (m.contains("sandpack") && m.contains("error")) ||
                m.contains("window.onerror") ||
                m.contains("unhandledrejection")
        }
    }
}
