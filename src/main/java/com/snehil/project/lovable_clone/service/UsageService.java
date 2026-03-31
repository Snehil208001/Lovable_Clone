package com.snehil.project.lovable_clone.service;

import com.snehil.project.lovable_clone.dto.subscriptions.PlanLimitsResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.UsageTodayResponse;
import org.jspecify.annotations.Nullable;

public interface UsageService {
    UsageTodayResponse getTodayUsageOfUser(Long userId);
    void recordTokenUsage(Long userId, int actualTokens);

    PlanLimitsResponse getCurrentSubscriptionLimitsOfUser(Long userId);
}
