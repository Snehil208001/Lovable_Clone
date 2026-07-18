package com.snehil.auracode.mainui.workspace.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.navigation.Routes
import com.snehil.auracode.domain.usecase.GetFileContentUseCase
import com.snehil.auracode.domain.usecase.GetFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    /** Bumped on every successful build so the WebView re-renders the bundle. */
    val renderKey: Int = 0
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFiles: GetFilesUseCase,
    private val getFileContent: GetFileContentUseCase
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L

    private val json = Json { encodeDefaults = true }

    private val _state = MutableStateFlow(PreviewUiState(projectId = projectId))
    val state: StateFlow<PreviewUiState> = _state.asStateFlow()

    init {
        PreviewLog.i("PreviewViewModel init projectId=$projectId")
        load()
    }

    fun load() {
        viewModelScope.launch {
            PreviewLog.clear()
            PreviewLog.i("load() start projectId=$projectId")
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    status = "Fetching project files…",
                    payloadJson = null,
                    payloadBytes = 0,
                    projectId = projectId
                )
            }

            if (projectId <= 0L) {
                PreviewLog.e("Missing project id from SavedStateHandle")
                _state.update { it.copy(loading = false, error = "Missing project id.") }
                return@launch
            }

            when (val filesRes = getFiles(projectId)) {
                is Resource.Success -> {
                    PreviewLog.i("file tree OK: ${filesRes.data.size} nodes")
                    filesRes.data.take(20).forEach { PreviewLog.i("  path=${it.path}") }
                    if (filesRes.data.size > 20) PreviewLog.i("  … +${filesRes.data.size - 20} more")
                    buildPreview(filesRes.data.map { it.path })
                }
                is Resource.Error -> {
                    PreviewLog.e("file tree failed: ${filesRes.message}")
                    _state.update {
                        it.copy(loading = false, error = "Failed to load file tree: ${filesRes.message}")
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }

    private suspend fun buildPreview(allPaths: List<String>) {
        val leaves = SandpackBuilder.leafPaths(allPaths)
        PreviewLog.i("leafPaths=${leaves.size} (from ${allPaths.size})")
        if (leaves.isEmpty()) {
            PreviewLog.w("No leaf files — cannot preview")
            _state.update {
                it.copy(
                    loading = false,
                    error = "No files available yet to render a preview. Chat with the AI first to generate an app."
                )
            }
            return
        }

        _state.update { it.copy(status = "Downloading ${leaves.size} files…", fileCount = leaves.size) }

        val fetched: List<Pair<String, String>> = coroutineScope {
            leaves.map { path ->
                async {
                    when (val res = getFileContent(projectId, path)) {
                        is Resource.Success -> {
                            PreviewLog.i("file OK $path (${res.data.content.length} chars)")
                            path to res.data.content
                        }
                        is Resource.Error -> {
                            PreviewLog.w("file FAIL $path: ${res.message}")
                            null
                        }
                        Resource.Loading -> null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        PreviewLog.i("downloaded ${fetched.size}/${leaves.size} files")
        if (fetched.isEmpty()) {
            PreviewLog.e("Zero file contents downloaded")
            _state.update {
                it.copy(loading = false, error = "Preview could not load any file contents from the API.")
            }
            return
        }

        _state.update { it.copy(status = "Building Sandpack bundle (${fetched.size} files)…") }

        try {
            val payload = SandpackBuilder.build(fetched)
            val encoded = json.encodeToString(payload)
            PreviewLog.i(
                "payload ready files=${payload.files.size} entry=${payload.entry} " +
                    "active=${payload.activeFile} bytes=${encoded.length}"
            )
            PreviewLog.i("deps=${payload.dependencies.keys.joinToString()}")
            payload.files.keys.take(30).forEach { PreviewLog.i("  sandpack file=$it") }
            _state.update {
                it.copy(
                    loading = false,
                    error = null,
                    status = "Starting WebView…",
                    payloadJson = encoded,
                    payloadBytes = encoded.length,
                    fileCount = fetched.size,
                    renderKey = it.renderKey + 1
                )
            }
        } catch (t: Throwable) {
            PreviewLog.e("buildSandpackFiles failed", t)
            _state.update {
                it.copy(
                    loading = false,
                    error = "Failed to build preview payload: ${t.message ?: t::class.java.simpleName}"
                )
            }
        }
    }
}
