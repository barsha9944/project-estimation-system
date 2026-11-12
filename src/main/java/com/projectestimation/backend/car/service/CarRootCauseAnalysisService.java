
package com.projectestimation.backend.car.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.car.ai.GeminiCarOrchestrator;
import com.projectestimation.backend.car.dto.CarActionDto;
import com.projectestimation.backend.car.model.CarAnalysis;
import com.projectestimation.backend.car.repository.CarAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import com.projectestimation.backend.projectmetrics.service.ProjectMetricsService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CarRootCauseAnalysisService {

    private final CarService carService;
    private final CarAnalysisRepository carAnalysisRepository;
    private final CarActionPlanService actionPlanService;
    private final GeminiCarOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    
    private final ProjectMetricsService projectMetricsService;

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
