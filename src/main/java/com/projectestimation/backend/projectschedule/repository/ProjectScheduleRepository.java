package com.projectestimation.backend.projectschedule.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectestimation.backend.projectschedule.model.ProjectSchedule;

public interface ProjectScheduleRepository
        extends JpaRepository<ProjectSchedule, Long> {

    Optional<ProjectSchedule> findByOpportunityId(Long opportunityId);
    
    @Query("""
    	    SELECT DISTINCT ps
    	    FROM ProjectSchedule ps
    	    LEFT JOIN FETCH ps.tasks
    	    WHERE ps.opportunity.id = :opportunityId
    	""")
    	Optional<ProjectSchedule> findByOpportunityIdWithTasks(
    	        @Param("opportunityId") Long opportunityId
    	);

    @Query("""
    	    SELECT DISTINCT ps
    	    FROM ProjectSchedule ps
    	    LEFT JOIN FETCH ps.tasks t
    	    LEFT JOIN FETCH t.taskBreakdowns
    	    WHERE ps.opportunity.id = :opportunityId
    	""")
    	Optional<ProjectSchedule> findByOpportunityIdWithTasksAndBreakdowns(
    	        @Param("opportunityId") Long opportunityId
    	);
    
    boolean existsByOpportunityId(Long opportunityId);
}