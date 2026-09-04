package com.projectestimation.backend.psr.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Calculates PSR reporting periods from the project schedule.
 *
 * Each PSR period contains up to 15 working days.
 * Saturday and Sunday are excluded.
 */
@Component
public class PsrPeriodCalculator {

    public static final int WORKING_DAYS_PER_PERIOD = 15;

    /**
     * Calculates all PSR periods between the project start and project end.
     *
     * Example:
     *
     * Working days 1-15   -> PSR #1
     * Working days 16-30  -> PSR #2
     * Working days 31-45  -> PSR #3
     *
     * The final period may contain fewer than 15 working days.
     */
    public List<PsrPeriod> calculatePeriods(
            LocalDate projectStartDate,
            LocalDate projectEndDate
    ) {

        if (projectStartDate == null) {
            throw new IllegalStateException(
                    "Project schedule must have a project start date"
            );
        }

        if (projectEndDate == null) {
            throw new IllegalStateException(
                    "Project schedule must have a project end date"
            );
        }

        LocalDate firstWorkingDay =
                nextWorkingDay(projectStartDate);

        LocalDate lastWorkingDay =
                previousWorkingDay(projectEndDate);

        if (firstWorkingDay.isAfter(lastWorkingDay)) {
            return List.of();
        }

        List<PsrPeriod> periods =
                new ArrayList<>();

        LocalDate periodStart =
                firstWorkingDay;

        int version = 1;

        while (!periodStart.isAfter(lastWorkingDay)) {

            LocalDate periodEnd =
                    addWorkingDays(
                            periodStart,
                            WORKING_DAYS_PER_PERIOD - 1
                    );

            if (periodEnd.isAfter(lastWorkingDay)) {
                periodEnd = lastWorkingDay;
            }

            periods.add(
                    new PsrPeriod(
                            version,
                            periodStart,
                            periodEnd
                    )
            );

            periodStart =
                    nextWorkingDay(
                            periodEnd.plusDays(1)
                    );

            version++;
        }

        return periods;
    }

    /**
     * Returns the PSR period containing the supplied date.
     */
    public PsrPeriod findPeriodForDate(
            List<PsrPeriod> periods,
            LocalDate date
    ) {

        if (periods == null || periods.isEmpty() || date == null) {
            return null;
        }

        for (PsrPeriod period : periods) {

            if (
                    !date.isBefore(period.startDate())
                    && !date.isAfter(period.endDate())
            ) {
                return period;
            }
        }

        return null;
    }

    /**
     * Adds the specified number of working days.
     *
     * addWorkingDays(start, 0) returns start itself.
     */
    private LocalDate addWorkingDays(
            LocalDate startDate,
            int workingDaysToAdd
    ) {

        LocalDate date = startDate;

        int added = 0;

        while (added < workingDaysToAdd) {

            date = date.plusDays(1);

            if (isWorkingDay(date)) {
                added++;
            }
        }

        return date;
    }

    private LocalDate nextWorkingDay(
            LocalDate date
    ) {

        LocalDate result = date;

        while (!isWorkingDay(result)) {
            result = result.plusDays(1);
        }

        return result;
    }

    private LocalDate previousWorkingDay(
            LocalDate date
    ) {

        LocalDate result = date;

        while (!isWorkingDay(result)) {
            result = result.minusDays(1);
        }

        return result;
    }

    private boolean isWorkingDay(
            LocalDate date
    ) {

        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}