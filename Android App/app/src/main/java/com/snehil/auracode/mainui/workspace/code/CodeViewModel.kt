package com.snehil.auracode.mainui.workspace.code

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.navigation.Routes
import com.snehil.auracode.domain.model.FileNode
import com.snehil.auracode.domain.usecase.GetFileContentUseCase
import com.snehil.auracode.domain.usecase.GetFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CodeUiState(
    val loadingFiles: Boolean = true,
    val filesError: String? = null,
    val files: List<FileNode> = emptyList(),
    val selectedPath: String? = null,
    val contentLoading: Boolean = false,
    val contentError: String? = null,
    val content: String = ""
)

@HiltViewModel
class CodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFiles: GetFilesUseCase,
    private val getFileContent: GetFileContentUseCase
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>(Routes.ARG_PROJECT_ID) ?: 0L

    private val _state = MutableStateFlow(CodeUiState())
    val state: StateFlow<CodeUiState> = _state.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _state.update { it.copy(loadingFiles = true, filesError = null) }
            when (val res = getFiles(projectId)) {
                is Resource.Success -> {
                    _state.update { it.copy(loadingFiles = false, files = res.data) }
                    res.data.firstOrNull()?.let { selectFile(it.path) }
                }
                is Resource.Error -> _state.update { it.copy(loadingFiles = false, filesError = res.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun selectFile(path: String) {
        _state.update { it.copy(selectedPath = path, contentLoading = true, contentError = null, content = "") }
        viewModelScope.launch {
            when (val res = getFileContent(projectId, path)) {
                is Resource.Success -> _state.update { it.copy(contentLoading = false, content = res.data.content) }
                is Resource.Error -> _state.update { it.copy(contentLoading = false, contentError = res.message) }
                Resource.Loading -> Unit
            }
        }
    }
}
