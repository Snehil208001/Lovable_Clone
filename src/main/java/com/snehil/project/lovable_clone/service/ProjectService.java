package com.snehil.project.lovable_clone.service;

import com.snehil.project.lovable_clone.dto.project.ProjectRequest;
import com.snehil.project.lovable_clone.dto.project.ProjectResponse;
import com.snehil.project.lovable_clone.dto.project.ProjectSummaryResponse;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface ProjectService {
    List<ProjectSummaryResponse> getUserProjects(Long userId);

    ProjectResponse getUserProjectbyId(Long id, Long userId);

    ProjectResponse createProject(ProjectRequest request, Long userId);

    ProjectResponse updateProject(Long id, ProjectRequest request, Long userId);

    void softDelete(Long id, Long userId);
}
