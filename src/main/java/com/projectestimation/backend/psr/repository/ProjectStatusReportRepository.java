package com.projectestimation.backend.psr.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectestimation.backend.psr.model.ProjectStatusReport;

public interface ProjectStatusReportRepository
        extends JpaRepository<ProjectStatusReport, Long> {

    Optional<ProjectStatusReport>
    findTopByOpportunityIdOrderByGeneratedAtDesc(
            Long opportunityId
    );

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM ProjectStatusReport p
        WHERE p.opportunity.id = :opportunityId
          AND p.generatedAt >= :from
    """)
    boolean existsRecentPsr(
            @Param("opportunityId") Long opportunityId,
            @Param("from") LocalDateTime from
    );
    
    long countByOpportunityId(Long opportunityId);
    
    List<ProjectStatusReport>
    findByOpportunityIdOrderByGeneratedAtAsc(
            Long opportunityId
    );
}