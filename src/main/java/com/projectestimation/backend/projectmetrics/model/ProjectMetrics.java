package com.projectestimation.backend.projectmetrics.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.projectestimation.backend.opportunity.model.Opportunity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    // =========================
    // SUMMARY
    // =========================

    private String projectName;

    private String releaseNo;

    private Double originalSize;

    private Double actualSize;

    private Double sizeVariance;

    private Double totalPlannedEffortWithoutPm;

    private Double totalPlannedEffort;

    private Double totalActualEffortWithoutPm;

    private Double totalActualEffort;

    private Double effortVariance;

    private Integer plannedDuration;

    private Integer actualDuration;

    private Double scheduleVariance;

    private Double actualOverallProductivity;

    private Double reviewEffectiveness;

    private Double testingEffectiveness;

    // =========================
    // QUALITY / UAT
    // =========================

    private Double averagePreDeliveryDefectDensity;

    private Integer uatDefects;

    private Double postDeliveryDefectDensity;

    private Double overallDefectDensity;

    private Double plannedUatEffort;

    private Double actualUatEffort;

    private Double overallDefectRate;

    private Double defectRemovalEfficiency;

    // =========================
    // SPRINT METRICS
    // =========================

    @OneToMany(
        mappedBy = "projectMetrics",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<ProjectMetricsSprint> sprints = new ArrayList<>();

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

    public void addSprint(ProjectMetricsSprint sprint) {
        sprints.add(sprint);
        sprint.setProjectMetrics(this);
    }
}