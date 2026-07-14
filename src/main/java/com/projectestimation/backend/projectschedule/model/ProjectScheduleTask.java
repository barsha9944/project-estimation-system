package com.projectestimation.backend.projectschedule.model;

import java.time.LocalDate;

import jakarta.persistence.*;
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

    // Generate Getters & Setters

}