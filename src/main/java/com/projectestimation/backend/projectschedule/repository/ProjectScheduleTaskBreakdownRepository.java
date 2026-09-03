package com.projectestimation.backend.projectschedule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;

public interface ProjectScheduleTaskBreakdownRepository
        extends JpaRepository<ProjectScheduleTaskBreakdown, Long> {

    List<ProjectScheduleTaskBreakdown> findByProjectScheduleTaskProjectScheduleOpportunityId(
            Long opportunityId);
    
    @Query("""
    	    SELECT b
    	    FROM ProjectScheduleTaskBreakdown b
    	    JOIN FETCH b.projectScheduleTask t
    	    WHERE t.projectSchedule.opportunity.id = :opportunityId
    	    ORDER BY t.sequence ASC, b.id ASC
    	""")
    	List<ProjectScheduleTaskBreakdown> findByOpportunityIdOrdered(
    	        @Param("opportunityId") Long opportunityId
    	);
}