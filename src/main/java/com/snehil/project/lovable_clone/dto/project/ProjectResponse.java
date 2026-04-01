package com.snehil.project.lovable_clone.dto.project;

import com.snehil.project.lovable_clone.dto.auth.UserProfileResponse;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        UserProfileResponse owner
) {
}
