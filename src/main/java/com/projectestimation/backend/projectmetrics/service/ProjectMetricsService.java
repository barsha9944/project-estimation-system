package com.projectestimation.backend.projectmetrics.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.projectmetrics.calculator.ProjectMetricsCalculator;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;

@Service
public class ProjectMetricsService {

    private final ProjectMetricsCalculator calculator;

    private final ProjectMetricsPersistenceService persistenceService;

    public ProjectMetricsService(
            ProjectMetricsCalculator calculator,
            ProjectMetricsPersistenceService persistenceService) {

        this.calculator = calculator;
        this.persistenceService = persistenceService;
    }

    @Transactional
    public ProjectMetricsResponse calculateMetrics(
            Long opportunityId) {

        ProjectMetricsResponse response =
                calculator.calculate(opportunityId);

        persistenceService.saveMetrics(
                opportunityId,
                response);

        return response;
    }
}