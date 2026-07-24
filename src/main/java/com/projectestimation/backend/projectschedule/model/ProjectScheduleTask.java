package com.projectestimation.backend.projectschedule.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_schedule_task")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectScheduleTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_schedule_id")
    private ProjectSchedule projectSchedule;

    private Integer sequence;

    private String taskName;

    private LocalDate plannedStartDate;

    private LocalDate plannedEndDate;

    private LocalDate actualStartDate;

    private LocalDate actualEndDate;
    
    private Integer duration;

    private String predecessor;

    private String status;
    
    @OneToMany(
            mappedBy = "projectScheduleTask",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProjectScheduleTaskBreakdown> taskBreakdowns;

    // Generate Getters & Setters

}