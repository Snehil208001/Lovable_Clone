package com.snehil.project.lovable_clone.dto.subscriptions;

/**
 * Checkout payload. Do not add convenience constructors — Jackson can bind them
 * and ignore {@code provider}, which silently falls back to Stripe.
 */
public record CheckoutRequest(
        Long planId,
        /** STRIPE (default when null/blank) or CASHFREE */
        String provider,
        /** Required by Cashfree for some payment methods; optional otherwise. */
        String customerPhone
) {
}
