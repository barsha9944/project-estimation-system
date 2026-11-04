
package com.projectestimation.backend.car.service;

import com.projectestimation.backend.car.model.CarAnalysis;
import com.projectestimation.backend.car.model.CarEffectiveness;
import com.projectestimation.backend.car.repository.CarEffectivenessRepository;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CarEffectivenessService {

    private final CarService carService;
    private final CarEffectivenessRepository effectivenessRepository;

    public CarEffectiveness createReview(
            Long carId,
            CarEffectiveness request) {

        CarAnalysis car = carService.getCarEntity(carId);

        CarEffectiveness review = CarEffectiveness.builder()
                .carAnalysis(car)
                .reviewDate(
                        request.getReviewDate() == null
                                ? LocalDate.now()
                                : request.getReviewDate()
                )
                .evaluationPeriod(request.getEvaluationPeriod())
                .expectedOutcome(request.getExpectedOutcome())
                .actualOutcome(request.getActualOutcome())
                .beforeImplementationMetric(
                        request.getBeforeImplementationMetric()
                )
                .afterImplementationMetric(
                        request.getAfterImplementationMetric()
                )
                .effectivenessResult(
                        request.getEffectivenessResult()
                )
                .supportingEvidence(
                        request.getSupportingEvidence()
                )
                .reviewedBy(request.getReviewedBy())
                .additionalActionsRequired(
                        request.getAdditionalActionsRequired()
                )
                .build();

        CarEffectiveness saved =
                effectivenessRepository.save(review);

        if ("EFFECTIVE".equalsIgnoreCase(
                saved.getEffectivenessResult())) {

            car.setStatus("CLOSED");

        } else if ("PARTIALLY_EFFECTIVE".equalsIgnoreCase(
                saved.getEffectivenessResult())
                || "NOT_EFFECTIVE".equalsIgnoreCase(
                saved.getEffectivenessResult())) {

            car.setStatus("IN_PROGRESS");
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<CarEffectiveness> getReviews(Long carId) {

        carService.getCarEntity(carId);

        return effectivenessRepository
                .findByCarAnalysisIdOrderByReviewDateDesc(carId);
    }

    @Transactional(readOnly = true)
    public CarEffectiveness getReview(Long reviewId) {

        return effectivenessRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CAR effectiveness review not found: " + reviewId
                ));
    }
}
