package com.wrg.dto;

import com.wrg.model.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private Long projectId;
    private String projectName;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String tasksCompleted;
    private String tasksPlanned;
    private String blockers;
    private Double hoursWorked;
    private String notes;
    private ReportStatus status;
    private Instant submittedAt;
    private Instant updatedAt;
}
