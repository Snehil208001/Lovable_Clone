package com.snehil.auracode.data.remote.dto

import com.snehil.auracode.domain.model.ChatMessage
import com.snehil.auracode.domain.model.CheckoutSession
import com.snehil.auracode.domain.model.FileContent
import com.snehil.auracode.domain.model.FileNode
import com.snehil.auracode.domain.model.MessageRole
import com.snehil.auracode.domain.model.Plan
import com.snehil.auracode.domain.model.PlanLimits
import com.snehil.auracode.domain.model.Project
import com.snehil.auracode.domain.model.ProjectSummary
import com.snehil.auracode.domain.model.Subscription
import com.snehil.auracode.domain.model.UsageToday
import com.snehil.auracode.domain.model.User

fun UserProfileDto.toDomain() = User(id = id, username = username, name = name)

fun ProjectSummaryDto.toDomain() = ProjectSummary(
    id = id,
    name = projectName,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProjectDto.toDomain() = Project(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt,
    owner = owner?.toDomain()
)

fun FileNodeDto.toDomain() = FileNode(path = path)

fun FileContentDto.toDomain() = FileContent(path = path, content = content)

fun ChatMessageDto.toDomain() = ChatMessage(
    id = id,
    content = content,
    role = runCatching { MessageRole.valueOf(role.uppercase()) }.getOrDefault(MessageRole.ASSISTANT),
    tokensUsed = tokensUsed,
    createdAt = createdAt
)

fun PlanDto.toDomain() = Plan(
    id = id,
    name = name,
    maxProjects = maxProjects,
    maxTokensPerDay = maxTokensPerDay,
    unlimitedAi = unlimitedAi,
    price = price,
    amountInr = amountInr
)

fun SubscriptionDto.toDomain() = Subscription(
    plan = plan?.toDomain(),
    status = status ?: "UNKNOWN",
    currentPeriod = currentPeriod,
    tokensUsedThisCycle = tokensUsedThisCycle
)

fun UsageTodayDto.toDomain() = UsageToday(
    tokensUsed = tokensUsed,
    tokensLimit = tokensLimit,
    previewsRunning = previewsRunning,
    previewsLimit = previewsLimit
)

fun PlanLimitsDto.toDomain() = PlanLimits(
    planName = planName,
    maxTokensPerDay = maxTokensPerDay,
    maxProjects = maxProjects,
    unlimitedAi = unlimitedAi
)

fun CheckoutResponse.toDomain() = CheckoutSession(
    checkoutUrl = checkoutUrl,
    paymentSessionId = paymentSessionId,
    provider = provider,
    cashfreeEnv = cashfreeEnv
)
