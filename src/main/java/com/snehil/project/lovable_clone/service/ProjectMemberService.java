package com.snehil.project.lovable_clone.service;

import com.snehil.project.lovable_clone.dto.member.InviteMemberRequest;
import com.snehil.project.lovable_clone.dto.member.MemberResponse;
import com.snehil.project.lovable_clone.entity.ProjectMember;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface ProjectMemberService {
    List<ProjectMember> getProjectMembers(Long projectId, Long userId);

    MemberResponse inviteMember(Long projectId, InviteMemberRequest request, Long userId);

    MemberResponse updateMemberRole(Long projectId, Long memberId, InviteMemberRequest request, Long userId);

    MemberResponse DeleteProjectMember(Long projectId, Long memberId, Long userId);
}
