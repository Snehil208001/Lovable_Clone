package com.snehil.auracode.domain.usecase

import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.model.CheckoutSession
import com.snehil.auracode.domain.model.Plan
import com.snehil.auracode.domain.model.Subscription
import com.snehil.auracode.domain.model.UsageToday
import com.snehil.auracode.domain.repository.BillingRepository
import javax.inject.Inject

class GetPlansUseCase @Inject constructor(private val repo: BillingRepository) {
    suspend operator fun invoke(): Resource<List<Plan>> = repo.getPlans()
}

class GetSubscriptionUseCase @Inject constructor(private val repo: BillingRepository) {
    suspend operator fun invoke(): Resource<Subscription> = repo.getSubscription()
}

class GetUsageTodayUseCase @Inject constructor(private val repo: BillingRepository) {
    suspend operator fun invoke(): Resource<UsageToday> = repo.getUsageToday()
}

class CreateCheckoutUseCase @Inject constructor(private val repo: BillingRepository) {
    suspend operator fun invoke(planId: Long, provider: String): Resource<CheckoutSession> =
        repo.createCheckout(planId, provider)
}

class OpenPortalUseCase @Inject constructor(private val repo: BillingRepository) {
    suspend operator fun invoke(): Resource<String> = repo.openPortal()
}
