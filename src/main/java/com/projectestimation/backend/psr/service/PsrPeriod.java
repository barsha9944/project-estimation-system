package com.projectestimation.backend.psr.service;

import java.time.LocalDate;

/** The immutable, schedule-derived identity of a PSR reporting period. */
public record PsrPeriod(int version, LocalDate startDate, LocalDate endDate) {
}
