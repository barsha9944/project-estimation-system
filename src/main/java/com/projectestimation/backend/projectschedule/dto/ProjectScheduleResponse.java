package com.projectestimation.backend.projectschedule.dto;

import java.util.List;

public class ProjectScheduleResponse {

    private Integer durationDays;

    private Integer totalTasks;

    private Integer completedTasks;

    private Integer criticalTasks;

    private Double estimatedHours;

    private List<ProjectScheduleTaskResponse> tasks;

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

    public List<ProjectScheduleTaskResponse> getTasks() {
        return tasks;
    }

    public void setTasks(List<ProjectScheduleTaskResponse> tasks) {
        this.tasks = tasks;
    }
}