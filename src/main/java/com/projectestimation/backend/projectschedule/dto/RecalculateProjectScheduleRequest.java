package com.projectestimation.backend.projectschedule.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecalculateProjectScheduleRequest {

	private Integer editedSequence;
	
	private String editedField;

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
    
    private List<SaveProjectScheduleTaskRequest> tasks;

}