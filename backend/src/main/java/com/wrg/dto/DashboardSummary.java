package com.wrg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class DashboardSummary {
    private int totalTeamMembers;
    private int reportsSubmittedThisWeek;
    private double complianceRate; // submitted / total team members, this week
    private int openBlockersCount;

    private List<TasksTrendPoint> tasksCompletedTrend;   // per week, count of "tasks completed" entries
    private List<SubmissionStatusPoint> submissionStatusByMember; // per member, this week
    private List<WorkloadPoint> workloadByProject;        // report count per project, this week
    private List<ActivityItem> recentActivity;

    @Data
    @AllArgsConstructor
    @Builder
    public static class TasksTrendPoint {
        private String week; // e.g. "2026-06-29"
        private long reportCount;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class SubmissionStatusPoint {
        private String memberName;
        private String status; // SUBMITTED / PENDING / LATE
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class WorkloadPoint {
        private String projectName;
        private long reportCount;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class ActivityItem {
        private String memberName;
        private String projectName;
        private String status;
        private String timestamp;
    }
}
