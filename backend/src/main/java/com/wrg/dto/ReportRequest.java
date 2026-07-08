package com.wrg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Fixed-shape request body. Every report, for every user, has exactly
 * these fields in this order - see the WeeklyReport entity docs.
 */
@Data
public class ReportRequest {

    @NotNull
    private LocalDate weekStartDate; // Monday of the reporting week

    @NotNull
    private Long projectId;

    @NotBlank
    private String tasksCompleted;

    private String tasksPlanned;

    private String blockers;

    private Double hoursWorked; // optional

    private String notes; // optional

    // true = submit immediately, false = save as draft
    private boolean submit;
}
