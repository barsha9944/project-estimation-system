package com.projectestimation.backend.psr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class PsrPeriodCalculatorTest {

    private final PsrPeriodCalculator calculator =
            new PsrPeriodCalculator();

    @Test
    void createsFifteenWorkingDayPeriodsAndSkipsWeekends() {

        LocalDate projectStart =
                LocalDate.of(2026, 9, 7); // Monday

        LocalDate projectEnd =
                LocalDate.of(2026, 11, 6); // Friday

        List<PsrPeriod> periods =
                calculator.calculatePeriods(
                        projectStart,
                        projectEnd);

        assertEquals(3, periods.size());

        PsrPeriod firstPeriod = periods.get(0);

        assertEquals(1, firstPeriod.version());
        assertEquals(
                LocalDate.of(2026, 9, 7),
                firstPeriod.startDate());
        assertEquals(
                LocalDate.of(2026, 9, 25),
                firstPeriod.endDate());

        PsrPeriod secondPeriod = periods.get(1);

        assertEquals(2, secondPeriod.version());
        assertEquals(
                LocalDate.of(2026, 9, 28),
                secondPeriod.startDate());
        assertEquals(
                LocalDate.of(2026, 10, 16),
                secondPeriod.endDate());

        PsrPeriod thirdPeriod = periods.get(2);

        assertEquals(3, thirdPeriod.version());
        assertEquals(
                LocalDate.of(2026, 10, 19),
                thirdPeriod.startDate());
        assertEquals(
                LocalDate.of(2026, 11, 6),
                thirdPeriod.endDate());
    }

    @Test
    void normalizesWeekendProjectStartToFollowingMonday() {

        LocalDate projectStart =
                LocalDate.of(2026, 9, 5); // Saturday

        LocalDate projectEnd =
                LocalDate.of(2026, 9, 25);

        List<PsrPeriod> periods =
                calculator.calculatePeriods(
                        projectStart,
                        projectEnd);

        assertEquals(1, periods.size());

        PsrPeriod firstPeriod = periods.get(0);

        assertEquals(1, firstPeriod.version());
        assertEquals(
                LocalDate.of(2026, 9, 7),
                firstPeriod.startDate());
        assertEquals(
                LocalDate.of(2026, 9, 25),
                firstPeriod.endDate());
    }

    @Test
    void findsCorrectPeriodForActivityDate() {

        List<PsrPeriod> periods =
                calculator.calculatePeriods(
                        LocalDate.of(2026, 9, 7),
                        LocalDate.of(2026, 11, 6));

        PsrPeriod period =
                calculator.findPeriodForDate(
                        periods,
                        LocalDate.of(2026, 10, 5));

        assertEquals(2, period.version());
        assertEquals(
                LocalDate.of(2026, 9, 28),
                period.startDate());
        assertEquals(
                LocalDate.of(2026, 10, 16),
                period.endDate());
    }
}