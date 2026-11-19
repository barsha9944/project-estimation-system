
package com.projectestimation.backend.car.service;

import com.projectestimation.backend.car.dto.CarActionDto;
import com.projectestimation.backend.car.dto.CarEffectivenessDto;
import com.projectestimation.backend.car.dto.CarResponseDto;
import com.projectestimation.backend.car.dto.CarCreateRequest;
import com.projectestimation.backend.car.model.CarAction;
import com.projectestimation.backend.car.model.CarAnalysis;
import com.projectestimation.backend.car.model.CarEffectiveness;
import com.projectestimation.backend.car.repository.CarAnalysisRepository;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.common.storage.OpportunityDocumentStorageService;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CarService {

    private static final Logger log = LoggerFactory.getLogger(CarService.class);

    private final CarAnalysisRepository carAnalysisRepository;
    private final OpportunityRepository opportunityRepository;
    private final OpportunityDocumentStorageService documentStorageService;
    private final CarExcelParser carExcelParser;

    public CarResponseDto createCar(CarCreateRequest request) {

        Opportunity opportunity = opportunityRepository
                .findById(request.getOpportunityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Opportunity not found: "
                                + request.getOpportunityId()));

        String type = request.getCarType()
                .trim().toUpperCase(Locale.ROOT);

        if (!type.equals("POSITIVE") && !type.equals("NEGATIVE")) {
            throw new IllegalArgumentException(
                    "CAR type must be POSITIVE or NEGATIVE");
        }

        CarAnalysis car = CarAnalysis.builder()
                .opportunity(opportunity)
                .carType(type)
                .triggerForCar(request.getTriggerForCar())
                .problemStatement(request.getProblemStatement())
                .originatorName(request.getOriginatorName())
                .performanceMonitoringDetails(
                        request.getPerformanceMonitoringDetails())
                .causalAnalysisTechnique(
                        request.getCausalAnalysisTechnique())
                .status("DRAFT")
                .build();

        return toDto(carAnalysisRepository.save(car));
    }

    @Transactional
    public CarAnalysis getCarEntity(Long carId) {

        CarAnalysis car = carAnalysisRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CAR record not found: " + carId));

        syncCarFromPhysicalFileIfModified(car);
        return car;
    }

    @Transactional
    public CarResponseDto getCar(Long carId) {
        return toDto(getCarEntity(carId));
    }

    @Transactional
    public List<CarResponseDto> getCarsByOpportunity(
            Long opportunityId) {

        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException(
                    "Opportunity not found: " + opportunityId);
        }

        List<CarAnalysis> cars = carAnalysisRepository
                .findByOpportunityIdOrderByCreatedAtDesc(opportunityId);

        for (CarAnalysis car : cars) {
            syncCarFromPhysicalFileIfModified(car);
        }

        return cars.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void syncCarFromPhysicalFileIfModified(CarAnalysis car) {
        if (car == null || car.getId() == null || car.getOpportunity() == null) {
            return;
        }

        Long opportunityId = car.getOpportunity().getId();
        String relativePath = car.getFilePath();
        if (relativePath == null || relativePath.isBlank()) {
            relativePath = "Quality/CAR/CAR_Report_" + car.getId() + ".xlsx";
        }

        try {
            if (documentStorageService.exists(opportunityId, relativePath)) {
                String currentHash = documentStorageService.getFileChecksum(opportunityId, relativePath);
                if (car.getFileHash() == null || !car.getFileHash().equals(currentHash)) {
                    log.info("Detected physical CAR Excel update for opp: {}, car: {}, syncing from file", opportunityId, car.getId());
                    byte[] bytes = documentStorageService.readFile(opportunityId, relativePath);
                    CarExcelParser.ParsedCarData parsed = carExcelParser.parse(bytes);
                    if (parsed != null) {
                        if (parsed.triggerForCar() != null && !parsed.triggerForCar().isBlank()) {
                            car.setTriggerForCar(parsed.triggerForCar());
                        }
                        if (parsed.problemStatement() != null && !parsed.problemStatement().isBlank()) {
                            car.setProblemStatement(parsed.problemStatement());
                        }
                        if (parsed.originatorName() != null && !parsed.originatorName().isBlank()) {
                            car.setOriginatorName(parsed.originatorName());
                        }
                        if (parsed.causalAnalysisTechnique() != null && !parsed.causalAnalysisTechnique().isBlank()) {
                            car.setCausalAnalysisTechnique(parsed.causalAnalysisTechnique());
                        }
                        if (parsed.fiveWhysDetails() != null && !parsed.fiveWhysDetails().isBlank()) {
                            car.setFiveWhysDetails(parsed.fiveWhysDetails());
                        }

                        // Update or populate actions from physical Excel
                        if (parsed.actions() != null && !parsed.actions().isEmpty()) {
                            List<CarAction> existingActions = car.getActions();
                            if (existingActions == null) {
                                existingActions = new ArrayList<>();
                                car.setActions(existingActions);
                            }

                            for (int i = 0; i < parsed.actions().size(); i++) {
                                CarExcelParser.ParsedAction pa = parsed.actions().get(i);
                                CarAction action;
                                if (i < existingActions.size()) {
                                    action = existingActions.get(i);
                                } else {
                                    action = new CarAction();
                                    action.setCarAnalysis(car);
                                    existingActions.add(action);
                                }
                                if (pa.actionDescription() != null) action.setActionDescription(pa.actionDescription());
                                if (pa.actionCategory() != null) action.setActionCategory(pa.actionCategory());
                                if (pa.priority() != null) action.setPriority(pa.priority());
                                if (pa.assignedTo() != null) action.setAssignedTo(pa.assignedTo());
                                if (pa.targetCompletionDate() != null) action.setTargetCompletionDate(pa.targetCompletionDate());
                                if (pa.actionTaken() != null) action.setActionTaken(pa.actionTaken());
                                if (pa.status() != null) action.setStatus(pa.status());
                            }
                        }
                    }

                    car.setFilePath(relativePath);
                    car.setFileHash(currentHash);
                    car.setFileLastModified(documentStorageService.getLastModified(opportunityId, relativePath));
                    carAnalysisRepository.save(car);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed syncing CAR from physical file for car {}: {}", car.getId(), ex.getMessage());
        }
    }

    @Transactional
    public void recordPhysicalFileMetadata(Long carId, String relativePath, String hash, LocalDateTime lastModified) {
        carAnalysisRepository.findById(carId).ifPresent(car -> {
            car.setFilePath(relativePath);
            car.setFileHash(hash);
            car.setFileLastModified(lastModified);
            carAnalysisRepository.save(car);
        });
    }

    public CarResponseDto toDto(CarAnalysis car) {

        List<CarActionDto> actionDtos = car.getActions()
                .stream()
                .map(action -> CarActionDto.builder()
                        .id(action.getId())
                        .actionCategory(action.getActionCategory())
                        .actionDescription(action.getActionDescription())
                        .assignedTo(action.getAssignedTo())
                        .priority(action.getPriority())
                        .targetCompletionDate(
                                action.getTargetCompletionDate())
                        .actionTaken(action.getActionTaken())
                        .implementationDate(
                                action.getImplementationDate())
                        .evidenceReference(
                                action.getEvidenceReference())
                        .status(action.getStatus())
                        .build())
                .toList();

        List<CarEffectivenessDto> effectivenessDtos =
                car.getEffectivenessReviews()
                        .stream()
                        .map(this::toEffectivenessDto)
                        .toList();

        return CarResponseDto.builder()
                .id(car.getId())
                .opportunityId(car.getOpportunity().getId())
                .opportunityName(
                        car.getOpportunity().getOpportunityName())
                .carType(car.getCarType())
                .triggerForCar(car.getTriggerForCar())
                .problemStatement(car.getProblemStatement())
                .originatorName(car.getOriginatorName())
                .performanceMonitoringDetails(
                        car.getPerformanceMonitoringDetails())
                .causalAnalysisTechnique(
                        car.getCausalAnalysisTechnique())
                .rootCauseSummary(car.getRootCauseSummary())
                .fiveWhysDetails(car.getFiveWhysDetails())
                .fishboneAnalysis(car.getFishboneAnalysis())
                .paretoAnalysis(car.getParetoAnalysis())
                .histogramAnalysis(car.getHistogramAnalysis())
                .actionProposedSummary(car.getActionProposedSummary())
                .lessonsLearned(car.getLessonsLearned())
                .status(car.getStatus())
                .actions(actionDtos)
                .effectivenessReviews(effectivenessDtos)
                .createdAt(car.getCreatedAt())
                .updatedAt(car.getUpdatedAt())
                .build();
    }

    private CarEffectivenessDto toEffectivenessDto(
            CarEffectiveness review) {

        return CarEffectivenessDto.builder()
                .id(review.getId())
                .carId(review.getCarAnalysis().getId())
                .reviewDate(review.getReviewDate())
                .evaluationPeriod(review.getEvaluationPeriod())
                .expectedOutcome(review.getExpectedOutcome())
                .actualOutcome(review.getActualOutcome())
                .beforeImplementationMetric(
                        review.getBeforeImplementationMetric())
                .afterImplementationMetric(
                        review.getAfterImplementationMetric())
                .effectivenessResult(review.getEffectivenessResult())
                .supportingEvidence(review.getSupportingEvidence())
                .reviewedBy(review.getReviewedBy())
                .additionalActionsRequired(
                        review.getAdditionalActionsRequired())
                .build();
    }
}
