
package com.projectestimation.backend.car.ai;

import com.projectestimation.backend.car.model.CarAnalysis;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import org.springframework.stereotype.Component;

@Component
public class CarPromptBuilder {

    public String buildRootCausePrompt(
            CarAnalysis car,
            ProjectMetricsResponse projectMetrics) {

        String sprintMetrics = buildSprintWiseMetrics(projectMetrics);

        return """
            You are an experienced CMMI process improvement and causal analysis expert.

            Prepare a complete CAR (Causal Analysis and Resolution) analysis using
            the CAR information and sprint-wise Project Summary Metrics supplied below.

            Use only the supplied information. Do not invent measurements, dates,
            people, evidence, or facts that are not present in the input.

            ==============================
            CAR INFORMATION
            ==============================

            CAR TYPE: %s
            TRIGGER: %s
            PROBLEM / OPPORTUNITY: %s
            PERFORMANCE DETAILS: %s
            SELECTED TECHNIQUE: %s

            ==============================
            SPRINT-WISE PROJECT SUMMARY METRICS
            ==============================

            %s

            ==============================
            SPRINT-WISE ANALYSIS REQUIREMENTS
            ==============================

            1. Analyse each sprint separately using its supplied Summary Metrics.

            2. For every sprint, identify relevant performance observations,
               deviations, and improvement opportunities supported by the metrics.

            3. Compare planned and actual effort, effort variance, planned and
               actual duration, schedule variance, productivity, review
               effectiveness, and testing effectiveness wherever available.

            4. Identify recurring patterns across sprints, including repeated
               effort or schedule deviations and changes in productivity,
               review effectiveness, and testing effectiveness.

            5. Provide an overall comparison across all available sprints.
               Highlight improvements, deteriorations, recurring issues,
               and significant variations.

            6. Clearly distinguish measured facts from possible explanations.
               Do not claim that a metric deviation proves a particular root cause.

            7. IMPORTANT: If sprint values are allocated or derived from
               project-level calculations rather than independently measured
               sprint actuals, explicitly identify this limitation. Do not
               present allocated values as independently collected sprint data.

            8. Do not calculate unsupported totals or averages. Only calculate
               comparisons when the supplied data supports them.

            ==============================
            ROOT CAUSE ANALYSIS REQUIREMENTS
            ==============================

            1. Support both NEGATIVE CAR (problem/defect) and POSITIVE CAR
               (successful outcome/opportunity to replicate).

            2. For NEGATIVE CAR, distinguish symptoms from verified root causes.
               Clearly mark assumptions or information that needs confirmation.

            3. For POSITIVE CAR, identify contributing success factors and explain
               how successful practices can be repeated or institutionalized.

            4. Provide a detailed, logically connected 5 Whys analysis.
               Number each why and answer in fiveWhysDetails.

            5. Provide a Fishbone analysis using People, Process, Technology,
               Measurement, Environment, and Management categories.
               Do not force a category when there is no supporting information.

            6. Provide Pareto analysis only when numerical frequency or impact
               data exists. Otherwise state that sufficient numerical data
               was not provided.

            7. Provide histogram analysis only when suitable numerical data
               exists. Otherwise state that sufficient numerical data
               was not provided.

            8. Suggest practical corrective/preventive actions for NEGATIVE CAR,
               and process-improvement/preventive actions for POSITIVE CAR.

            9. Actions should address the findings from the sprint-wise analysis
               and overall comparison wherever justified.

            10. Do not invent action owners or target dates. Use null when unknown.

            11. targetCompletionDate, when supported by input, must use yyyy-MM-dd.

            12. Priority must be one of LOW, MEDIUM, HIGH, CRITICAL.

            13. Include actionable lessons learned and a concise action summary.

            ==============================
            OUTPUT REQUIREMENTS
            ==============================

            Include the sprint-wise findings and overall comparison inside
            rootCauseSummary and/or the relevant analysis fields.

            Return ONLY one valid JSON object. Do not use Markdown fences,
            comments, or text outside the JSON.

            Use this exact structure:

            {
              "rootCauseSummary": "",
              "fiveWhysDetails": "",
              "fishboneAnalysis": "",
              "paretoAnalysis": "",
              "histogramAnalysis": "",
              "actionProposedSummary": "",
              "lessonsLearned": "",
              "actions": [
                {
                  "actionCategory": "CORRECTIVE",
                  "actionDescription": "",
                  "assignedTo": null,
                  "priority": "MEDIUM",
                  "targetCompletionDate": null
                }
              ]
            }

            For POSITIVE CAR, actionCategory must be PROCESS_IMPROVEMENT or PREVENTIVE.

            For NEGATIVE CAR, actionCategory must be CORRECTIVE or PREVENTIVE.

            Include at least one meaningful action only when the supplied
            information supports a practical recommendation.

            If no action is justified, return an empty actions array.
            """.formatted(
                value(car.getCarType()),
                value(car.getTriggerForCar()),
                value(car.getProblemStatement()),
                value(car.getPerformanceMonitoringDetails()),
                value(car.getCausalAnalysisTechnique()),
                sprintMetrics
        );
    }

    private String buildSprintWiseMetrics(
            ProjectMetricsResponse projectMetrics) {

        if (projectMetrics == null
                || projectMetrics.getSprints() == null
                || projectMetrics.getSprints().isEmpty()) {

            return "No sprint-wise metrics available.";
        }

        StringBuilder result = new StringBuilder();

        projectMetrics.getSprints().forEach(sprint -> {

            result.append("\n--------------------------------\n");
            result.append("SPRINT: ")
                    .append(sprint.getSprintNumber())
                    .append("\n");

            if (sprint.getTaskName() != null) {
                result.append("TASK NAME: ")
                        .append(sprint.getTaskName())
                        .append("\n");
            }

            if (sprint.getSummary() == null) {
                result.append("Summary Metrics: Not available.\n");
                return;
            }

            var summary = sprint.getSummary();

            result.append("Project Name: ")
                    .append(value(summary.getProjectName())).append("\n");

            result.append("Release Number: ")
                    .append(value(summary.getReleaseNo())).append("\n");

            result.append("Original Size: ")
                    .append(value(summary.getOriginalSize())).append("\n");

            result.append("Actual Size: ")
                    .append(value(summary.getActualSize())).append("\n");

            result.append("Size Variance: ")
                    .append(value(summary.getSizeVariance())).append("\n");

            result.append("Total Planned Effort Without PM: ")
                    .append(value(summary.getTotalPlannedEffortWithoutPm())).append("\n");

            result.append("Total Planned Effort: ")
                    .append(value(summary.getTotalPlannedEffort())).append("\n");

            result.append("Total Actual Effort Without PM: ")
                    .append(value(summary.getTotalActualEffortWithoutPm())).append("\n");

            result.append("Total Actual Effort: ")
                    .append(value(summary.getTotalActualEffort())).append("\n");

            result.append("Effort Variance: ")
                    .append(value(summary.getEffortVariance())).append("\n");

            result.append("Planned Duration: ")
                    .append(value(summary.getPlannedDuration())).append("\n");

            result.append("Actual Duration: ")
                    .append(value(summary.getActualDuration())).append("\n");

            result.append("Schedule Variance: ")
                    .append(value(summary.getScheduleVariance())).append("\n");

            result.append("Actual Overall Productivity: ")
                    .append(value(summary.getActualOverallProductivity())).append("\n");

            result.append("Review Effectiveness: ")
                    .append(value(summary.getReviewEffectiveness())).append("\n");

            result.append("Testing Effectiveness: ")
                    .append(value(summary.getTestingEffectiveness())).append("\n");
        });

        return result.toString();
    }

    private String value(Object value) {
        return value == null ? "Not available" : String.valueOf(value);
    }

    private String value(String value) {
        return value == null || value.isBlank()
                ? "Not provided"
                : value;
    }
}
