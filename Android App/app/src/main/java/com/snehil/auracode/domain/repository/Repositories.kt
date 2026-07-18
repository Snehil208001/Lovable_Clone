package com.snehil.auracode.domain.repository

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.model.ChatMessage
import com.snehil.auracode.domain.model.CheckoutSession
import com.snehil.auracode.domain.model.FileContent
import com.snehil.auracode.domain.model.FileNode
import com.snehil.auracode.domain.model.Plan
import com.snehil.auracode.domain.model.PlanLimits
import com.snehil.auracode.domain.model.Project
import com.snehil.auracode.domain.model.ProjectSummary
import com.snehil.auracode.domain.model.StreamEvent
import com.snehil.auracode.domain.model.Subscription
import com.snehil.auracode.domain.model.UsageToday
import com.snehil.auracode.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val token: Flow<String?>
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun signup(name: String, email: String, password: String): Resource<User>
    suspend fun refreshMe(): Resource<User>
    suspend fun logout()
    fun hasToken(): Boolean
}

interface ProjectRepository {
    suspend fun getProjects(): Resource<List<ProjectSummary>>
    suspend fun getProject(id: Long): Resource<Project>
    suspend fun createProject(name: String, description: String?): Resource<Project>
    suspend fun deleteProject(id: Long): Resource<Unit>
}

interface FileRepository {
    suspend fun getFiles(projectId: Long): Resource<List<FileNode>>
    suspend fun getFileContent(projectId: Long, path: String): Resource<FileContent>
}

interface ChatRepository {
    suspend fun getMessages(projectId: Long): Resource<List<ChatMessage>>
    fun streamChat(projectId: Long, message: String): Flow<StreamEvent>
}

interface BillingRepository {
    suspend fun getPlans(): Resource<List<Plan>>
    suspend fun getSubscription(): Resource<Subscription>
    suspend fun getUsageToday(): Resource<UsageToday>
    suspend fun getUsageLimits(): Resource<PlanLimits>
    suspend fun createCheckout(planId: Long, provider: String): Resource<CheckoutSession>
    suspend fun openPortal(): Resource<String>
}
