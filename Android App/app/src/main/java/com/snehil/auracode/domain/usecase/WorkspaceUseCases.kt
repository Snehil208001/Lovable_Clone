package com.snehil.auracode.domain.usecase

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.model.ChatMessage
import com.snehil.auracode.domain.model.FileContent
import com.snehil.auracode.domain.model.FileNode
import com.snehil.auracode.domain.model.StreamEvent
import com.snehil.auracode.domain.repository.ChatRepository
import com.snehil.auracode.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFilesUseCase @Inject constructor(private val repo: FileRepository) {
    suspend operator fun invoke(projectId: Long): Resource<List<FileNode>> = repo.getFiles(projectId)
}

class GetFileContentUseCase @Inject constructor(private val repo: FileRepository) {
    suspend operator fun invoke(projectId: Long, path: String): Resource<FileContent> =
        repo.getFileContent(projectId, path)
}

class GetMessagesUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(projectId: Long): Resource<List<ChatMessage>> =
        repo.getMessages(projectId)
}

class StreamChatUseCase @Inject constructor(private val repo: ChatRepository) {
    operator fun invoke(projectId: Long, message: String): Flow<StreamEvent> =
        repo.streamChat(projectId, message)
}
