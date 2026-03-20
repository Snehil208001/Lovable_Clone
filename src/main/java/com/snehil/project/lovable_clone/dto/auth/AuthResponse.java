package com.snehil.project.lovable_clone.dto.auth;

public record AuthResponse(
        String token,
        UserProfileResponse userProfileResponse
) {
}
