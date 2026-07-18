package com.snehil.auracode.mainui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.model.ProjectSummary
import com.snehil.auracode.domain.model.UsageToday
import com.snehil.auracode.domain.repository.AuthRepository
import com.snehil.auracode.domain.usecase.CreateProjectUseCase
import com.snehil.auracode.domain.usecase.DeleteProjectUseCase
import com.snehil.auracode.domain.usecase.GetProjectsUseCase
import com.snehil.auracode.domain.usecase.GetUsageTodayUseCase
import com.snehil.auracode.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "",
    val userInitials: String = "A",
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val projects: List<ProjectSummary> = emptyList(),
    val usage: UsageToday? = null,
    val creating: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getProjects: GetProjectsUseCase,
    private val getUsageToday: GetUsageTodayUseCase,
    private val createProjectUseCase: CreateProjectUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _loggedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loggedOut: SharedFlow<Unit> = _loggedOut.asSharedFlow()

    init {
        observeUser()
        load()
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    _state.update { it.copy(userName = user.name, userInitials = user.initials) }
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            loadData()
            _state.update { it.copy(loading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            loadData()
            _state.update { it.copy(refreshing = false) }
        }
    }

    private suspend fun loadData() {
        when (val projectsRes = getProjects()) {
            is Resource.Success -> _state.update { it.copy(projects = projectsRes.data, error = null) }
            is Resource.Error -> _state.update { it.copy(error = projectsRes.message) }
            Resource.Loading -> Unit
        }
        (getUsageToday() as? Resource.Success)?.let { usage ->
            _state.update { it.copy(usage = usage.data) }
        }
    }

    fun createProject(name: String, description: String, onCreated: (Long) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(creating = true) }
            when (val res = createProjectUseCase(name, description)) {
                is Resource.Success -> {
                    _state.update { it.copy(creating = false) }
                    loadData()
                    onCreated(res.data.id)
                }
                is Resource.Error -> {
                    _state.update { it.copy(creating = false) }
                    _messages.tryEmit(res.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            when (val res = deleteProjectUseCase(id)) {
                is Resource.Success -> {
                    _state.update { s -> s.copy(projects = s.projects.filterNot { it.id == id }) }
                    _messages.tryEmit("Project deleted")
                }
                is Resource.Error -> _messages.tryEmit(res.message)
                Resource.Loading -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _loggedOut.tryEmit(Unit)
        }
    }
}
