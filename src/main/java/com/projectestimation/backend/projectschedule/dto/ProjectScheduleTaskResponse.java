package com.projectestimation.backend.projectschedule.dto;

import java.time.LocalDate;
import java.util.List;

public class ProjectScheduleTaskResponse {
	
	private Long id;
	
	private List<TaskBreakdownResponse> taskBreakdowns;

    private Integer sequence;

    private String taskName;

    private LocalDate plannedStartDate;

    private LocalDate plannedEndDate;

    private LocalDate actualStartDate;

    private LocalDate actualEndDate;

    private String predecessor;

    private String status;

    private Integer duration;
    
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<TaskBreakdownResponse> getTaskBreakdowns() {
		return taskBreakdowns;
	}

	public void setTaskBreakdowns(List<TaskBreakdownResponse> taskBreakdowns) {
		this.taskBreakdowns = taskBreakdowns;
	}

	public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(LocalDate plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public void setPlannedEndDate(LocalDate plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    public LocalDate getActualStartDate() {
        return actualStartDate;
    }

    public void setActualStartDate(LocalDate actualStartDate) {
        this.actualStartDate = actualStartDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public String getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(String predecessor) {
        this.predecessor = predecessor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}
    
    
}