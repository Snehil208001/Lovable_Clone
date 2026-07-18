package com.snehil.auracode.domain.model

data class User(
    val id: Long,
    val username: String,
    val name: String
) {
    val initials: String
        get() = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { username.take(1).uppercase() }
}

data class ProjectSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class Project(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val owner: User?
)

data class FileNode(
    val path: String
) {
    val displayName: String get() = path.substringAfterLast('/')
}

data class FileContent(
    val path: String,
    val content: String
)

enum class MessageRole { USER, ASSISTANT, SYSTEM, TOOL }

data class ChatMessage(
    val id: Long,
    val content: String,
    val role: MessageRole,
    val tokensUsed: Int?,
    val createdAt: String?
)

data class Plan(
    val id: Long,
    val name: String,
    val maxProjects: Int,
    val maxTokensPerDay: Int,
    val unlimitedAi: Boolean,
    val price: String?,
    val amountInr: Double?
)

data class Subscription(
    val plan: Plan?,
    val status: String,
    val currentPeriod: String?,
    val tokensUsedThisCycle: Int
)

data class UsageToday(
    val tokensUsed: Int,
    val tokensLimit: Int,
    val previewsRunning: Int,
    val previewsLimit: Int
)

data class PlanLimits(
    val planName: String,
    val maxTokensPerDay: Int,
    val maxProjects: Int,
    val unlimitedAi: Boolean
)

enum class PaymentProvider(val apiValue: String) {
    STRIPE("STRIPE"),
    CASHFREE("CASHFREE")
}

data class CheckoutSession(
    val checkoutUrl: String?,
    val paymentSessionId: String?,
    val provider: String?,
    val cashfreeEnv: String?
)
