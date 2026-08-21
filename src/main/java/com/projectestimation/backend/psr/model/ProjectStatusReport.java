package com.projectestimation.backend.psr.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.projectestimation.backend.opportunity.model.Opportunity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_status_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatusReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    // =========================
    // REPORT INFORMATION
    // =========================

    private String fileName;

    private String fileLocation;

    private LocalDateTime generatedAt;

    private LocalDate reportDate;

    // =========================
    // PSR CONTENT
    // =========================

    @Column(columnDefinition = "TEXT")
    private String activitiesPerformed;

    @Column(columnDefinition = "TEXT")
    private String nextWeekPlannedActivities;

    @Column(columnDefinition = "TEXT")
    private String riskStatus;

    @Column(columnDefinition = "TEXT")
    private String trainingOfProjectTeamMembers;

    @Column(columnDefinition = "TEXT")
    private String issuesManagementAttention;

    // =========================
    // AUDIT
    // =========================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}