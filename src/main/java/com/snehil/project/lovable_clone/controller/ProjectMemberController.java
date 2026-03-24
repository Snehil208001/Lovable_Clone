package com.snehil.project.lovable_clone.controller;

import com.snehil.project.lovable_clone.dto.member.InviteMemberRequest;
import com.snehil.project.lovable_clone.dto.member.MemberResponse;
import com.snehil.project.lovable_clone.dto.member.UpdateMemberRoleRequest;
import com.snehil.project.lovable_clone.security.JwtUserPrincipal;
import com.snehil.project.lovable_clone.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getProjectMembers(
            @PathVariable Long projectId,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(projectMemberService.getProjectMembers(projectId, currentUser.userId()));
    }

    @PostMapping
    public ResponseEntity<MemberResponse> inviteMember(
            @PathVariable Long projectId,
            @RequestBody @Valid InviteMemberRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                projectMemberService.inviteMember(projectId, request, currentUser.userId())
        );
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestBody @Valid UpdateMemberRoleRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser
    ) {
        return ResponseEntity.ok((projectMemberService.updateMemberRole(projectId, memberId, request, currentUser.userId())));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal JwtUserPrincipal currentUser
    ) {
        projectMemberService.removeProjectMember(projectId, memberId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

}
