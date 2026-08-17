package com.projectestimation.backend.projectmetrics.model;

import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_metrics_sprint")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMetricsSprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // PARENT METRICS
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_metrics_id", nullable = false)
    private ProjectMetrics projectMetrics;

    // =========================
    // PROJECT SCHEDULE TASK
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_schedule_task_id", nullable = false)
    private ProjectScheduleTask projectScheduleTask;

    private Integer sprintNumber;

    private String taskName;

    // =========================
    // ANALYSIS
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
    // DESIGN
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
    // CODING
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
    // SIT
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
    // OTHER ACTIVITY
    // =========================

    private Double otherActualTotal;

    private Double otherActualProjectManagement;

    private Double otherActualSupportGroup;

    private Double otherActualOthers;

    private Double otherPlannedTotal;

    private Double otherPlannedProjectManagement;

    private Double otherPlannedSupportGroup;

    private Double otherPlannedOthers;
}