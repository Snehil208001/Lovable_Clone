package com.snehil.auracode.mainui.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snehil.auracode.domain.model.PaymentProvider
import com.snehil.auracode.domain.model.Plan
import com.snehil.auracode.domain.model.Subscription
import com.snehil.auracode.domain.model.UsageToday
import com.snehil.auracode.ui.components.AuraBackground
import com.snehil.auracode.ui.components.ErrorState
import com.snehil.auracode.ui.components.GlassCard
import com.snehil.auracode.ui.components.LoadingState
import com.snehil.auracode.ui.components.PrimaryButton
import com.snehil.auracode.ui.theme.BorderColor
import com.snehil.auracode.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    onBack: () -> Unit,
    viewModel: BillingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.banner) {
        state.banner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissBanner()
        }
    }
    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissActionError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Billing", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        AuraBackground {
            when {
                state.loading -> LoadingState(Modifier.padding(padding))
                state.error != null && state.plans.isEmpty() ->
                    ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.subscription?.let {
                        CurrentSubscriptionCard(
                            subscription = it,
                            portalLoading = state.checkoutLoadingKey == BillingViewModel.PORTAL_KEY,
                            onManage = viewModel::openPortal
                        )
                    }
                    state.usage?.let { UsageCard(it) }

                    Text(
                        text = "Available plans",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    state.plans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            isCurrent = plan.id == state.subscription?.plan?.id,
                            checkoutLoadingKey = state.checkoutLoadingKey,
                            onCheckout = { provider -> viewModel.checkout(plan.id, provider) }
                        )
                    }

                    Text(
                        text = "Payments are processed securely by Stripe and Cashfree. Your plan updates automatically once payment completes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    state.checkoutTarget?.let { target ->
        CheckoutWebViewDialog(
            target = target,
            onResult = viewModel::onCheckoutResult,
            onDismiss = viewModel::dismissCheckout
        )
    }
}

@Composable
private fun CurrentSubscriptionCard(
    subscription: Subscription,
    portalLoading: Boolean,
    onManage: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CURRENT PLAN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subscription.plan?.name ?: "Free",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(subscription.status)
            }
            subscription.currentPeriod?.let {
                Text(
                    text = "Current period: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onManage,
                enabled = !portalLoading,
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                if (portalLoading) {
                    CircularProgressIndicator(
                        color = Primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Manage subscription", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = if (status.equals("ACTIVE", true) || status.equals("TRAILING", true)) Primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.height(16.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun UsageCard(usage: UsageToday) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "TODAY'S USAGE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            UsageBar("Tokens", usage.tokensUsed, usage.tokensLimit)
            UsageBar("Previews", usage.previewsRunning, usage.previewsLimit)
        }
    }
}

@Composable
private fun UsageBar(label: String, used: Int, total: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$used / $total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { if (total <= 0) 0f else (used.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Primary,
            trackColor = BorderColor
        )
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    isCurrent: Boolean,
    checkoutLoadingKey: String?,
    onCheckout: (PaymentProvider) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = when {
                        plan.amountInr != null && plan.amountInr > 0 -> "₹${plan.amountInr.toInt()}"
                        !plan.price.isNullOrBlank() -> plan.price
                        else -> "—"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
            if (isCurrent) {
                Text(
                    text = "Your current plan",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            PlanFeature("${plan.maxProjects} projects")
            PlanFeature(if (plan.unlimitedAi) "Unlimited AI tokens" else "${plan.maxTokensPerDay} tokens / day")

            val isFree = (plan.amountInr ?: 0.0) <= 0.0 && plan.price.isNullOrBlank()
            val cashfreeEnabled = (plan.amountInr ?: 0.0) > 0.0
            if (!isCurrent && !isFree) {
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    text = "Pay with card",
                    onClick = { onCheckout(PaymentProvider.STRIPE) },
                    loading = checkoutLoadingKey == "${PaymentProvider.STRIPE.apiValue}-${plan.id}",
                    enabled = checkoutLoadingKey == null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (cashfreeEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onCheckout(PaymentProvider.CASHFREE) },
                        enabled = checkoutLoadingKey == null,
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        if (checkoutLoadingKey == "${PaymentProvider.CASHFREE.apiValue}-${plan.id}") {
                            CircularProgressIndicator(
                                color = Primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text("Pay via UPI / Cashfree", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanFeature(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
