package com.snehil.auracode.data.repository

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.core.util.apiCall
import com.snehil.auracode.data.remote.AuraCodeApi
import com.snehil.auracode.data.remote.dto.CheckoutRequest
import com.snehil.auracode.data.remote.dto.toDomain
import com.snehil.auracode.domain.model.CheckoutSession
import com.snehil.auracode.domain.model.Plan
import com.snehil.auracode.domain.model.PlanLimits
import com.snehil.auracode.domain.model.Subscription
import com.snehil.auracode.domain.model.UsageToday
import com.snehil.auracode.domain.repository.BillingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepositoryImpl @Inject constructor(
    private val api: AuraCodeApi
) : BillingRepository {

    override suspend fun getPlans(): Resource<List<Plan>> =
        when (val res = apiCall { api.getPlans() }) {
            is Resource.Success -> Resource.Success(res.data.map { it.toDomain() })
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun getSubscription(): Resource<Subscription> =
        when (val res = apiCall { api.getSubscription() }) {
            is Resource.Success -> Resource.Success(res.data.toDomain())
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun getUsageToday(): Resource<UsageToday> =
        when (val res = apiCall { api.getUsageToday() }) {
            is Resource.Success -> Resource.Success(res.data.toDomain())
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun getUsageLimits(): Resource<PlanLimits> =
        when (val res = apiCall { api.getUsageLimits() }) {
            is Resource.Success -> Resource.Success(res.data.toDomain())
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun createCheckout(planId: Long, provider: String): Resource<CheckoutSession> =
        when (val res = apiCall {
            val body = CheckoutRequest(planId = planId, provider = provider)
            if (provider.equals("CASHFREE", ignoreCase = true)) {
                api.createCashfreeCheckout(body)
            } else {
                api.createCheckout(provider = provider, body = body)
            }
        }) {
            is Resource.Success -> Resource.Success(res.data.toDomain())
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }

    override suspend fun openPortal(): Resource<String> =
        when (val res = apiCall { api.openPortal() }) {
            is Resource.Success -> Resource.Success(res.data.portalUrl)
            is Resource.Error -> res
            Resource.Loading -> Resource.Loading
        }
}
