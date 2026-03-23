package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.member.InviteMemberRequest;
import com.snehil.project.lovable_clone.dto.member.MemberResponse;
import com.snehil.project.lovable_clone.dto.member.UpdateMemberRoleRequest;
import com.snehil.project.lovable_clone.entity.Project;
import com.snehil.project.lovable_clone.entity.ProjectMember;
import com.snehil.project.lovable_clone.entity.ProjectMemberId;
import com.snehil.project.lovable_clone.entity.User;
import com.snehil.project.lovable_clone.mapper.ProjectMemberMapper;
import com.snehil.project.lovable_clone.repository.ProjectMemberRepository;
import com.snehil.project.lovable_clone.repository.ProjectRepository;
import com.snehil.project.lovable_clone.repository.UserRepository;
import com.snehil.project.lovable_clone.service.ProjectMemberService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    private final UserRepository userRepository;

    @Override
    public List<MemberResponse> getProjectMembers(Long projectId, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(projectId, userId);

        List<MemberResponse> memberResponsesList = new ArrayList<>();
        memberResponsesList.add(projectMemberMapper.toProjectMemberResponseFromOwner(project.getOwner()));

        memberResponsesList.addAll(
                projectMemberRepository.findByIdProjectId(projectId)
                        .stream()
                        .map(projectMemberMapper::toProjectMemberResponseFromMember)
                        .toList());

        return memberResponsesList;
    }

    @Override
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(projectId, userId);

        if (!project.getOwner().getId().equals(userId)){
            throw new RuntimeException("Not allowed");
        }

        User invitee = userRepository.findByEmail(request.email()).orElseThrow();

        if (invitee.getId().equals(userId)){
            throw new RuntimeException("Cannot invite yourself");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId,invitee.getId());

        if (projectMemberRepository.existsById(projectMemberId)) {
            throw new RuntimeException("Cannot invite once again");
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
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(projectId,userId);

        if (!project.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Not allowed");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId,memberId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId).orElseThrow();

        projectMember.setProjectRole(request.role());
        projectMemberRepository.save(projectMember);
        return projectMemberMapper.toProjectMemberResponseFromMember(projectMember);
    }

    @Override
    public Void removeProjectMember(Long projectId, Long memberId, Long userId) {
        // 1. Fetch the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // 2. Check if the project is deleted
        if (project.getDeletedAt() != null) {
            throw new RuntimeException("Project is deleted");
        }

        // 3. Authorization Logic:
        // Only the owner can remove others, BUT a user should be able to remove themselves
        boolean isOwner = project.getOwner().getId().equals(userId);
        boolean isRemovingSelf = memberId.equals(userId);

        if (!isOwner && !isRemovingSelf) {
            throw new RuntimeException("Not allowed to remove this member");
        }

        // 4. Verification and Deletion
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);

        if (!projectMemberRepository.existsById(projectMemberId)) {
            throw new RuntimeException("Member not found in this project");
        }

        projectMemberRepository.deleteById(projectMemberId);

        return null;
    }

    private Project getProjectIfAccessibleAndNotDeleted(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        if (!project.getOwner().getId().equals(userId) || project.getDeletedAt() != null) {
            throw new RuntimeException("Project not found or access denied");
        }

        return project;
    }
}
