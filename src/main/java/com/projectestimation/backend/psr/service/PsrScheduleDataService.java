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
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleTaskBreakdownRepository;
import com.projectestimation.backend.psr.dto.PsrActivityDto;
import com.projectestimation.backend.psr.dto.PsrContentDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PsrScheduleDataService {

    private final ProjectScheduleRepository projectScheduleRepository;

    private final ProjectScheduleTaskBreakdownRepository
            projectScheduleTaskBreakdownRepository;

    private final PsrPeriodCalculator psrPeriodCalculator;

    @Transactional(readOnly = true)
    public PsrContentDto buildPsrContent(
            Long opportunityId,
            PsrPeriod period
    ) {

        ProjectSchedule schedule =
                projectScheduleRepository
                        .findByOpportunityIdWithTasks(opportunityId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Project schedule not found for opportunity: "
                                                + opportunityId
                                )
                        );

        /*
         * ============================================================
         * ACTIVITIES DURING THIS PSR PERIOD
         * ============================================================
         *
         * The Project Schedule is the source of truth.
         *
         * Every activity whose schedule dates overlap this PSR period
         * is included, regardless of status.
         */
        List<PsrActivityDto> activitiesPerformed =
                new ArrayList<>();

        /*
         * ============================================================
         * NEXT 15 WORKING DAYS
         * ============================================================
         */
        List<PsrActivityDto> nextWeekPlannedActivities =
                new ArrayList<>();

        /*
         * Calculate all PSR periods for the project.
         */
        List<PsrPeriod> periods =
                psrPeriodCalculator.calculatePeriods(
                        schedule.getProjectStartDate(),
                        schedule.getProjectEndDate()
                );

        /*
         * Find the next PSR period.
         */
        PsrPeriod nextPeriod = null;

        for (PsrPeriod candidate : periods) {

            if (candidate.version() == period.version() + 1) {

                nextPeriod = candidate;
                break;
            }
        }

        /*
         * ============================================================
         * LOAD ALL BREAKDOWNS FOR THIS OPPORTUNITY
         * ============================================================
         *
         * IMPORTANT:
         *
         * We do NOT access:
         *
         *     schedule.getTasks()
         *         .getTaskBreakdowns()
         *
         * for PSR data.
         *
         * The breakdown repository already has a query which fetches
         * every breakdown together with its parent task.
         *
         * This avoids fetching two Hibernate List collections together.
         */
        List<ProjectScheduleTaskBreakdown> breakdowns =
                projectScheduleTaskBreakdownRepository
                        .findByOpportunityIdOrdered(
                                opportunityId
                        );
        
        projectScheduleTaskBreakdownRepository.flush();

        System.out.println("PSR DATA opportunityId = " + opportunityId);
        System.out.println("PSR DATA breakdown count = " + breakdowns.size());

        for (ProjectScheduleTaskBreakdown breakdown : breakdowns) {
            System.out.println(
                "PSR DATA -> breakdownId=" + breakdown.getId()
                + ", taskId=" +
                    (breakdown.getProjectScheduleTask() != null
                        ? breakdown.getProjectScheduleTask().getId()
                        : null)
                + ", status=" + breakdown.getStatus()
                + ", progress=" + breakdown.getProgress()
            );
        }
        /*
         * ============================================================
         * PROCESS EVERY SCHEDULE BREAKDOWN
         * ============================================================
         */
        for (ProjectScheduleTaskBreakdown breakdown :
                breakdowns) {

            ProjectScheduleTask task =
                    breakdown.getProjectScheduleTask();

            if (task == null) {
                continue;
            }

            /*
             * If the breakdown has no status, expose it as Not Started.
             */
            String status =
                    breakdown.getStatus() != null
                            ? breakdown.getStatus()
                            : "Not Started";

            /*
             * Create the PSR activity from the actual schedule data.
             */
            PsrActivityDto activity =
                    new PsrActivityDto(
                            breakdown.getId(),
                            task.getSequence(),
                            task.getTaskName(),
                            breakdown.getActivityName(),
                            status,
                            breakdown.getProgress(),
                            breakdown.getDuration(),
                            toString(
                                    breakdown.getPlannedStartDate()
                            ),
                            toString(
                                    breakdown.getPlannedEndDate()
                            ),
                            toString(
                                    breakdown.getActualEndDate()
                            )
                    );
            
            System.out.println(
            	    "PSR ACTIVITY -> breakdownId=" + breakdown.getId()
            	    + ", activity=" + breakdown.getActivityName()
            	    + ", status=" + breakdown.getStatus()
            	    + ", progress=" + breakdown.getProgress()
            	);

            /*
             * ========================================================
             * CURRENT PSR PERIOD
             * ========================================================
             *
             * Include the activity when its schedule dates overlap
             * the current PSR reporting period.
             *
             * Status does NOT determine inclusion.
             */
            if (isActivityInPeriod(
                    breakdown,
                    period
            )) {

                activitiesPerformed.add(
                        activity
                );
            }

            /*
             * ========================================================
             * NEXT PSR PERIOD
             * ========================================================
             *
             * Include all activities belonging to the next
             * 15-working-day reporting period.
             *
             * We do NOT require "Not Started".
             */
            if (nextPeriod != null
                    && isActivityInPeriod(
                            breakdown,
                            nextPeriod
                    )) {

                nextWeekPlannedActivities.add(
                        activity
                );
            }
        }

        /*
         * ============================================================
         * RETURN PSR CONTENT
         * ============================================================
         */
        return new PsrContentDto(
                activitiesPerformed,
                nextWeekPlannedActivities,
                getRiskStatus(),
                "",
                "",
                period.startDate().toString(),
                period.endDate().toString(),
                LocalDate.now().toString()
        );
    }

    /*
     * ================================================================
     * CHECK WHETHER ACTIVITY BELONGS TO A PSR PERIOD
     * ================================================================
     *
     * Planned dates are preferred because the Project Schedule is
     * the source of truth.
     *
     * Actual dates are used only when planned dates are unavailable.
     */
    private boolean isActivityInPeriod(
            ProjectScheduleTaskBreakdown breakdown,
            PsrPeriod period
    ) {

        if (period == null) {
            return false;
        }

        LocalDate plannedStart =
                breakdown.getPlannedStartDate();

        LocalDate plannedEnd =
                breakdown.getPlannedEndDate();

        LocalDate actualStart =
                breakdown.getActualStartDate();

        LocalDate actualEnd =
                breakdown.getActualEndDate();

        /*
         * Use planned schedule dates whenever available.
         */
        if (plannedStart != null || plannedEnd != null) {

            return datesOverlap(
                    plannedStart,
                    plannedEnd,
                    period.startDate(),
                    period.endDate()
            );
        }

        /*
         * Fallback to actual dates.
         */
        if (actualStart != null || actualEnd != null) {

            return datesOverlap(
                    actualStart,
                    actualEnd,
                    period.startDate(),
                    period.endDate()
            );
        }

        return false;
    }

    /*
     * ================================================================
     * DATE OVERLAP
     * ================================================================
     */
    private boolean datesOverlap(
            LocalDate activityStart,
            LocalDate activityEnd,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {

        LocalDate start =
                activityStart != null
                        ? activityStart
                        : activityEnd;

        LocalDate end =
                activityEnd != null
                        ? activityEnd
                        : activityStart;

        if (start == null || end == null) {
            return false;
        }

        return !end.isBefore(periodStart)
                && !start.isAfter(periodEnd);
    }

    /*
     * ================================================================
     * RISK STATUS
     * ================================================================
     *
     * The fixed Risk Status table is already supplied by the
     * Gemini prompt, so this remains null.
     */
    private String getRiskStatus() {
        return null;
    }

    /*
     * ================================================================
     * DATE → STRING
     * ================================================================
     */
    private String toString(
            LocalDate date
    ) {

        return date != null
                ? date.toString()
                : null;
    }

    /*
     * ================================================================
     * SCHEDULE ACCESS FOR PSR
     * ================================================================
     */
    public ProjectSchedule getScheduleForPsr(
            Long opportunityId
    ) {

        return projectScheduleRepository
                .findByOpportunityIdWithTasks(opportunityId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Project schedule not found for opportunity: "
                                        + opportunityId
                        )
                );
    }
}