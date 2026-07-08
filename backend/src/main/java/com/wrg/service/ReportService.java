package com.wrg.service;

import com.wrg.dto.ReportRequest;
import com.wrg.dto.ReportResponse;
import com.wrg.exception.ApiException;
import com.wrg.model.*;
import com.wrg.repository.ProjectRepository;
import com.wrg.repository.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final WeeklyReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;
    private final ProjectService projectService;

    public ReportResponse create(ReportRequest request) {
        User user = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));

        if (!projectService.isAccessibleToUser(project, user)) {
            throw new ApiException("You are not assigned to this project", HttpStatus.FORBIDDEN);
        }

        WeeklyReport report = WeeklyReport.builder()
                .user(user)
                .project(project)
                .weekStartDate(request.getWeekStartDate())
                .weekEndDate(request.getWeekStartDate().plusDays(6))
                .tasksCompleted(request.getTasksCompleted())
                .tasksPlanned(request.getTasksPlanned())
                .blockers(request.getBlockers())
                .hoursWorked(request.getHoursWorked())
                .notes(request.getNotes())
                .status(request.isSubmit() ? ReportStatus.SUBMITTED : ReportStatus.DRAFT)
                .submittedAt(request.isSubmit() ? Instant.now() : null)
                .build();

        return toResponse(reportRepository.save(report));
    }

    public ReportResponse update(Long id, ReportRequest request) {
        WeeklyReport report = getOwnedReportOrThrow(id);
        User user = currentUserService.getCurrentUser();

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));

        if (!projectService.isAccessibleToUser(project, user)) {
            throw new ApiException("You are not assigned to this project", HttpStatus.FORBIDDEN);
        }

        report.setProject(project);
        report.setWeekStartDate(request.getWeekStartDate());
        report.setWeekEndDate(request.getWeekStartDate().plusDays(6));
        report.setTasksCompleted(request.getTasksCompleted());
        report.setTasksPlanned(request.getTasksPlanned());
        report.setBlockers(request.getBlockers());
        report.setHoursWorked(request.getHoursWorked());
        report.setNotes(request.getNotes());

        if (request.isSubmit() && report.getStatus() != ReportStatus.SUBMITTED) {
            report.setStatus(ReportStatus.SUBMITTED);
            report.setSubmittedAt(Instant.now());
        }

        return toResponse(reportRepository.save(report));
    }

    public void delete(Long id) {
        WeeklyReport report = getOwnedReportOrThrow(id);
        reportRepository.delete(report);
    }

    public List<ReportResponse> getMyReports() {
        User user = currentUserService.getCurrentUser();
        return reportRepository.findByUserIdOrderByWeekStartDateDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    /** Manager-only: search/filter across the whole team. */
    public List<ReportResponse> searchTeamReports(Long userId, Long projectId, ReportStatus status,
                                                   LocalDate from, LocalDate to) {
        return reportRepository.search(userId, projectId, status, from, to)
                .stream().map(this::toResponse).toList();
    }

    private WeeklyReport getOwnedReportOrThrow(Long id) {
        User user = currentUserService.getCurrentUser();
        WeeklyReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ApiException("Report not found", HttpStatus.NOT_FOUND));

        boolean isOwner = report.getUser().getId().equals(user.getId());
        boolean isManager = user.getRole() == Role.MANAGER;

        if (!isOwner && !isManager) {
            throw new ApiException("You can only modify your own reports", HttpStatus.FORBIDDEN);
        }
        return report;
    }

    private ReportResponse toResponse(WeeklyReport r) {
        return ReportResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userFullName(r.getUser().getFullName())
                .projectId(r.getProject().getId())
                .projectName(r.getProject().getName())
                .weekStartDate(r.getWeekStartDate())
                .weekEndDate(r.getWeekEndDate())
                .tasksCompleted(r.getTasksCompleted())
                .tasksPlanned(r.getTasksPlanned())
                .blockers(r.getBlockers())
                .hoursWorked(r.getHoursWorked())
                .notes(r.getNotes())
                .status(r.getStatus())
                .submittedAt(r.getSubmittedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
