package com.projectestimation.backend.projectmetrics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectestimation.backend.projectmetrics.model.ProjectMetrics;

public interface ProjectMetricsRepository
        extends JpaRepository<ProjectMetrics, Long> {

    Optional<ProjectMetrics> findByOpportunityId(Long opportunityId);

    @Query("""
        SELECT DISTINCT pm
        FROM ProjectMetrics pm
        LEFT JOIN FETCH pm.sprints
        WHERE pm.opportunity.id = :opportunityId
    """)
    Optional<ProjectMetrics> findByOpportunityIdWithSprints(
            @Param("opportunityId") Long opportunityId
    );
    
    boolean existsByOpportunityId(Long opportunityId);
}