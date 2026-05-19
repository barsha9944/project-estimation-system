package com.projectestimation.backend.estimation.service;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.ai.AiEstimationResult;
import com.projectestimation.backend.estimation.ai.GeminiEstimationOrchestrator;
import com.projectestimation.backend.estimation.dto.EstimateCalculationRequest;
import com.projectestimation.backend.estimation.dto.EstimateCalculationResponse;
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

    private final GeminiEstimationOrchestrator geminiEstimationOrchestrator;
    private final EstimateResultRepository estimateResultRepository;
    private final OpportunityRepository opportunityRepository;
    private final ParametersRepository parametersRepository;

    public EstimationService(GeminiEstimationOrchestrator geminiEstimationOrchestrator,
                             EstimateResultRepository estimateResultRepository,
                             OpportunityRepository opportunityRepository,
                             ParametersRepository parametersRepository) {
        this.geminiEstimationOrchestrator = geminiEstimationOrchestrator;
        this.estimateResultRepository = estimateResultRepository;
        this.opportunityRepository = opportunityRepository;
        this.parametersRepository = parametersRepository;
    }

    /**
     * Legacy frontend-driven calculation (backward compatibility) — powered by Gemini AI.
     */
    public EstimateCalculationResponse calculate(EstimateCalculationRequest request, User user) {
        AiEstimationResult aiResult = geminiEstimationOrchestrator.estimateFromLegacyPayload(
                request.projectName(),
                request.requirementSummary(),
                request.parameters().complexityFactor(),
                request.parameters().riskFactor(),
                request.parameters().productivityFactor(),
                request.parameters().hourlyRate(),
                request.parameters().teamSize()
        );

        EstimateResult result = new EstimateResult();
        result.setProjectName(request.projectName());
        result.setRequirementSummary(request.requirementSummary());
        result.setComplexityFactor(request.parameters().complexityFactor());
        result.setRiskFactor(request.parameters().riskFactor());
        result.setProductivityFactor(request.parameters().productivityFactor());
        result.setHourlyRate(request.parameters().hourlyRate());
        result.setTeamSize(request.parameters().teamSize());
        applyAiOutput(result, aiResult);
        result.setCalculatedBy(user);

        return persistAndRespond(result);
    }

    /**
     * Primary opportunity-driven AI estimation workflow.
     */
    @Transactional
    public EstimateCalculationResponse calculateForOpportunity(Long opportunityId, User user) {
        Opportunity opportunity = loadOpportunity(opportunityId);
        Parameters parameters = loadParameters(opportunityId);

        AiEstimationResult aiResult = geminiEstimationOrchestrator.estimate(opportunity, parameters);

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
        applyAiOutput(result, aiResult);
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

    private Opportunity loadOpportunity(Long opportunityId) {
        return opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }

    private Parameters loadParameters(Long opportunityId) {
        return parametersRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Parameters not found for this opportunity"));
    }

    private void applyAiOutput(EstimateResult result, AiEstimationResult aiResult) {
        result.setTotalEffortHours(aiResult.totalEffortHours());
        result.setEstimatedCost(aiResult.estimatedCost());
        result.setTimelineWeeks(aiResult.timelineWeeks());
        result.setConfidenceScore(aiResult.confidenceScore());
        result.setBreakdown(aiResult.breakdown());
        result.setReasoning(aiResult.reasoning());
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
