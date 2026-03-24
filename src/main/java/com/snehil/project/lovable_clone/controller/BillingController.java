package com.snehil.project.lovable_clone.controller;


import com.snehil.project.lovable_clone.dto.subscriptions.*;
import com.snehil.project.lovable_clone.security.JwtUserPrincipal;
import com.snehil.project.lovable_clone.service.PlanService;
import com.snehil.project.lovable_clone.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BillingController {

    private final PlanService planService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/api/plans")
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllActivePlans());
    }

    @GetMapping("/api/me/subscription")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(currentUser.userId()));
    }

    @PostMapping("/api/stripe/checkout")
    public ResponseEntity<CheckoutResponse> createCheckoutResponse(
            @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser
    ) {
        return ResponseEntity.ok(subscriptionService.createCheckoutSessionUrl(request, currentUser.userId()));
    }

    @PostMapping("/api/stripe/portal")
    public ResponseEntity<PostalResponse> openCustomerPortal(
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(subscriptionService.openCustomerPortal(currentUser.userId()));
    }
}
