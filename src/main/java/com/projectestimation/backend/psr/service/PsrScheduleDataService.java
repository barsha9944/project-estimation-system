package com.projectestimation.backend.psr.service;

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

        boolean reachedCurrentPoint = false;

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

                    if (!reachedCurrentPoint
                            && isCompleted(status)) {

                    	nextWeekPlannedActivities.add(
                    		    new PsrActivityDto(
                    		        task.getSequence(),
                    		        task.getTaskName(),
                    		        breakdown.getActivityName(),
                    		        breakdown.getStatus(),
                    		        null,
                    		        breakdown.getPlannedEndDate() != null
                    		                ? breakdown.getPlannedEndDate().toString()
                    		                : null,
                    		        breakdown.getActualEndDate() != null
                    		                ? breakdown.getActualEndDate().toString()
                    		                : null
                    		    )
                    		);

                    } else {

                        reachedCurrentPoint = true;

                        nextWeekPlannedActivities.add(
                        	    new PsrActivityDto(
                        	        task.getSequence(),
                        	        task.getTaskName(),
                        	        breakdown.getActivityName(),
                        	        breakdown.getStatus(),
                        	        null,
                        	        breakdown.getPlannedEndDate() != null
                        	                ? breakdown.getPlannedEndDate().toString()
                        	                : null,
                        	        breakdown.getActualEndDate() != null
                        	                ? breakdown.getActualEndDate().toString()
                        	                : null
                        	    )
                        	);
                    }
                }
            }
        }

        return new PsrContentDto(
                activitiesPerformed,
                nextWeekPlannedActivities,
                getRiskStatus(),
                "",
                ""
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

    private String getRiskStatus() {

        // Fixed value will be supplied from the PSR template.
        return null;
    }
}