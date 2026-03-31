package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.subscriptions.PlanLimitsResponse;
import com.snehil.project.lovable_clone.dto.subscriptions.UsageTodayResponse;
import com.snehil.project.lovable_clone.service.UsageService;
import org.springframework.stereotype.Service;

@Service
public class UsageServiceImpl implements UsageService {
    @Override
    public UsageTodayResponse getTodayUsageOfUser(Long userId) {
        return null;
    }

    @Override
    public void recordTokenUsage(Long userId, int actualTokens) {

    }

    @Override
    public PlanLimitsResponse getCurrentSubscriptionLimitsOfUser(Long userId) {
        return null;
    }
}
