package com.projectestimation.backend.projectmetrics.service;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.projectmetrics.calculator.ProjectMetricsCalculator;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;

@Service
public class ProjectMetricsService {

    private final ProjectMetricsCalculator calculator;

    public ProjectMetricsService(
            ProjectMetricsCalculator calculator) {

        this.calculator = calculator;
    }

    public ProjectMetricsResponse calculateMetrics(
            Long opportunityId) {

        return calculator.calculate(opportunityId);
    }
}