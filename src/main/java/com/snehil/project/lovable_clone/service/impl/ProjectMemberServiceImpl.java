package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.member.InviteMemberRequest;
import com.snehil.project.lovable_clone.dto.member.MemberResponse;
import com.snehil.project.lovable_clone.dto.member.UpdateMemberRoleRequest;
import com.snehil.project.lovable_clone.entity.Project;
import com.snehil.project.lovable_clone.entity.ProjectMember;
import com.snehil.project.lovable_clone.entity.ProjectMemberId;
import com.snehil.project.lovable_clone.entity.User;
import com.snehil.project.lovable_clone.enums.ProjectRole;
import com.snehil.project.lovable_clone.error.BadRequestException;
import com.snehil.project.lovable_clone.error.ResourceNotFoundException;
import com.snehil.project.lovable_clone.mapper.ProjectMemberMapper;
import com.snehil.project.lovable_clone.repository.ProjectMemberRepository;
import com.snehil.project.lovable_clone.repository.ProjectRepository;
import com.snehil.project.lovable_clone.repository.UserRepository;
import com.snehil.project.lovable_clone.service.ProjectMemberService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    UserRepository userRepository;

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(projectId, userId);

        return projectMemberRepository.findByIdProjectId(projectId)
                .stream()
                .map(projectMemberMapper::toProjectMemberResponseFromMember)
                .toList();
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(projectId, userId);

        User invitee = userRepository.findByUsername(request.username()).orElseThrow(
                () -> new ResourceNotFoundException("User", request.username())
        );

        if (invitee.getId().equals(userId)){
            throw new BadRequestException("Cannot invite yourself");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, invitee.getId());

        if (projectMemberRepository.existsById(projectMemberId)) {
            throw new BadRequestException("User is already a member");
        }

        ProjectMember member = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .user(invitee)
                .projectRole(request.role())
                .invitedAt(Instant.now())
                .build();

        projectMemberRepository.save(member);

        return projectMemberMapper.toProjectMemberResponseFromMember(member);
    }

    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(projectId, userId);

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectMember", memberId.toString()));

        projectMember.setProjectRole(request.role());
        projectMemberRepository.save(projectMember);
        return projectMemberMapper.toProjectMemberResponseFromMember(projectMember);
    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public Void removeProjectMember(Long projectId, Long memberId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));

        ProjectMember currentUserMember = projectMemberRepository.findById(new ProjectMemberId(projectId, userId))
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        boolean isOwner = currentUserMember.getProjectRole() == ProjectRole.OWNER;
        boolean isRemovingSelf = memberId.equals(userId);

        if (!isOwner && !isRemovingSelf) {
            throw new AccessDeniedException("Not allowed to remove this member. Only owners can remove other users.");
        }

        ProjectMemberId targetMemberId = new ProjectMemberId(projectId, memberId);

        if (!projectMemberRepository.existsById(targetMemberId)) {
            throw new ResourceNotFoundException("Member", memberId.toString());
        }

        projectMemberRepository.deleteById(targetMemberId);

        return null;
    }

    private Project getProjectIfAccessibleAndNotDeleted(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));

        if (project.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Project", projectId.toString());
        }

        projectMemberRepository.findById(new ProjectMemberId(projectId, userId))
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        return project;
    }
}