package com.snehil.auracode.data.repository

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.util.apiCall
import com.snehil.auracode.data.remote.AuraCodeApi
import com.snehil.auracode.data.remote.dto.toDomain
import com.snehil.auracode.domain.model.FileContent
import com.snehil.auracode.domain.model.FileNode
import com.snehil.auracode.domain.repository.FileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val api: AuraCodeApi
) : FileRepository {

    override suspend fun getFiles(projectId: Long): Resource<List<FileNode>> =
        when (val res = apiCall { api.getFiles(projectId) }) {
            is Resource.Success -> Resource.Success(res.data.map { it.toDomain() }.sortedBy { it.path })
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun getFileContent(projectId: Long, path: String): Resource<FileContent> =
        when (val res = apiCall { api.getFileContent(projectId, path) }) {
            is Resource.Success -> Resource.Success(res.data.toDomain())
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }
}
