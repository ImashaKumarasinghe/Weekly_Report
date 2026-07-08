package com.wrg.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Every report has the exact same fixed set of fields, in the same order,
 * for every user. Users cannot add/remove/reorder fields - this keeps
 * reports comparable across the team on the manager dashboard.
 */
@Entity
@Table(name = "weekly_reports", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_week_project", columnNames = {"user_id", "week_start_date", "project_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Monday of the reporting week - identifies "Week / date range"
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String tasksCompleted;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String tasksPlanned;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String blockers;

    private Double hoursWorked;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.DRAFT;

    private Instant submittedAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
