package com.wrg.repository;

import com.wrg.model.ReportStatus;
import com.wrg.model.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    List<WeeklyReport> findByUserIdOrderByWeekStartDateDesc(Long userId);

    List<WeeklyReport> findByWeekStartDate(LocalDate weekStartDate);

    @Query("""
        SELECT r FROM WeeklyReport r
        WHERE (:userId IS NULL OR r.user.id = :userId)
          AND (:projectId IS NULL OR r.project.id = :projectId)
          AND (:status IS NULL OR r.status = :status)
          AND (:from IS NULL OR r.weekStartDate >= :from)
          AND (:to IS NULL OR r.weekStartDate <= :to)
        ORDER BY r.weekStartDate DESC, r.user.fullName ASC
        """)
    List<WeeklyReport> search(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("status") ReportStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT r FROM WeeklyReport r WHERE r.weekStartDate >= :from ORDER BY r.weekStartDate ASC")
    List<WeeklyReport> findAllSince(@Param("from") LocalDate from);
}
