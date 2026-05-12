package com.projectestimation.backend.estimation.service;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.estimation.dto.EstimateCalculationRequest;
import com.projectestimation.backend.estimation.dto.EstimateCalculationResponse;
import com.projectestimation.backend.estimation.engine.EstimationEngine;
import com.projectestimation.backend.estimation.engine.EstimationInput;
import com.projectestimation.backend.estimation.engine.EstimationOutput;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.estimation.repository.EstimateResultRepository;
import org.springframework.stereotype.Service;

@Service
public class EstimationService {

    private final EstimationEngine estimationEngine;
    private final EstimateResultRepository estimateResultRepository;

    public EstimationService(EstimationEngine estimationEngine, EstimateResultRepository estimateResultRepository) {
        this.estimationEngine = estimationEngine;
        this.estimateResultRepository = estimateResultRepository;
    }

    public EstimateCalculationResponse calculate(EstimateCalculationRequest request, User user) {
        EstimationInput input = new EstimationInput(
                request.requirementSummary(),
                request.parameters().complexityFactor(),
                request.parameters().riskFactor(),
                request.parameters().productivityFactor(),
                request.parameters().hourlyRate(),
                request.parameters().teamSize()
        );
        EstimationOutput output = estimationEngine.compute(input);

        EstimateResult result = new EstimateResult();
        result.setProjectName(request.projectName());
        result.setRequirementSummary(request.requirementSummary());
        result.setComplexityFactor(request.parameters().complexityFactor());
        result.setRiskFactor(request.parameters().riskFactor());
        result.setProductivityFactor(request.parameters().productivityFactor());
        result.setHourlyRate(request.parameters().hourlyRate());
        result.setTeamSize(request.parameters().teamSize());
        result.setTotalEffortHours(output.totalEffortHours());
        result.setEstimatedCost(output.estimatedCost());
        result.setTimelineWeeks(output.timelineWeeks());
        result.setConfidenceScore(output.confidenceScore());
        result.setBreakdown(output.breakdown());
        result.setCalculatedBy(user);

        EstimateResult saved = estimateResultRepository.save(result);
        return new EstimateCalculationResponse(
                saved.getId(),
                saved.getProjectName(),
                saved.getTotalEffortHours(),
                saved.getEstimatedCost(),
                saved.getTimelineWeeks(),
                saved.getConfidenceScore(),
                saved.getBreakdown(),
                saved.getCalculatedAt()
        );
    }
}
