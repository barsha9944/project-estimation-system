
package com.projectestimation.backend.car.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.car.ai.GeminiCarOrchestrator;
import com.projectestimation.backend.car.dto.CarActionDto;
import com.projectestimation.backend.car.dto.CarResponseDto;
import com.projectestimation.backend.car.model.CarAnalysis;
import com.projectestimation.backend.car.repository.CarAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import com.projectestimation.backend.projectmetrics.service.ProjectMetricsService;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CarRootCauseAnalysisService {

    private final CarService carService;
    private final CarAnalysisRepository carAnalysisRepository;
    private final CarActionPlanService actionPlanService;
    private final GeminiCarOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    
    private final OpportunityRepository opportunityRepository;
    
    private final ProjectMetricsService projectMetricsService;
    
    public CarResponseDto generateAutomatedCar(Long opportunityId) {

        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new IllegalStateException(
                        "Opportunity not found: " + opportunityId));

        // Prevent accidental duplicate CAR records.
        List<CarAnalysis> existingCars =
                carAnalysisRepository
                        .findByOpportunityIdOrderByCreatedAtDesc(opportunityId);

        if (!existingCars.isEmpty()) {
            throw new IllegalStateException(
                    "A CAR already exists for this opportunity. "
                    + "Use the existing CAR instead of generating a duplicate.");
        }

        try {
            ProjectMetricsResponse projectMetrics =
                    projectMetricsService.getMetrics(opportunityId);

            if (projectMetrics == null
                    || projectMetrics.getSprints() == null
                    || projectMetrics.getSprints().isEmpty()) {

                throw new IllegalStateException(
                        "Sprint-wise Project Metrics are not available. "
                        + "Please calculate Project Metrics before generating CAR.");
            }

            boolean hasSprintSummary = projectMetrics.getSprints()
                    .stream()
                    .anyMatch(sprint -> sprint.getSummary() != null);

            if (!hasSprintSummary) {
                throw new IllegalStateException(
                        "Sprint-wise Summary Metrics are not available.");
            }

            // Ask Gemini to generate the complete CAR information.
            String response = orchestrator.generateAutomatedCar(
                    opportunity,
                    projectMetrics);

            JsonNode json = objectMapper.readTree(normalizeJson(response));

            if (json == null || !json.isObject()) {
                throw new IllegalStateException(
                        "CAR AI response is not a JSON object.");
            }

            String carType = text(json, "carType");

            if (carType == null
                    || (!carType.equalsIgnoreCase("POSITIVE")
                    && !carType.equalsIgnoreCase("NEGATIVE"))) {

                throw new IllegalStateException(
                        "Gemini returned an invalid CAR type.");
            }

            // Build the CAR only after Gemini has returned valid JSON.
            CarAnalysis car = CarAnalysis.builder()
                    .opportunity(opportunity)
                    .carType(carType.trim().toUpperCase(Locale.ROOT))
                    .triggerForCar(text(json, "triggerForCar"))
                    .problemStatement(text(json, "problemStatement"))
                    .originatorName(text(json, "originatorName"))
                    .performanceMonitoringDetails(
                            text(json, "performanceMonitoringDetails"))
                    .causalAnalysisTechnique(
                            text(json, "causalAnalysisTechnique"))
                    .rootCauseSummary(text(json, "rootCauseSummary"))
                    .fiveWhysDetails(text(json, "fiveWhysDetails"))
                    .fishboneAnalysis(text(json, "fishboneAnalysis"))
                    .paretoAnalysis(text(json, "paretoAnalysis"))
                    .histogramAnalysis(text(json, "histogramAnalysis"))
                    .actionProposedSummary(
                            text(json, "actionProposedSummary"))
                    .lessonsLearned(text(json, "lessonsLearned"))
                    .status("OPEN")
                    .build();

            CarAnalysis savedCar = carAnalysisRepository.save(car);

            // Save the actions generated by Gemini.
            saveSuggestedActions(savedCar, json.path("actions"));

            savedCar = carAnalysisRepository.save(savedCar);

            return carService.toDto(savedCar);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to generate automated CAR: "
                            + safeMessage(ex), ex);
        }
    }

    public CarAnalysis generateAnalysis(Long carId) {

        CarAnalysis car = carService.getCarEntity(carId);

        Long opportunityId = car.getOpportunity().getId();

        try {

            // Retrieve saved Project Metrics for this opportunity.
            ProjectMetricsResponse projectMetrics =
                    projectMetricsService.getMetrics(opportunityId);

            // Validate sprint-wise metrics.
            if (projectMetrics == null
                    || projectMetrics.getSprints() == null
                    || projectMetrics.getSprints().isEmpty()) {

                throw new IllegalStateException(
                        "Sprint-wise Project Metrics are not available. "
                        + "Please calculate Project Metrics before generating CAR.");
            }

            boolean hasSprintSummary =
                    projectMetrics.getSprints().stream()
                            .anyMatch(sprint -> sprint.getSummary() != null);

            if (!hasSprintSummary) {
                throw new IllegalStateException(
                        "Sprint-wise Summary Metrics are not available.");
            }

            // Generate CAR using CAR details and sprint-wise metrics.
            String response =
                    orchestrator.generateRootCauseAnalysis(
                            car,
                            projectMetrics);
            JsonNode json = objectMapper.readTree(normalizeJson(response));

            if (json == null || !json.isObject()) {
                throw new IllegalStateException(
                        "CAR AI response is not a JSON object.");
            }

            car.setRootCauseSummary(text(json, "rootCauseSummary"));
            car.setFiveWhysDetails(text(json, "fiveWhysDetails"));
            car.setFishboneAnalysis(text(json, "fishboneAnalysis"));
            car.setParetoAnalysis(text(json, "paretoAnalysis"));
            car.setHistogramAnalysis(text(json, "histogramAnalysis"));
            car.setActionProposedSummary(text(json, "actionProposedSummary"));
            car.setLessonsLearned(text(json, "lessonsLearned"));
            car.setStatus("OPEN");

            CarAnalysis savedCar = carAnalysisRepository.save(car);

            saveSuggestedActions(savedCar, json.path("actions"));

            return carAnalysisRepository.save(savedCar);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to generate CAR root cause analysis: "
                            + safeMessage(ex), ex);
        }
    }

    private void saveSuggestedActions(
            CarAnalysis car, JsonNode actions) {

        if (!actions.isArray()) {
            return;
        }

        for (JsonNode action : actions) {

            String description = text(action, "actionDescription");

            if (description == null || description.isBlank()) {
                continue;
            }

            String category = text(action, "actionCategory");

            if (category == null || category.isBlank()) {
                category = "POSITIVE".equalsIgnoreCase(car.getCarType())
                        ? "PROCESS_IMPROVEMENT"
                        : "CORRECTIVE";
            }

            String priority = text(action, "priority");

            if (!isValidPriority(priority)) {
                priority = "MEDIUM";
            }

            CarActionDto actionDto = CarActionDto.builder()
                    .actionCategory(
                            category.trim().toUpperCase(Locale.ROOT))
                    .actionDescription(description.trim())
                    .assignedTo(text(action, "assignedTo"))
                    .priority(
                            priority.trim().toUpperCase(Locale.ROOT))
                    .targetCompletionDate(
                            parseDate(text(action, "targetCompletionDate")))
                    .status("PENDING")
                    .build();

            actionPlanService.addAction(car.getId(), actionDto);
        }
    }

    private boolean isValidPriority(String priority) {

        if (priority == null) {
            return false;
        }

        return switch (priority.trim().toUpperCase(Locale.ROOT)) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> true;
            default -> false;
        };
    }

    private LocalDate parseDate(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String normalizeJson(String response) {

        if (response == null || response.isBlank()) {
            throw new IllegalStateException(
                    "CAR AI returned an empty response.");
        }

        String value = response.trim();

        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }

        return value.trim();
    }

    private String text(JsonNode json, String field) {

        JsonNode node = json.path(field);

        return node.isMissingNode() || node.isNull()
                ? null
                : node.asText();
    }

    private String safeMessage(Exception ex) {

        return ex.getMessage() == null
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
