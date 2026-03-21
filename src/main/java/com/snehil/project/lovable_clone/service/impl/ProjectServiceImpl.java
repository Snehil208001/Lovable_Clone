package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.project.ProjectRequest;
import com.snehil.project.lovable_clone.dto.project.ProjectResponse;
import com.snehil.project.lovable_clone.dto.project.ProjectSummaryResponse;
import com.snehil.project.lovable_clone.entity.Project;
import com.snehil.project.lovable_clone.entity.User;
import com.snehil.project.lovable_clone.mapper.ProjectMapper;
import com.snehil.project.lovable_clone.repository.ProjectRepository;
import com.snehil.project.lovable_clone.repository.UserRepository;
import com.snehil.project.lovable_clone.service.ProjectService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {

    ProjectRepository projectRepository;
    UserRepository userRepository;
    ProjectMapper projectMapper;

    @Override
    public ProjectResponse createProject(ProjectRequest request, Long userId) {

        User owner = userRepository.findById(userId).orElseThrow();

        Project project = Project.builder()
                .name(request.name())
                .owner(owner)
                .isPublic(false)
                .build();

        project = projectRepository.save(project);

        return projectMapper.toProjectResponse(project);
    }

    @Override
    public List<ProjectSummaryResponse> getUserProjects(Long userId) {
        var projects = projectRepository.findAllAccessibleByUser(userId);
        return projectMapper.toListOfProjectSummaryResponse(projects);
    }

    @Override
    public ProjectResponse getUserProjectbyId(Long id, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(id, userId);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(id, userId);

        // Update fields
        project.setName(request.name());

        project = projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public void softDelete(Long id, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(id, userId);

        // Set the deletedAt timestamp for soft deletion
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }

    /**
     * Helper method to fetch a project and ensure it belongs to the user
     * and has not been soft-deleted.
     */
    private Project getProjectIfAccessibleAndNotDeleted(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        if (!project.getOwner().getId().equals(userId) || project.getDeletedAt() != null) {
            throw new RuntimeException("Project not found or access denied");
        }

        return project;
    }
}