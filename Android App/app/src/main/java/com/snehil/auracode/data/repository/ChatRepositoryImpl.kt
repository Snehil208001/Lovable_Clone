package com.snehil.auracode.data.repository

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.util.apiCall
import com.snehil.auracode.data.network.ChatStreamClient
import com.snehil.auracode.data.remote.AuraCodeApi
import com.snehil.auracode.data.remote.dto.toDomain
import com.snehil.auracode.domain.model.ChatMessage
import com.snehil.auracode.domain.model.StreamEvent
import com.snehil.auracode.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: AuraCodeApi,
    private val streamClient: ChatStreamClient
) : ChatRepository {

    override suspend fun getMessages(projectId: Long): Resource<List<ChatMessage>> =
        when (val res = apiCall { api.getMessages(projectId) }) {
            is Resource.Success -> Resource.Success(res.data.map { it.toDomain() })
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override fun streamChat(projectId: Long, message: String): Flow<StreamEvent> =
        streamClient.stream(projectId, message)
}
