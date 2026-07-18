package com.snehil.project.lovable_clone.dto.subscriptions;

public record CheckoutRequest(
        Long planId,
        /** STRIPE (default) or CASHFREE */
        String provider,
        /** Required by Cashfree for some payment methods; optional otherwise. */
        String customerPhone
) {
    public CheckoutRequest(Long planId) {
        this(planId, "STRIPE", null);
    }
}
