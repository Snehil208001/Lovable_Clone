package com.snehil.auracode.domain.usecase

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.model.Project
import com.snehil.auracode.domain.model.ProjectSummary
import com.snehil.auracode.domain.repository.ProjectRepository
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(private val repo: ProjectRepository) {
    suspend operator fun invoke(): Resource<List<ProjectSummary>> = repo.getProjects()
}

class GetProjectUseCase @Inject constructor(private val repo: ProjectRepository) {
    suspend operator fun invoke(id: Long): Resource<Project> = repo.getProject(id)
}

class CreateProjectUseCase @Inject constructor(private val repo: ProjectRepository) {
    suspend operator fun invoke(name: String, description: String?): Resource<Project> =
        repo.createProject(name.trim(), description?.trim()?.ifBlank { null })
}

class DeleteProjectUseCase @Inject constructor(private val repo: ProjectRepository) {
    suspend operator fun invoke(id: Long): Resource<Unit> = repo.deleteProject(id)
}
