package com.snehil.project.lovable_clone.service;

import com.snehil.project.lovable_clone.dto.auth.UserProfileResponse;

public interface UserService {
    UserProfileResponse getProfile(Long userId);
}
