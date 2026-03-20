package com.snehil.project.lovable_clone.dto.member;

import com.snehil.project.lovable_clone.enums.ProjectRole;

public record InviteMemberRequest(
        String email,
        ProjectRole role
) {
}
