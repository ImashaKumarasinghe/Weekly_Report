package com.wrg.service;

import com.wrg.dto.ProjectRequest;
import com.wrg.dto.ProjectResponse;
import com.wrg.dto.UserSummary;
import com.wrg.exception.ApiException;
import com.wrg.model.Project;
import com.wrg.model.Role;
import com.wrg.model.User;
import com.wrg.repository.ProjectRepository;
import com.wrg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    /**
     * Managers see every project (active and inactive) for management purposes.
     * Team members only see active projects that are either open to everyone
     * (no members assigned) or that they've been explicitly assigned to.
     */
    public List<ProjectResponse> getVisibleForCurrentUser() {
        User currentUser = currentUserService.getCurrentUser();
        List<Project> projects = projectRepository.findAll();

        if (currentUser.getRole() == Role.MANAGER) {
            return projects.stream().map(this::toResponse).toList();
        }

        return projects.stream()
                .filter(Project::isActive)
                .filter(p -> isAccessibleToUser(p, currentUser))
                .map(this::toResponse)
                .toList();
    }

    public boolean isAccessibleToUser(Project project, User user) {
        if (user.getRole() == Role.MANAGER) return true;
        return project.getMembers().isEmpty()
                || project.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId()));
    }

    public Project getProjectEntity(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
    }

    public ProjectResponse create(ProjectRequest request) {
        if (projectRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ApiException("A project with this name already exists", HttpStatus.CONFLICT);
        }
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .members(resolveMembers(request.getMemberIds()))
                .build();
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getProjectEntity(id);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setMembers(resolveMembers(request.getMemberIds()));
        return toResponse(projectRepository.save(project));
    }

    public void delete(Long id) {
        Project project = getProjectEntity(id);
        // Soft delete to preserve historical report integrity
        project.setActive(false);
        projectRepository.save(project);
    }

    private Set<User> resolveMembers(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return new HashSet<>();
        }
        List<User> users = userRepository.findAllById(memberIds);
        return new HashSet<>(users);
    }

    private ProjectResponse toResponse(Project project) {
        List<UserSummary> members = project.getMembers().stream()
                .map(u -> UserSummary.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .build())
                .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                .toList();

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .active(project.isActive())
                .members(members)
                .build();
    }
}
