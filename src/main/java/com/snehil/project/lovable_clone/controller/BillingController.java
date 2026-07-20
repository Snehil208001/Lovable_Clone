package com.snehil.project.lovable_clone.controller;

import com.snehil.project.lovable_clone.dto.subscriptions.*;
import com.snehil.project.lovable_clone.security.JwtUserPrincipal;
import com.snehil.project.lovable_clone.service.PaymentProcessor;
import com.snehil.project.lovable_clone.service.PlanService;
import com.snehil.project.lovable_clone.service.SubscriptionService;
import com.snehil.project.lovable_clone.service.impl.CashfreePaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private final PaymentProcessor paymentProcessor;
    private final CashfreePaymentService cashfreePaymentService;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @GetMapping("/api/plans")
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllActivePlans());
    }

    @GetMapping("/api/me/subscription")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription());
    }

    /**
     * Cashfree-only checkout. Prefer this from Android/web for UPI — avoids provider
     * routing bugs where a Stripe session was returned for CASHFREE requests.
     */
    @PostMapping("/api/payments/cashfree/checkout")
    public ResponseEntity<CheckoutResponse> createCashfreeCheckout(
            @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser
    ) {
        log.info("Cashfree checkout planId={} userId={}", request.planId(), currentUser.userId());
        return ResponseEntity.ok(cashfreePaymentService.createCheckoutSession(request, currentUser.userId()));
    }

    @PostMapping("/api/payments/checkout")
    public ResponseEntity<CheckoutResponse> createCheckoutResponse(
            @RequestBody CheckoutRequest request,
            @RequestParam(value = "provider", required = false) String providerParam,
            @AuthenticationPrincipal JwtUserPrincipal currentUser
    ) {
        String fromBody = request.provider();
        String fromQuery = providerParam;
        // Prefer an explicit CASHFREE from either source — never let a missing/defaulted
        // body field hide a Cashfree query param (Android sends both).
        String provider;
        if ("CASHFREE".equalsIgnoreCase(safe(fromBody)) || "CASHFREE".equalsIgnoreCase(safe(fromQuery))) {
            provider = "CASHFREE";
        } else if (StringUtils.hasText(fromBody)) {
            provider = fromBody.trim().toUpperCase();
        } else if (StringUtils.hasText(fromQuery)) {
            provider = fromQuery.trim().toUpperCase();
        } else {
            provider = "STRIPE";
        }
        log.info("Checkout requested provider={} planId={} userId={} (body={}, query={})",
                provider, request.planId(), currentUser.userId(), fromBody, fromQuery);
        if ("CASHFREE".equals(provider)) {
            return ResponseEntity.ok(cashfreePaymentService.createCheckoutSession(request, currentUser.userId()));
        }
        return ResponseEntity.ok(paymentProcessor.createCheckoutSessionUrl(request, currentUser.userId()));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @PostMapping("/api/payments/portal")
    public ResponseEntity<PostalResponse> openCustomerPortal(
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(paymentProcessor.openCustomerPortal(currentUser.userId()));
    }

    @PostMapping("/webhooks/payment")
    public ResponseEntity<String> handlePaymentWebHooks(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            // trim guards against stray whitespace in the env var, which silently breaks the HMAC
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret.trim());

            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = null;

            if (deserializer.getObject().isPresent()) { // happy case
                stripeObject = deserializer.getObject().get();
            } else {
                // Fallback: Deserialize from raw JSON
                try {
                    stripeObject = deserializer.deserializeUnsafe();
                    if (stripeObject == null) {
                        log.warn("Failed to deserialize webhook object for event: {}", event.getType());
                        return ResponseEntity.ok().build();
                    }
                } catch (Exception e) {
                    log.error("Unsafe deserialization failed for event {}: {}", event.getType(), e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Deserialization failed");
                }
            }

            // Now extract metadata only if it's a Checkout Session
            Map<String, String> metadata = new HashMap<>();
            if (stripeObject instanceof Session session) {
                // FIX: Guard against session.getMetadata() returning null, which would cause a NullPointerException downstream
                if (session.getMetadata() != null) {
                    metadata = session.getMetadata();
                }
            }

            // Pass to your processor
            paymentProcessor.handleWebhookEvent(event.getType(), stripeObject, metadata);
            return ResponseEntity.ok().build();

        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    @PostMapping("/webhooks/cashfree")
    public ResponseEntity<String> handleCashfreeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "x-webhook-signature", required = false) String signature,
            @RequestHeader(value = "x-webhook-timestamp", required = false) String timestamp
    ) {
        try {
            cashfreePaymentService.handleWebhook(payload, signature, timestamp);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Cashfree webhook rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
