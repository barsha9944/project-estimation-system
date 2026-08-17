package com.projectestimation.backend.projectmetrics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.projectmetrics.model.ProjectMetrics;

public interface ProjectMetricsRepository
        extends JpaRepository<ProjectMetrics, Long> {

    Optional<ProjectMetrics> findByOpportunity_Id(Long opportunityId);
}