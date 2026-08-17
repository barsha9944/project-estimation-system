package com.projectestimation.backend.projectmetrics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.projectmetrics.model.ProjectMetricsSprint;

public interface ProjectMetricsSprintRepository
        extends JpaRepository<ProjectMetricsSprint, Long> {
}