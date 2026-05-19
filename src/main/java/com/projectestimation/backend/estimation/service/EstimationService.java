package com.projectestimation.backend.estimation.service;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.dto.EstimateCalculationRequest;
import com.projectestimation.backend.estimation.dto.EstimateCalculationResponse;
import com.projectestimation.backend.estimation.engine.EstimationEngine;
import com.projectestimation.backend.estimation.engine.EstimationInput;
import com.projectestimation.backend.estimation.engine.EstimationOutput;
import com.projectestimation.backend.estimation.engine.OpportunityEstimationInputResolver;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.estimation.repository.EstimateResultRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.parameters.model.Parameters;
import com.projectestimation.backend.parameters.repository.ParametersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstimationService {

    private final EstimationEngine estimationEngine;
    private final EstimateResultRepository estimateResultRepository;
    private final OpportunityRepository opportunityRepository;
    private final ParametersRepository parametersRepository;

    public EstimationService(EstimationEngine estimationEngine,
                             EstimateResultRepository estimateResultRepository,
                             OpportunityRepository opportunityRepository,
                             ParametersRepository parametersRepository) {
        this.estimationEngine = estimationEngine;
        this.estimateResultRepository = estimateResultRepository;
        this.opportunityRepository = opportunityRepository;
        this.parametersRepository = parametersRepository;
    }

    /**
     * Legacy frontend-driven calculation (backward compatibility).
     */
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
        applyOutput(result, output);
        result.setCalculatedBy(user);

        return persistAndRespond(result);
    }

    /**
     * Primary opportunity-driven estimation workflow.
     */
    @Transactional
    public EstimateCalculationResponse calculateForOpportunity(Long opportunityId, User user) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        Parameters parameters = parametersRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Parameters not found for this opportunity"));

        EstimationInput input = OpportunityEstimationInputResolver.resolve(opportunity, parameters);
        EstimationOutput output = estimationEngine.compute(input);

        double complexityFactor = OpportunityEstimationInputResolver.resolveComplexityFactor(
                parameters.getComplexity(), opportunity);

        EstimateResult result = new EstimateResult();
        result.setOpportunity(opportunity);
        result.setProjectName(opportunity.getOpportunityName());
        result.setRequirementSummary(opportunity.getRequirementSummary());
        result.setComplexityFactor(complexityFactor);
        result.setRiskFactor(parameters.getRiskFactor());
        result.setProductivityFactor(parameters.getProductivityFactor());
        result.setHourlyRate(parameters.getHourlyRate());
        result.setTeamSize(parameters.getTeamSize());
        applyOutput(result, output);
        result.setCalculatedBy(user);

        opportunity.setStatus(OpportunityStatus.ESTIMATED);
        opportunityRepository.save(opportunity);

        return persistAndRespond(result);
    }

    public EstimateCalculationResponse getLatestEstimateByOpportunityId(Long opportunityId) {
        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException("Opportunity not found");
        }

        EstimateResult estimate = estimateResultRepository.findFirstByOpportunity_IdOrderByCreatedAtDesc(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found for this opportunity"));

        return toResponse(estimate);
    }

    private void applyOutput(EstimateResult result, EstimationOutput output) {
        result.setTotalEffortHours(output.totalEffortHours());
        result.setEstimatedCost(output.estimatedCost());
        result.setTimelineWeeks(output.timelineWeeks());
        result.setConfidenceScore(output.confidenceScore());
        result.setBreakdown(output.breakdown());
    }

    private EstimateCalculationResponse persistAndRespond(EstimateResult result) {
        EstimateResult saved = estimateResultRepository.save(result);
        return toResponse(saved);
    }

    private EstimateCalculationResponse toResponse(EstimateResult saved) {
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
