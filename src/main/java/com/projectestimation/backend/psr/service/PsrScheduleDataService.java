package com.projectestimation.backend.psr.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.psr.dto.PsrActivityDto;
import com.projectestimation.backend.psr.dto.PsrContentDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PsrScheduleDataService {

    private final ProjectScheduleRepository projectScheduleRepository;

    @Transactional(readOnly = true)
	public PsrContentDto buildPsrContent(Long opportunityId) {
	
	    ProjectSchedule schedule =
	            projectScheduleRepository
	            .findByOpportunityIdWithTasks(
	                            opportunityId
	                    )
	                    .orElseThrow(() ->
	                            new IllegalStateException(
	                                    "Project schedule not found for opportunity: "
	                                            + opportunityId
	                            )
	                    );
	
	    List<PsrActivityDto> activitiesPerformed =
	            new ArrayList<>();
	
	    List<PsrActivityDto> nextWeekPlannedActivities =
	            new ArrayList<>();
	
	    if (schedule.getTasks() != null) {
	
	        for (ProjectScheduleTask task : schedule.getTasks()) {
	
	            if (task.getTaskBreakdowns() == null) {
	                continue;
	            }
	
	            for (ProjectScheduleTaskBreakdown breakdown :
	                    task.getTaskBreakdowns()) {
	
	                String status =
	                        breakdown.getStatus() != null
	                                ? breakdown.getStatus()
	                                : "Not Started";
	
	                PsrActivityDto activity =
	                        new PsrActivityDto(
	                                breakdown.getId(),
	                                task.getSequence(),
	                                task.getTaskName(),
	                                breakdown.getActivityName(),
	                                status,
	                                breakdown.getProgress(),
	                                breakdown.getDuration(),
	                                breakdown.getPlannedStartDate() != null
	                                        ? breakdown.getPlannedStartDate().toString()
	                                        : null,
	                                breakdown.getPlannedEndDate() != null
	                                        ? breakdown.getPlannedEndDate().toString()
	                                        : null,
	                                breakdown.getActualEndDate() != null
	                                        ? breakdown.getActualEndDate().toString()
	                                        : null
	                        );
	
	             // ============================================
	             // ACTIVITIES DURING THE PERIOD
	             // Completed + In Progress
	             // ============================================

	             if (isCompleted(status)
	                     || isInProgress(status)) {

	                 activitiesPerformed.add(activity);

	             }

	             // ============================================
	             // NEXT WEEK PLANNED ACTIVITIES
	             // Not Started
	             // ============================================

	             else if (isWithinNext15Days(breakdown.getPlannedStartDate())) {

	            	    nextWeekPlannedActivities.add(activity);

	            	}
	            }
	        }
	    }
	
	    return new PsrContentDto(
	            activitiesPerformed,
	            nextWeekPlannedActivities,
	            getRiskStatus(),
	            "",
	            "",
	            schedule.getProjectStartDate() != null
	                    ? schedule.getProjectStartDate().toString()
	                    : null,
	            schedule.getProjectEndDate() != null
	                    ? schedule.getProjectEndDate().toString()
	                    : null,
	            LocalDate.now().toString()
	    );
	}

    private boolean isCompleted(String status) {

        return status != null
                && (
                    status.equalsIgnoreCase("Completed")
                    || status.equalsIgnoreCase("Complete")
                    || status.equalsIgnoreCase("Done")
                );
    }

    private boolean isInProgress(String status) {

        return status != null
                && (
                    status.equalsIgnoreCase("In Progress")
                    || status.equalsIgnoreCase("In-Progress")
                    || status.equalsIgnoreCase("InProgress")
                );
    }
    
    private String getRiskStatus() {

        // Fixed value will be supplied from the PSR template.
        return null;
    }
    
    private boolean isWithinNext15Days(LocalDate plannedStartDate) {

        if (plannedStartDate == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(15);

        return !plannedStartDate.isBefore(today)
                && !plannedStartDate.isAfter(endDate);
    }
    
    public ProjectSchedule getScheduleForPsr(Long opportunityId) {

        return projectScheduleRepository
        		.findByOpportunityIdWithTasks(
                        opportunityId
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Project schedule not found for opportunity: "
                                        + opportunityId
                        )
                );
    }
}