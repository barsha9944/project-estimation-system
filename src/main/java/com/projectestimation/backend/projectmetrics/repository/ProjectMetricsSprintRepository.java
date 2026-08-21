package com.projectestimation.backend.projectmetrics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectestimation.backend.projectmetrics.model.ProjectMetricsSprint;

public interface ProjectMetricsSprintRepository
        extends JpaRepository<ProjectMetricsSprint, Long> {
	
	@Modifying
    @Query("""
        DELETE FROM ProjectMetricsSprint s
        WHERE s.projectMetrics.id = :projectMetricsId
    """)
    int deleteByProjectMetricsId(
            @Param("projectMetricsId") Long projectMetricsId
    );
}