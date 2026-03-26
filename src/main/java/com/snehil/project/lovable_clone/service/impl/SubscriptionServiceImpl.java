package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutRequest;
import com.snehil.project.lovable_clone.dto.subscriptions.CheckoutResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.PostalResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.SubscriptionResponse;
import com.snehil.project.lovable_clone.entity.Plan;
import com.snehil.project.lovable_clone.entity.Subscription;
import com.snehil.project.lovable_clone.entity.User;
import com.snehil.project.lovable_clone.enums.SubscriptionStatus;
import com.snehil.project.lovable_clone.error.ResourceNotFoundException;
import com.snehil.project.lovable_clone.mapper.SubscriptionMapper;
import com.snehil.project.lovable_clone.repository.PlanRepository;
import com.snehil.project.lovable_clone.repository.SubscriptionRepository;
import com.snehil.project.lovable_clone.repository.UserRepository;
import com.snehil.project.lovable_clone.security.AuthUtil;
import com.snehil.project.lovable_clone.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {


    private final AuthUtil authUtil;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    @Override
    public SubscriptionResponse getCurrentSubscription() {
        Long userId = authUtil.getCurrentUserId();

        var currentSubscription = subscriptionRepository.findByUserIdAndStatusIn(userId, Set.of(
                SubscriptionStatus.ACTIVE,SubscriptionStatus.PAST_DUE,
                SubscriptionStatus.TRAILING
        )).orElse(
                new Subscription()
        );
        return subscriptionMapper.toSubscriptionResponse(currentSubscription);
    }

    @Override
    public void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId) {
        boolean exists = subscriptionRepository.existsByStripeSubscriptionId(subscriptionId);
        if (exists) return;

        User user = getUser(userId);
        Plan plan = getPlan(planId);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .stripeSubscriptionId(subscriptionId)
                .status(SubscriptionStatus.INCOMPLETE)
                .build();

        subscriptionRepository.save(subscription);
    }

    @Override
    public void updateSubscription(String subscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId) {

    }

    @Override
    public void cancelSubscription(String subscriptionId) {

    }

    @Override
    public void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd) {
        Subscription subscription = getSubscription(gatewaySubscriptionId);

        Instant newStart = periodStart != null ? periodStart : subscription.getCurrentPeriodEnd();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE || subscription.getStatus() == SubscriptionStatus.INCOMPLETE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }
        subscriptionRepository.save(subscription);
    }



    @Override
    public void markSubscriptionPastDue(String gatewaySubscriptionId) {

    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User",userId.toString()));
    }

    private Plan getPlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId.toString()));
    }

    private Subscription getSubscription(String gatewaySubscriptionId) {
        return subscriptionRepository.findByStripeSubscriptionId(gatewaySubscriptionId).orElseThrow(() ->
                new ResourceNotFoundException("Subscription", gatewaySubscriptionId));
    }

}
