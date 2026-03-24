package com.snehil.project.lovable_clone.dto.member;

import com.snehil.project.lovable_clone.enums.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @Email @NotBlank String username,
        @NotNull ProjectRole role
) {
}
