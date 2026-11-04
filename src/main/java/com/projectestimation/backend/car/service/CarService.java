
package com.projectestimation.backend.car.service;

import com.projectestimation.backend.car.dto.CarActionDto;
import com.projectestimation.backend.car.dto.CarEffectivenessDto;
import com.projectestimation.backend.car.dto.CarResponseDto;
import com.projectestimation.backend.car.dto.CarCreateRequest;
import com.projectestimation.backend.car.model.CarAnalysis;
import com.projectestimation.backend.car.model.CarEffectiveness;
import com.projectestimation.backend.car.repository.CarAnalysisRepository;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CarService {

    private final CarAnalysisRepository carAnalysisRepository;
    private final OpportunityRepository opportunityRepository;

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

    @Transactional(readOnly = true)
    public CarAnalysis getCarEntity(Long carId) {

        return carAnalysisRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CAR record not found: " + carId));
    }

    @Transactional(readOnly = true)
    public CarResponseDto getCar(Long carId) {
        return toDto(getCarEntity(carId));
    }

    @Transactional(readOnly = true)
    public List<CarResponseDto> getCarsByOpportunity(
            Long opportunityId) {

        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException(
                    "Opportunity not found: " + opportunityId);
        }

        return carAnalysisRepository
                .findByOpportunityIdOrderByCreatedAtDesc(opportunityId)
                .stream()
                .map(this::toDto)
                .toList();
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
