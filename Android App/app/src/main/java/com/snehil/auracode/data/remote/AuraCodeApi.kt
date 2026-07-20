package com.snehil.auracode.data.remote

import com.snehil.auracode.data.remote.dto.AuthResponse
import com.snehil.auracode.data.remote.dto.ChatMessageDto
import com.snehil.auracode.data.remote.dto.CheckoutRequest
import com.snehil.auracode.data.remote.dto.CheckoutResponse
import com.snehil.auracode.data.remote.dto.FileContentDto
import com.snehil.auracode.data.remote.dto.FileNodeDto
import com.snehil.auracode.data.remote.dto.LoginRequest
import com.snehil.auracode.data.remote.dto.PlanDto
import com.snehil.auracode.data.remote.dto.PlanLimitsDto
import com.snehil.auracode.data.remote.dto.PortalResponse
import com.snehil.auracode.data.remote.dto.ProjectDto
import com.snehil.auracode.data.remote.dto.ProjectRequest
import com.snehil.auracode.data.remote.dto.ProjectSummaryDto
import com.snehil.auracode.data.remote.dto.SignupRequest
import com.snehil.auracode.data.remote.dto.SubscriptionDto
import com.snehil.auracode.data.remote.dto.UsageTodayDto
import com.snehil.auracode.data.remote.dto.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuraCodeApi {

    @POST("api/auth/signup")
    suspend fun signup(@Body body: SignupRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("api/auth/me")
    suspend fun me(): UserProfileDto

    @GET("api/projects")
    suspend fun getProjects(): List<ProjectSummaryDto>

    @GET("api/projects/{id}")
    suspend fun getProject(@Path("id") id: Long): ProjectDto

    @POST("api/projects")
    suspend fun createProject(@Body body: ProjectRequest): ProjectDto

    @PATCH("api/projects/{id}")
    suspend fun updateProject(@Path("id") id: Long, @Body body: ProjectRequest): ProjectDto

    @DELETE("api/projects/{id}")
    suspend fun deleteProject(@Path("id") id: Long): Response<Unit>

    @GET("api/projects/{id}/files")
    suspend fun getFiles(@Path("id") id: Long): List<FileNodeDto>

    // Backend uses a catch-all path; keep slashes unencoded.
    @GET("api/projects/{id}/files/{path}")
    suspend fun getFileContent(
        @Path("id") id: Long,
        @Path(value = "path", encoded = true) path: String
    ): FileContentDto

    @GET("api/projects/{id}/messages")
    suspend fun getMessages(@Path("id") id: Long): List<ChatMessageDto>

    @GET("api/plans")
    suspend fun getPlans(): List<PlanDto>

    @GET("api/me/subscription")
    suspend fun getSubscription(): SubscriptionDto

    @GET("api/usage/today")
    suspend fun getUsageToday(): UsageTodayDto

    @GET("api/usage/limits")
    suspend fun getUsageLimits(): PlanLimitsDto

    @POST("api/payments/checkout")
    suspend fun createCheckout(
        @Query("provider") provider: String,
        @Body body: CheckoutRequest
    ): CheckoutResponse

    /** Dedicated Cashfree path — avoids Stripe fallback when provider routing fails on the server. */
    @POST("api/payments/cashfree/checkout")
    suspend fun createCashfreeCheckout(
        @Body body: CheckoutRequest
    ): CheckoutResponse

    @POST("api/payments/portal")
    suspend fun openPortal(): PortalResponse
}
