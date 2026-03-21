package com.snehil.project.lovable_clone.dto.subscriptions;

public record PlanLimitsResponse(
        String planeName,
        Integer maxTokensPerDay,
        Integer maxProjects,
        Boolean unlimitedAi
) {
}
