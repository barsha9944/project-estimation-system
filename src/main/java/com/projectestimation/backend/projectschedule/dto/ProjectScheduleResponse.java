package com.projectestimation.backend.projectschedule.dto;

import java.time.LocalDate;
import java.util.List;

public class ProjectScheduleResponse {
	
	private Long id;

    private Integer durationDays;

    private Integer totalTasks;

    private Integer completedTasks;

    private Integer criticalTasks;

    private Double estimatedHours;
    
    private Integer bufferPercentage;
    
    private Integer teamSize;
    
    private Integer WorkingDaysPerWeek;
    
    private Integer workingHoursPerDays;
    
    private LocalDate projectStartDate;

    private List<ProjectScheduleTaskResponse> tasks;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Integer getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(Integer completedTasks) {
        this.completedTasks = completedTasks;
    }

    public Integer getCriticalTasks() {
        return criticalTasks;
    }

    public void setCriticalTasks(Integer criticalTasks) {
        this.criticalTasks = criticalTasks;
    }

    public Double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    
    public Integer getBufferPercentage() {
		return bufferPercentage;
	}

	public void setBufferPercentage(Integer bufferPercentage) {
		this.bufferPercentage = bufferPercentage;
	}

	public Integer getTeamSize() {
		return teamSize;
	}

	public void setTeamSize(Integer teamSize) {
		this.teamSize = teamSize;
	}

	public Integer getWorkingDaysPerWeek() {
		return WorkingDaysPerWeek;
	}

	public void setWorkingDaysPerWeek(Integer workingDaysPerWeek) {
		WorkingDaysPerWeek = workingDaysPerWeek;
	}

	public Integer getWorkingHoursPerDays() {
		return workingHoursPerDays;
	}

	public void setWorkingHoursPerDays(Integer workingHoursPerDays) {
		this.workingHoursPerDays = workingHoursPerDays;
	}

	public LocalDate getProjectStartDate() {
		return projectStartDate;
	}

	public void setProjectStartDate(LocalDate projectStartDate) {
		this.projectStartDate = projectStartDate;
	}

	public List<ProjectScheduleTaskResponse> getTasks() {
        return tasks;
    }

    public void setTasks(List<ProjectScheduleTaskResponse> tasks) {
        this.tasks = tasks;
    }
}