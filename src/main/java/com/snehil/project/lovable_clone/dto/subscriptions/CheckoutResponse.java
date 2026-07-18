package com.snehil.project.lovable_clone.dto.subscriptions;

public record CheckoutResponse(
        String checkoutUrl,
        String paymentSessionId,
        String provider,
        /** Cashfree SDK mode: sandbox | production */
        String cashfreeEnv
) {
    public static CheckoutResponse stripe(String checkoutUrl) {
        return new CheckoutResponse(checkoutUrl, null, "STRIPE", null);
    }

    public static CheckoutResponse cashfree(String paymentSessionId, String cashfreeEnv) {
        return new CheckoutResponse(null, paymentSessionId, "CASHFREE", cashfreeEnv);
    }
}
