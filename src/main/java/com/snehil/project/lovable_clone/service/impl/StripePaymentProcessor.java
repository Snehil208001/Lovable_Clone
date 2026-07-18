package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutRequest;
import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.PostalResponse;
import com.snehil.project.lovable_clone.entity.Plan;
import com.snehil.project.lovable_clone.entity.User;
import com.snehil.project.lovable_clone.enums.SubscriptionStatus;
import com.snehil.project.lovable_clone.error.BadRequestException;
import com.snehil.project.lovable_clone.error.ResourceNotFoundException;
import com.snehil.project.lovable_clone.repository.PlanRepository;
import com.snehil.project.lovable_clone.repository.UserRepository;
import com.snehil.project.lovable_clone.security.AuthUtil;
import com.snehil.project.lovable_clone.service.PaymentProcessor;
import com.snehil.project.lovable_clone.service.SubscriptionService;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentProcessor implements PaymentProcessor {


    private final AuthUtil authUtil;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    @Value("${client.url}")
    private String frontendUrl;

    @Override
    public CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request, Long userId) {
        Plan plan = planRepository.findById(request.planId()).orElseThrow(() ->
                new ResourceNotFoundException("Plan", request.planId().toString()));

        Long currentUserId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("user",userId.toString()));

        var params = SessionCreateParams.builder()
                .addLineItem(
                        SessionCreateParams.LineItem.builder().setPrice(plan.getStripePriceId()).setQuantity(1L).build())
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSubscriptionData(
                        new SessionCreateParams.SubscriptionData.Builder()
                                .setBillingMode(SessionCreateParams.SubscriptionData.BillingMode.builder()
                                        .setType(SessionCreateParams.SubscriptionData.BillingMode.Type.FLEXIBLE)
                                        .build())
                                .build()
                )
                .setSuccessUrl(frontendUrl + "/billing?status=success&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/billing?status=cancelled")
                .putMetadata("user_id",userId.toString())
                .putMetadata("plan_id",plan.getId().toString());
        try {
            String stripeCustomerId = user.getStripeCustomerId();

            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                params.setCustomerEmail(user.getUsername());
            } else {
                params.setCustomer(stripeCustomerId);
            }


            Session session = Session.create(params.build());
            return CheckoutResponse.stripe(session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe checkout failed for plan {} (price {})", plan.getId(), plan.getStripePriceId(), e);
            throw new BadRequestException("Payment provider rejected the checkout: " + stripeErrorMessage(e));
        }
    }

    @Override
    public PostalResponse openCustomerPortal(Long userId) {
        // FIX: Removed 'Long' to prevent variable redeclaration error.
        // It now correctly updates the method parameter with the authenticated user's ID.
        userId = authUtil.getCurrentUserId();
        User user = getUser(userId);

        try {
            String stripeCustomerId = ensureStripeCustomerId(user);
            var portalSession = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(frontendUrl + "/billing")
                            .build()
            );
            return new PostalResponse(portalSession.getUrl());
        } catch (StripeException e) {
            log.error("Stripe customer portal session failed for user {}", userId, e);
            throw new BadRequestException("Payment provider rejected the portal request: " + stripeErrorMessage(e));
        }

    }

    /**
     * Persists a Stripe Customer when missing (e.g. user never completed checkout so webhook never ran).
     */
    private String ensureStripeCustomerId(User user) throws StripeException {
        String existing = user.getStripeCustomerId();
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getUsername())
                .putMetadata("user_id", user.getId().toString())
                .build();
        Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);
        return customer.getId();
    }

    @Override
    public void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {
        // FIX: Replaced literal "type" with proper logging so you can actually see the event type dynamically
        log.debug("Handling webhook event of type: {}", type);

        switch (type) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted((Session) stripeObject,metadata);
            case "customer.subscription.updated" -> handleCustomerSubscriptionUpdated((Subscription) stripeObject);
            case "customer.subscription.deleted" -> handleCustomerSubscriptionDeleted((Subscription) stripeObject);
            case "invoice.paid" -> handleInvoicePaid((Invoice) stripeObject);
            case "invoice.payment_failed" -> handleInvoicePaymentFailed((Invoice) stripeObject);
            default -> log.debug("Ignoring the event: {}", type);
        }
    }
    private void handleCheckoutSessionCompleted(Session session,Map<String, String > metadata) {
        if (session == null) {
            log.error("session object was null");
            return;
        }

        Long userId = Long.parseLong(metadata.get("user_id"));
        Long planId = Long.parseLong(metadata.get("plan_id"));

        String subscriptionId = session.getSubscription();
        String customerId = session.getCustomer();

        User user = getUser(userId);
        if (user.getStripeCustomerId() == null) {
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        }

        subscriptionService.activateSubscription(userId, planId, subscriptionId, customerId, "STRIPE");
    }

    private void handleCustomerSubscriptionUpdated(Subscription subscription) {
        if (subscription == null) {
            log.error("subscription object was null inside handleCustomerSubscriptionUpdated");
            return;
        }

        SubscriptionStatus status = mapStripeStatusToEnum(subscription.getStatus());
        if (status == null) {
            log.warn("Unknown status '{}' for subscription {}", subscription.getStatus(),subscription.getId());
            return;
        }

        SubscriptionItem item = subscription.getItems().getData().get(0);
        Instant periodStart = toInstant(item.getCurrentPeriodStart());
        Instant periodEnd = toInstant(item.getCurrentPeriodStart());

        Long planId = resolvePlanId(item.getPrice());

        subscriptionService.updateSubscription(
                subscription.getId(),status,periodStart,periodEnd,
                subscription.getCancelAtPeriodEnd(),planId
        );

    }

    private void handleCustomerSubscriptionDeleted(Subscription subscription) {
        if (subscription == null) {
            log.error("subscription object was null inside handleCustomerSubscriptionDeleted");
            return;
        }

        subscriptionService.cancelSubscription(subscription.getId());
    }

    private void handleInvoicePaid(Invoice invoice) {
        String subId = extractSubscription(invoice);
        if (subId == null) return;

        try {
            Subscription subscription = Subscription.retrieve(subId);
            var item = subscription.getItems().getData().get(0);

            Instant periodStart = toInstant(item.getCurrentPeriodStart());
            Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

            subscriptionService.renewSubscriptionPeriod(
                    subId,
                    periodStart,
                    periodEnd
            );

        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleInvoicePaymentFailed(Invoice invoice) {
        String subId = extractSubscription(invoice);
        if (subId == null) return;

        subscriptionService.markSubscriptionPastDue(subId);


    }



    /// // Utility Methods

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("user", userId.toString()));
    }

    // getStripeError() is null for connection-level failures.
    private String stripeErrorMessage(StripeException e) {
        return e.getStripeError() != null ? e.getStripeError().getMessage() : e.getMessage();
    }
    private SubscriptionStatus mapStripeStatusToEnum(String status) {
        return switch (status) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRAILING;
            case "past_due", "unpaid", "paused", "incomplete_expired" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELLED;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            default -> {
                log.warn("Unmapped Stripe status: {}", status);
                yield null;
            }
        };
    }

    private Instant toInstant(Long epoch) {
        return epoch != null ? Instant.ofEpochSecond(epoch) : null;
    }

    private Long resolvePlanId(Price price) {
        if (price == null || price.getId() == null) return null;
        return planRepository.findByStripePriceId(price.getId())
                .map(Plan::getId)
                .orElse(null);
    }

    private String extractSubscription(Invoice invoice) {
        var parent = invoice.getParent();
        if (parent == null) return null;

        var subDetails = parent.getSubscriptionDetails();
        if (subDetails == null) return null;

        return subDetails.getSubscription();
    }
}