package com.snehil.auracode.mainui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.auracode.core.common.Resource
import com.snehil.auracode.domain.model.Plan
import com.snehil.auracode.domain.model.PaymentProvider
import com.snehil.auracode.domain.model.Subscription
import com.snehil.auracode.domain.model.UsageToday
import com.snehil.auracode.domain.usecase.CreateCheckoutUseCase
import com.snehil.auracode.domain.usecase.GetPlansUseCase
import com.snehil.auracode.domain.usecase.GetSubscriptionUseCase
import com.snehil.auracode.domain.usecase.GetUsageTodayUseCase
import com.snehil.auracode.domain.usecase.OpenPortalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the checkout WebView overlay should load. */
sealed interface CheckoutTarget {
    /** Stripe checkout URL or Stripe/Cashfree customer-portal URL. */
    data class Url(val url: String) : CheckoutTarget

    /** Cashfree needs the JS SDK launched with a payment session id. */
    data class Cashfree(val sessionId: String, val sandbox: Boolean) : CheckoutTarget
}

data class BillingUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val subscription: Subscription? = null,
    val usage: UsageToday? = null,
    val plans: List<Plan> = emptyList(),
    val checkoutLoadingKey: String? = null,
    val checkoutTarget: CheckoutTarget? = null,
    val banner: String? = null,
    val actionError: String? = null
)

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val getSubscription: GetSubscriptionUseCase,
    private val getUsageToday: GetUsageTodayUseCase,
    private val getPlans: GetPlansUseCase,
    private val createCheckout: CreateCheckoutUseCase,
    private val openPortalUseCase: OpenPortalUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            val plansRes = getPlans()
            val subRes = getSubscription()
            val usageRes = getUsageToday()

            val error = (plansRes as? Resource.Error)?.message
                ?: (subRes as? Resource.Error)?.message

            _state.update {
                it.copy(
                    loading = false,
                    plans = (plansRes as? Resource.Success)?.data ?: it.plans,
                    subscription = (subRes as? Resource.Success)?.data ?: it.subscription,
                    usage = (usageRes as? Resource.Success)?.data ?: it.usage,
                    error = if ((plansRes is Resource.Success) || (subRes is Resource.Success)) null else error
                )
            }
        }
    }

    fun checkout(planId: Long, provider: PaymentProvider) {
        val key = "${provider.apiValue}-$planId"
        viewModelScope.launch {
            _state.update { it.copy(checkoutLoadingKey = key, actionError = null) }
            when (val res = createCheckout(planId, provider.apiValue)) {
                is Resource.Success -> {
                    val session = res.data
                    // Route by the provider the user tapped — never prefer a Stripe URL
                    // when Cashfree was requested (mirrors web billing page).
                    val target: CheckoutTarget? = when (provider) {
                        PaymentProvider.CASHFREE -> {
                            val sessionId = session.paymentSessionId
                            if (!sessionId.isNullOrBlank()) {
                                CheckoutTarget.Cashfree(
                                    sessionId = sessionId,
                                    sandbox = session.cashfreeEnv.equals("sandbox", ignoreCase = true)
                                )
                            } else {
                                null
                            }
                        }
                        PaymentProvider.STRIPE -> {
                            session.checkoutUrl?.takeIf { it.isNotBlank() }?.let { CheckoutTarget.Url(it) }
                        }
                    }
                    val error = when {
                        target != null -> null
                        provider == PaymentProvider.CASHFREE && !session.checkoutUrl.isNullOrBlank() ->
                            "Server returned Stripe checkout instead of Cashfree. Restart the backend and try again."
                        provider == PaymentProvider.CASHFREE ->
                            "Cashfree session missing. Set CASHFREE_APP_ID, CASHFREE_SECRET_KEY, and plan amount_inr."
                        else ->
                            "Stripe did not return a checkout URL."
                    }
                    _state.update {
                        it.copy(
                            checkoutLoadingKey = null,
                            checkoutTarget = target,
                            actionError = error
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(checkoutLoadingKey = null, actionError = res.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun openPortal() {
        viewModelScope.launch {
            _state.update { it.copy(checkoutLoadingKey = PORTAL_KEY, actionError = null) }
            when (val res = openPortalUseCase()) {
                is Resource.Success -> _state.update {
                    it.copy(checkoutLoadingKey = null, checkoutTarget = CheckoutTarget.Url(res.data))
                }
                is Resource.Error -> _state.update {
                    it.copy(checkoutLoadingKey = null, actionError = res.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    /** Called when the checkout WebView is dismissed without a detected status. */
    fun dismissCheckout() {
        _state.update { it.copy(checkoutTarget = null) }
    }

    /** Called when the WebView redirects back to /billing?status=... */
    fun onCheckoutResult(status: String?) {
        _state.update {
            it.copy(
                checkoutTarget = null,
                banner = when (status) {
                    "success" -> "Payment completed successfully! Your plan will update shortly."
                    "cancelled" -> "Checkout cancelled."
                    else -> null
                }
            )
        }
        if (status == "success") load()
    }

    fun dismissBanner() = _state.update { it.copy(banner = null) }

    fun dismissActionError() = _state.update { it.copy(actionError = null) }

    companion object {
        const val PORTAL_KEY = "portal"
    }
}
