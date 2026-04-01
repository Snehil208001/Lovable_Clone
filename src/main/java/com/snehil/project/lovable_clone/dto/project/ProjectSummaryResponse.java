package com.snehil.project.lovable_clone.dto.project;

import java.time.Instant;

public record ProjectSummaryResponse(
        Long id,
        String projectName,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
