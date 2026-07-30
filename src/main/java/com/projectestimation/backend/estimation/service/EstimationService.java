package com.projectestimation.backend.estimation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.enums.CurrencyCode;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.common.util.CurrencyFormatter;
import com.projectestimation.backend.estimation.ai.AiEstimationResult;
import com.projectestimation.backend.estimation.ai.GeminiEstimationOrchestrator;
import com.projectestimation.backend.estimation.dto.EstimateCalculationRequest;
import com.projectestimation.backend.estimation.dto.EstimateCalculationResponse;
import com.projectestimation.backend.estimation.dto.EstimationAnalysisResponse;
import com.projectestimation.backend.estimation.engine.OpportunityEstimationInputResolver;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.estimation.repository.EstimateResultRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.parameters.model.Parameters;
import com.projectestimation.backend.parameters.repository.ParametersRepository;

@Service
public class EstimationService {

    private final GeminiEstimationOrchestrator geminiEstimationOrchestrator;
    private final EstimateResultRepository estimateResultRepository;
    private final OpportunityRepository opportunityRepository;
    private final ParametersRepository parametersRepository;
    private final GeminiEstimationAnalysisService geminiService;
    private final EstimationHtmlParser htmlParser;

    public EstimationService(GeminiEstimationOrchestrator geminiEstimationOrchestrator,
                             EstimateResultRepository estimateResultRepository,
                             OpportunityRepository opportunityRepository,
                             ParametersRepository parametersRepository,
                             GeminiEstimationAnalysisService geminiService,
                             EstimationHtmlParser htmlParser) {
        this.geminiEstimationOrchestrator = geminiEstimationOrchestrator;
        this.estimateResultRepository = estimateResultRepository;
        this.opportunityRepository = opportunityRepository;
        this.parametersRepository = parametersRepository;
        this.geminiService = geminiService;
        this.htmlParser = htmlParser;
    }

    /**
     * Legacy frontend-driven calculation (backward compatibility) — powered by Gemini AI.
     */
    public EstimateCalculationResponse calculate(EstimateCalculationRequest request, User user) {
        CurrencyCode currency = CurrencyFormatter.requireCurrency(request.parameters().currency());

        AiEstimationResult aiResult = geminiEstimationOrchestrator.estimateFromLegacyPayload(
                request.projectName(),
                request.requirementSummary(),
                request.parameters().complexityFactor(),
                request.parameters().riskFactor(),
                request.parameters().productivityFactor(),
                request.parameters().hourlyRate(),
                currency,
                request.parameters().teamSize()
        );

        EstimateResult result = new EstimateResult();
        result.setProjectName(request.projectName());
        result.setRequirementSummary(request.requirementSummary());
        result.setComplexityFactor(request.parameters().complexityFactor());
        result.setRiskFactor(request.parameters().riskFactor());
        result.setProductivityFactor(request.parameters().productivityFactor());
        result.setHourlyRate(request.parameters().hourlyRate());
        result.setCurrency(currency);
        result.setTeamSize(request.parameters().teamSize());
        applyAiOutput(result, aiResult, request.parameters().hourlyRate(), currency);
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
        result.setCurrency(parameters.getCurrency());
        result.setTeamSize(parameters.getTeamSize());
        applyAiOutput(result, aiResult, parameters.getHourlyRate(), parameters.getCurrency());
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

    private void applyAiOutput(EstimateResult result, AiEstimationResult aiResult, double hourlyRate, CurrencyCode currency) {
        result.setTotalEffortHours(aiResult.totalEffortHours());
        result.setEstimatedCost(CurrencyFormatter.calculateEstimatedCost(aiResult.totalEffortHours(), hourlyRate));
        result.setCurrency(currency);
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
                saved.getCurrency(),
                saved.getTimelineWeeks(),
                saved.getConfidenceScore(),
                saved.getBreakdown(),
                saved.getCalculatedAt()
        );
    }
    
    public EstimationAnalysisResponse generate(
            Long opportunityId
    ) {

        Opportunity opportunity =
                opportunityRepository.findById(opportunityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        String html =
                geminiService.generateAnalysisHtml(
                        opportunity
                );

        return new EstimationAnalysisResponse(
                htmlParser.extractActorTable(html),
                htmlParser.extractUseCaseTable(html)
        );
    }
    
    
}
