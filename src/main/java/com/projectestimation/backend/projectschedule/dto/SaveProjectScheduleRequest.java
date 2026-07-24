package com.projectestimation.backend.projectschedule.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public class SaveProjectScheduleRequest {

    @NotNull
    private LocalDate projectStartDate;

    @NotNull
    private Integer teamSize;

    @NotNull
    private Integer workingDaysPerWeek;

    @NotNull
    private Integer workingHoursPerDay;

    @NotNull
    private Integer bufferPercentage;

    private Integer durationDays;

    private Integer totalTasks;

    private Integer completedTasks;

    private Integer criticalTasks;

    private Double estimatedHours;
    
    private Integer duration;

    private LocalDate projectEndDate;
    
    private List<SaveProjectScheduleTaskRequest> tasks;

    public LocalDate getProjectStartDate() {
        return projectStartDate;
    }

    public void setProjectStartDate(LocalDate projectStartDate) {
        this.projectStartDate = projectStartDate;
    }

    public Integer getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(Integer teamSize) {
        this.teamSize = teamSize;
    }

    public Integer getWorkingDaysPerWeek() {
        return workingDaysPerWeek;
    }

    public void setWorkingDaysPerWeek(Integer workingDaysPerWeek) {
        this.workingDaysPerWeek = workingDaysPerWeek;
    }

    public Integer getWorkingHoursPerDay() {
        return workingHoursPerDay;
    }

    public void setWorkingHoursPerDay(Integer workingHoursPerDay) {
        this.workingHoursPerDay = workingHoursPerDay;
    }

    public Integer getBufferPercentage() {
        return bufferPercentage;
    }

    public void setBufferPercentage(Integer bufferPercentage) {
        this.bufferPercentage = bufferPercentage;
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

    

	public List<SaveProjectScheduleTaskRequest> getTasks() {
		return tasks;
	}

	public void setTasks(List<SaveProjectScheduleTaskRequest> tasks) {
		this.tasks = tasks;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public LocalDate getProjectEndDate() {
		return projectEndDate;
	}

	public void setProjectEndDate(LocalDate projectEndDate) {
		this.projectEndDate = projectEndDate;
	}
    
    
}