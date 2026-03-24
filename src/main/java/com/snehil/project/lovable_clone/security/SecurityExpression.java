package com.snehil.project.lovable_clone.security;

import com.snehil.project.lovable_clone.enums.ProjectPermission;
import com.snehil.project.lovable_clone.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("security")
@RequiredArgsConstructor
public class SecurityExpression {

    private final ProjectMemberRepository projectMemberRepository;
    private final AuthUtil authUtil;

    public boolean canViewProject(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .map(role -> role.getPermissions().contains(ProjectPermission.VIEW))
                .orElse(false);
    }

    public boolean canEditProject(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .map(role -> role.getPermissions().contains(ProjectPermission.EDIT))
                .orElse(false);
    }

    public boolean canManageMembers(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .map(role -> role.getPermissions().contains(ProjectPermission.MANAGE_MEMBERS))
                .orElse(false);
    }
}