package com.projectestimation.backend.projectschedule.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GenerateProjectScheduleRequest {

	@NotNull
	private LocalDate projectStartDate;

	@NotNull
	private LocalDate projectEndDate;
	
	@JsonProperty("durationDays")
	private int duration;

	@NotNull
	@DecimalMin("0.01")
	private BigDecimal teamSize;
	
	@Min(1)
	private Integer sprintCount;

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

	public LocalDate getProjectEndDate() {
	    return projectEndDate;
	}

	public void setProjectEndDate(LocalDate projectEndDate) {
	    this.projectEndDate = projectEndDate;
	}
	
	public BigDecimal getTeamSize() {
	    return teamSize;
	}

	public void setTeamSize(BigDecimal teamSize) {
	    this.teamSize = teamSize;
	}
	
	public Integer getSprintCount() {
	    return sprintCount;
	}

	public void setSprintCount(Integer sprintCount) {
	    this.sprintCount = sprintCount;
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

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

}