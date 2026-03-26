package com.snehil.project.lovable_clone.dto.subscriptions;

import java.time.Instant;

public record SubscriptionResponse(
        PlanResponse plan,
        String status,
        Instant currentPeriod,
        Long tokensUsedThisCycle
) {
}
