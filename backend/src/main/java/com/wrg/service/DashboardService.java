package com.wrg.service;

import com.wrg.dto.DashboardSummary;
import com.wrg.model.Role;
import com.wrg.model.User;
import com.wrg.model.WeeklyReport;
import com.wrg.repository.UserRepository;
import com.wrg.repository.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WeeklyReportRepository reportRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public DashboardSummary getSummary() {
        LocalDate thisWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<User> teamMembers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TEAM_MEMBER && u.isActive())
                .toList();

        List<WeeklyReport> thisWeekReports = reportRepository.findByWeekStartDate(thisWeekStart);

        Map<Long, WeeklyReport> latestReportByUser = new HashMap<>();
        for (WeeklyReport r : thisWeekReports) {
            latestReportByUser.put(r.getUser().getId(), r);
        }

        int submittedCount = (int) thisWeekReports.stream()
                .filter(r -> r.getStatus().name().equals("SUBMITTED") || r.getStatus().name().equals("LATE"))
                .map(r -> r.getUser().getId()).distinct().count();

        double compliance = teamMembers.isEmpty() ? 0.0
                : (submittedCount * 100.0) / teamMembers.size();

        int openBlockers = (int) thisWeekReports.stream()
                .filter(r -> r.getBlockers() != null && !r.getBlockers().isBlank())
                .count();

        // Tasks trend: report counts per week for the last 8 weeks
        LocalDate eightWeeksAgo = thisWeekStart.minusWeeks(7);
        List<WeeklyReport> recentReports = reportRepository.findAllSince(eightWeeksAgo);
        Map<String, Long> byWeek = recentReports.stream()
                .collect(Collectors.groupingBy(r -> r.getWeekStartDate().format(ISO), LinkedHashMap::new, Collectors.counting()));
        List<DashboardSummary.TasksTrendPoint> trend = byWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> DashboardSummary.TasksTrendPoint.builder().week(e.getKey()).reportCount(e.getValue()).build())
                .toList();

        // Submission status by member, this week
        List<DashboardSummary.SubmissionStatusPoint> statusByMember = teamMembers.stream()
                .map(u -> {
                    WeeklyReport r = latestReportByUser.get(u.getId());
                    String status = r == null ? "PENDING" : r.getStatus().name();
                    return DashboardSummary.SubmissionStatusPoint.builder()
                            .memberName(u.getFullName())
                            .status(status)
                            .build();
                })
                .toList();

        // Workload by project, this week
        Map<String, Long> byProject = thisWeekReports.stream()
                .collect(Collectors.groupingBy(r -> r.getProject().getName(), LinkedHashMap::new, Collectors.counting()));
        List<DashboardSummary.WorkloadPoint> workload = byProject.entrySet().stream()
                .map(e -> DashboardSummary.WorkloadPoint.builder().projectName(e.getKey()).reportCount(e.getValue()).build())
                .toList();

        // Recent activity - last 10 reports overall, most recently updated first
        List<DashboardSummary.ActivityItem> activity = reportRepository.findAll().stream()
                .sorted(Comparator.comparing((WeeklyReport r) ->
                        r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt()).reversed())
                .limit(10)
                .map(r -> DashboardSummary.ActivityItem.builder()
                        .memberName(r.getUser().getFullName())
                        .projectName(r.getProject().getName())
                        .status(r.getStatus().name())
                        .timestamp((r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt()).toString())
                        .build())
                .toList();

        return DashboardSummary.builder()
                .totalTeamMembers(teamMembers.size())
                .reportsSubmittedThisWeek(submittedCount)
                .complianceRate(Math.round(compliance * 10.0) / 10.0)
                .openBlockersCount(openBlockers)
                .tasksCompletedTrend(trend)
                .submissionStatusByMember(statusByMember)
                .workloadByProject(workload)
                .recentActivity(activity)
                .build();
    }
}
