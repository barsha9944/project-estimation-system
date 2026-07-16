package com.projectestimation.backend.projectschedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class GenerateProjectScheduleRequest {

    @NotNull
    private LocalDate projectStartDate;

    @NotNull
    @Min(1)
    private Integer teamSize;

    @NotNull
    @Min(1)
    @Max(7)
    private Integer workingDaysPerWeek;

    @NotNull
    @Min(1)
    @Max(24)
    private Integer workingHoursPerDay;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer bufferPercentage;
    
    @NotNull
    @Min(0)
    private Double estimatedHours;

    public Double getEstimatedHours() {
		return estimatedHours;
	}

	public void setEstimatedHours(Double estimatedHours) {
		this.estimatedHours = estimatedHours;
	}

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
}