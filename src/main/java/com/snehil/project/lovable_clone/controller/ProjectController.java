package com.snehil.project.lovable_clone.controller;

import com.snehil.project.lovable_clone.dto.project.ProjectRequest;
import com.snehil.project.lovable_clone.dto.project.ProjectResponse;
import com.snehil.project.lovable_clone.dto.project.ProjectSummaryResponse;
import com.snehil.project.lovable_clone.security.JwtUserPrincipal;
import com.snehil.project.lovable_clone.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects(
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(projectService.getUserProjects(currentUser.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(projectService.getUserProjectbyId(id, currentUser.userId()));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody @Valid ProjectRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request, currentUser.userId()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @RequestBody @Valid ProjectRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(projectService.updateProject(id, request, currentUser.userId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        projectService.softDelete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
