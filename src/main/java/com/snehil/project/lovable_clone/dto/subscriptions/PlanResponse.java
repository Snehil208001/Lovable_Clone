package com.snehil.project.lovable_clone.dto.subscriptions;

import java.math.BigDecimal;

public record PlanResponse(
        Long id,
        String name,
        Integer maxProjects,
        Integer maxTokensPerDay,
        Boolean unlimitedAi,
        String price,
        BigDecimal amountInr
) {
}
