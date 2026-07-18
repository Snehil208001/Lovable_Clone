package com.snehil.auracode.data.repository

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.util.apiCall
import com.snehil.auracode.data.remote.AuraCodeApi
import com.snehil.auracode.data.remote.dto.ProjectRequest
import com.snehil.auracode.data.remote.dto.toDomain
import com.snehil.auracode.domain.model.Project
import com.snehil.auracode.domain.model.ProjectSummary
import com.snehil.auracode.domain.repository.ProjectRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val api: AuraCodeApi
) : ProjectRepository {

    override suspend fun getProjects(): Resource<List<ProjectSummary>> =
        when (val res = apiCall { api.getProjects() }) {
            is Resource.Success -> Resource.Success(res.data.map { it.toDomain() })
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun getProject(id: Long): Resource<Project> =
        when (val res = apiCall { api.getProject(id) }) {
            is Resource.Success -> Resource.Success(res.data.toDomain())
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun createProject(name: String, description: String?): Resource<Project> =
        when (val res = apiCall {
            api.createProject(ProjectRequest(name = name, description = description))
        }) {
            is Resource.Success -> Resource.Success(res.data.toDomain())
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun deleteProject(id: Long): Resource<Unit> =
        when (val res = apiCall { api.deleteProject(id) }) {
            is Resource.Success ->
                if (res.data.isSuccessful) Resource.Success(Unit)
                else Resource.Error("Failed to delete project (${res.data.code()}).")
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }
}
