package com.projectestimation.backend.projectmetrics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    
    @Query("""
    	    SELECT o.opportunityName
    	    FROM ProjectMetrics pm
    	    JOIN pm.opportunity o
    	    WHERE pm.opportunity.id = :opportunityId
    	""")
    	Optional<String> findOpportunityNameByOpportunityId(
    	        @Param("opportunityId") Long opportunityId
    	);
    
//    @Modifying
//    @Query("""
//        DELETE FROM ProjectMetrics p
//        WHERE p.opportunity.id = :opportunityId
//    """)
//    void deleteByOpportunityId(
//            @Param("opportunityId") Long opportunityId
//    );
    
    @Modifying
    @Query("""
        DELETE FROM ProjectMetrics pm
        WHERE pm.opportunity.id = :opportunityId
    """)
    int deleteByOpportunityId(
            @Param("opportunityId") Long opportunityId
    );
}