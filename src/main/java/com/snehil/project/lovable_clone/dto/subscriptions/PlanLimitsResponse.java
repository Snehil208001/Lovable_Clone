package com.snehil.project.lovable_clone.dto.subscriptions;

public record PlanLimitsResponse(
        String planeName,
        int maxTokensPerDay,
        int maxProjects,
        boolean unlimitedAi
) {
}
