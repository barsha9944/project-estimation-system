
package com.projectestimation.backend.car.ai;

import com.projectestimation.backend.car.model.CarAnalysis;
import org.springframework.stereotype.Component;

@Component
public class CarPromptBuilder {

    public String buildRootCausePrompt(CarAnalysis car) {
        return """
            You are an experienced CMMI process improvement and causal analysis expert.
            Prepare a complete CAR (Causal Analysis and Resolution) analysis using only
            the information supplied below. Do not invent measurements, dates, people,
            evidence, or facts that are not present in the input.

            CAR TYPE: %s
            TRIGGER: %s
            PROBLEM / OPPORTUNITY: %s
            PERFORMANCE DETAILS: %s
            SELECTED TECHNIQUE: %s

            ANALYSIS REQUIREMENTS:
            1. Support both NEGATIVE CAR (problem/defect) and POSITIVE CAR (successful
               outcome/opportunity to replicate).
            2. For NEGATIVE CAR, distinguish symptoms from verified root causes. Clearly
               mark assumptions or information that needs confirmation.
            3. For POSITIVE CAR, identify the contributing success factors and explain
               how the successful practice can be repeated or institutionalized.
            4. Provide a detailed, logically connected 5 Whys analysis. Number each why
               and answer in fiveWhysDetails.
            5. Provide a Fishbone analysis using People, Process, Technology,
               Measurement, Environment, and Management categories. Do not force a
               category when there is no supporting information.
            6. Provide Pareto analysis only when numerical frequency/impact data exists.
               Otherwise state that sufficient numerical data was not provided.
            7. Provide histogram analysis only when suitable numerical data exists.
               Otherwise state that sufficient numerical data was not provided.
            8. Suggest practical corrective/preventive actions for NEGATIVE CAR, and
               process-improvement/preventive actions for POSITIVE CAR.
            9. Do not invent action owners or target dates. Use null when unknown.
            10. targetCompletionDate, when supported by input, must use yyyy-MM-dd.
            11. Priority must be one of LOW, MEDIUM, HIGH, CRITICAL.
            12. Include actionable lessons learned and a concise action summary.

            Return ONLY one valid JSON object. Do not use Markdown fences, comments,
            or text outside the JSON. Use this exact structure:
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
            Include at least one meaningful action only when the supplied information
            supports a practical recommendation. If no action is justified, return
            an empty actions array.
            """.formatted(
                value(car.getCarType()),
                value(car.getTriggerForCar()),
                value(car.getProblemStatement()),
                value(car.getPerformanceMonitoringDetails()),
                value(car.getCausalAnalysisTechnique())
        );
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
}
