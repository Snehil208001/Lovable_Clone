package com.snehil.auracode.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class UserProfileDto(
    val id: Long = 0,
    val username: String = "",
    val name: String = ""
)

@Serializable
data class AuthResponse(
    val token: String = "",
    val userProfileResponse: UserProfileDto = UserProfileDto()
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val username: String,
    val name: String,
    val password: String
)

@Serializable
data class ProjectRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class ProjectSummaryDto(
    val id: Long = 0,
    val projectName: String = "",
    val description: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class ProjectDto(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val owner: UserProfileDto? = null
)

@Serializable
data class FileNodeDto(
    val path: String = ""
)

@Serializable
data class FileContentDto(
    val path: String = "",
    val content: String = ""
)

@Serializable
data class ChatMessageDto(
    val id: Long = 0,
    val content: String = "",
    val role: String = "ASSISTANT",
    val tokensUsed: Int? = null,
    val createdAt: String? = null
)

@Serializable
data class ChatRequest(
    val message: String,
    val projectId: Long
)

@Serializable
data class PlanDto(
    val id: Long = 0,
    val name: String = "",
    val maxProjects: Int = 0,
    val maxTokensPerDay: Int = 0,
    val unlimitedAi: Boolean = false,
    val price: String? = null,
    val amountInr: Double? = null
)

@Serializable
data class SubscriptionDto(
    val plan: PlanDto? = null,
    val status: String? = null,
    val currentPeriod: String? = null,
    val tokensUsedThisCycle: Int = 0
)

@Serializable
data class UsageTodayDto(
    val tokensUsed: Int = 0,
    val tokensLimit: Int = 0,
    val previewsRunning: Int = 0,
    val previewsLimit: Int = 0
)

@Serializable
data class PlanLimitsDto(
    // Backend field is misspelled "planeName".
    @SerialName("planeName") val planName: String = "",
    val maxTokensPerDay: Int = 0,
    val maxProjects: Int = 0,
    val unlimitedAi: Boolean = false
)

@Serializable
data class CheckoutRequest(
    val planId: Long,
    val provider: String,
    val customerPhone: String? = null
)

@Serializable
data class CheckoutResponse(
    @JsonNames("checkout_url")
    val checkoutUrl: String? = null,
    @JsonNames("payment_session_id")
    val paymentSessionId: String? = null,
    val provider: String? = null,
    @JsonNames("cashfree_env")
    val cashfreeEnv: String? = null
)

@Serializable
data class PortalResponse(
    val portalUrl: String = ""
)

@Serializable
data class FileReadyPayload(
    val path: String = ""
)

@Serializable
data class ApiErrorDto(
    val status: String? = null,
    val message: String? = null,
    val timestamp: String? = null
)
