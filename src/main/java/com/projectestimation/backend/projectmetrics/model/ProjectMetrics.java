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
    @JoinColumn(
            name = "opportunity_id",
            nullable = false,
            unique = true
    )
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
 // CUMULATIVE ANALYSIS
 // =========================

 private Integer analysisPlannedDuration;

 private Integer analysisActualDuration;

 private Double analysisScheduleVariance;

 private Double analysisPlannedEffort;

 private Double analysisActualEffort;

 private Double analysisProductivity;

 private Double analysisEffortVariance;

 private Double analysisEffortInAnalysis;

 private Integer analysisReviewDefects;

 private Double analysisReviewEffort;

 private Double analysisDefectDensity;

 private Double analysisDefectDetectionRate;

 private Double analysisDefectRate;


 // =========================
 // CUMULATIVE DESIGN
 // =========================

 private Integer designPlannedDuration;

 private Integer designActualDuration;

 private Double designScheduleVariance;

 private Double designPlannedEffort;

 private Double designActualEffort;

 private Double designProductivity;

 private Double designEffortVariance;

 private Double designEffortInAnalysis;

 private Integer designReviewDefects;

 private Double designReviewEffort;

 private Double designDefectDensity;

 private Double designDefectDetectionRate;

 private Double designDefectRate;


 // =========================
 // CUMULATIVE CODING
 // =========================

 private Integer codingPlannedDuration;

 private Integer codingActualDuration;

 private Double codingScheduleVariance;

 private Double codingPlannedEffort;

 private Double codingActualEffort;

 private Double codingEffortVariance;

 private Double codingEffort;

 private Integer codeReviewDefects;

 private Double codeReviewEffort;

 private Double codingDefectDensity;

 private Double codeReviewDetectionRate;

 private Integer unitTestingDefects;

 private Double unitTestingEffort;

 private Double unitTestingDetectionRate;

 private Double codingDefectRate;

 private Double codingProductivity;


 // =========================
 // CUMULATIVE SIT
 // =========================

 private Integer sitPlannedDuration;

 private Integer sitActualDuration;

 private Double sitScheduleVariance;

 private Double sitPlannedEffort;

 private Double sitActualEffort;

 private Double sitEffortVariance;

 private Integer totalTestConditions;

 private Double testCaseWritingEffort;

 private Integer testCaseReviewDefects;

 private Double testCaseReviewEffort;

 private Double testExecutionEffort;

 private Double testCaseReviewDetectionRate;

 private Integer sitDefects;

 private Double sitEffort;

 private Double sitDetectionRate;


 // =========================
 // CUMULATIVE OTHER ACTIVITY
 // =========================

 private Double otherActualTotal;

 private Double otherActualProjectManagement;

 private Double otherActualSupportGroup;

 private Double otherActualOthers;

 private Double otherPlannedTotal;

 private Double otherPlannedProjectManagement;

 private Double otherPlannedSupportGroup;

 private Double otherPlannedOthers;


 // =========================
 // SPRINT METRICS
 // =========================
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