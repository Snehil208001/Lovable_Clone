package com.snehil.project.lovable_clone.service;

import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutRequest;
import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.PostalResponse;
import com.stripe.model.StripeObject;

import java.util.Map;

public interface PaymentProcessor {

    CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request, Long userId);

    PostalResponse openCustomerPortal(Long userId);

    void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata);
}