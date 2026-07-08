package com.wrg.service;

import com.wrg.dto.ProjectRequest;
import com.wrg.dto.ProjectResponse;
import com.wrg.exception.ApiException;
import com.wrg.model.Project;
import com.wrg.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ProjectResponse create(ProjectRequest request) {
        if (projectRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ApiException("A project with this name already exists", HttpStatus.CONFLICT);
        }
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .build();
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        return toResponse(projectRepository.save(project));
    }

    public void delete(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
        // Soft delete to preserve historical report integrity
        project.setActive(false);
        projectRepository.save(project);
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .active(project.isActive())
                .build();
    }
}
