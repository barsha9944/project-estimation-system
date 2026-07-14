package com.projectestimation.backend.projectschedule.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_analysis_id", nullable = false)
    private EstimationAnalysis estimationAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_by")
    private User savedBy;

    private LocalDate projectStartDate;

    private Integer teamSize;

    private Integer workingDaysPerWeek;

    private Integer workingHoursPerDay;

    private Integer bufferPercentage;

    private Integer durationDays;

    private Integer totalTasks;

    private Integer completedTasks;

    private Integer criticalTasks;

    private Double estimatedHours;

    @OneToMany(
            mappedBy = "projectSchedule",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProjectScheduleTask> tasks;

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

    // Generate Getters & Setters
}