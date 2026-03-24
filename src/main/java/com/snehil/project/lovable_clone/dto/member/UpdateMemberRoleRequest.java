package com.snehil.project.lovable_clone.dto.member;

import com.snehil.project.lovable_clone.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
       @NotNull ProjectRole role
) {
}
