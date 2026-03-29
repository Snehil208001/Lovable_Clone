package com.snehil.project.lovable_clone.controller;

import com.snehil.project.lovable_clone.dto.project.FileContentResponse;
import com.snehil.project.lovable_clone.dto.project.FileNode;
import com.snehil.project.lovable_clone.security.JwtUserPrincipal;
import com.snehil.project.lovable_clone.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/files")
@RequiredArgsConstructor
public class FileController {
    private final ProjectFileService fileService;

    @GetMapping
    public ResponseEntity<List<FileNode>> getFileTree(
            @PathVariable Long projectId,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        return ResponseEntity.ok(fileService.getFileTree(projectId, currentUser.userId()));
    }

    @GetMapping("/{*path}")
    public ResponseEntity<FileContentResponse> getFile(
            @PathVariable Long projectId,
            @PathVariable String path,
            @AuthenticationPrincipal JwtUserPrincipal currentUser
    ) {
        return ResponseEntity.ok(fileService.getFileContent(projectId, path, currentUser.userId()));
    }

}
