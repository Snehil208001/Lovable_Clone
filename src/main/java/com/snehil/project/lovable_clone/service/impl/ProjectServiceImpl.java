package com.snehil.project.lovable_clone.service.impl;

import com.snehil.project.lovable_clone.dto.project.ProjectRequest;
import com.snehil.project.lovable_clone.dto.project.ProjectResponse;
import com.snehil.project.lovable_clone.dto.project.ProjectSummaryResponse;
import com.snehil.project.lovable_clone.entity.Project;
import com.snehil.project.lovable_clone.entity.ProjectMember;
import com.snehil.project.lovable_clone.entity.ProjectMemberId;
import com.snehil.project.lovable_clone.entity.User;
import com.snehil.project.lovable_clone.enums.ProjectRole;
import com.snehil.project.lovable_clone.error.BadRequestException;
import com.snehil.project.lovable_clone.error.ResourceNotFoundException;
import com.snehil.project.lovable_clone.mapper.ProjectMapper;
import com.snehil.project.lovable_clone.repository.ProjectMemberRepository;
import com.snehil.project.lovable_clone.repository.ProjectRepository;
import com.snehil.project.lovable_clone.repository.UserRepository;
import com.snehil.project.lovable_clone.service.ProjectService;
import com.snehil.project.lovable_clone.service.ProjectTemplateService;
import com.snehil.project.lovable_clone.service.SubscriptionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
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
    ProjectMemberRepository projectMemberRepository;
    SubscriptionService subscriptionService;
    ProjectTemplateService projectTemplateService;

    @Override
    public ProjectResponse createProject(ProjectRequest request, Long userId) {

        if (!subscriptionService.canCreateNewProject()) {
            throw new BadRequestException("User cannot create a New Project with current plan, Upgrade plan now ");
        }

        User owner = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User", userId.toString())
        );

        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .owner(owner) // ADDED: Set the owner before saving
                .build();
        project = projectRepository.save(project);

        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), owner.getId());
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(ProjectRole.OWNER)
                .user(owner)
                .acceptedAt(Instant.now())
                .invitedAt(Instant.now())
                .project(project)
                .build();

        projectMemberRepository.save(projectMember);

        projectTemplateService.initializeProjectFromTemplate(project.getId());

        return projectMapper.toProjectResponse(project);
    }

    @Override
    public List<ProjectSummaryResponse> getUserProjects(Long userId) {
        var projects = projectRepository.findAllAccessibleByUser(userId);
        return projectMapper.toListOfProjectSummaryResponse(projects);
    }

    @Override
    // Use #id to map the method parameter "id" to the SecurityExpression method
    @PreAuthorize("@security.canViewProject(#id)")
    public ProjectResponse getUserProjectbyId(Long id, Long userId) {
        // id is the projectId
        Project project = getProjectIfAccessibleAndNotDeleted(id, userId);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canEditProject(#id)")
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(id, userId);

        project.setName(request.name());

        project = projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canEditProject(#id)")
    public void softDelete(Long id, Long userId) {
        Project project = getProjectIfAccessibleAndNotDeleted(id, userId);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }

    private Project getProjectIfAccessibleAndNotDeleted(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
    }
}