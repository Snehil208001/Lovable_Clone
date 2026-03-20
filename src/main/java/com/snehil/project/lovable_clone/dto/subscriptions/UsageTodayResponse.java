package com.snehil.project.lovable_clone.dto.subscriptions;

public record UsageTodayResponse(
        int tokensuser,
        int tokensLimit,
        int previewsRunning,
        int previewsLimit
) {
}
