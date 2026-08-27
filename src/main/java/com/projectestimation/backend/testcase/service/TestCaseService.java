package com.projectestimation.backend.testcase.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.estimation.repository.EstimationUseCaseRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.testcase.ai.GeminiTestCaseOrchestrator;
import com.projectestimation.backend.testcase.dto.TestCaseGenerationResponse;

@Service
public class TestCaseService {

    private final OpportunityRepository opportunityRepository;
    private final EstimationAnalysisRepository estimationAnalysisRepository;
    private final EstimationUseCaseRepository estimationUseCaseRepository;
    private final GeminiTestCaseOrchestrator geminiTestCaseOrchestrator;
    private final ObjectMapper objectMapper;

    public TestCaseService(
            OpportunityRepository opportunityRepository,
            EstimationAnalysisRepository estimationAnalysisRepository,
            EstimationUseCaseRepository estimationUseCaseRepository,
            GeminiTestCaseOrchestrator geminiTestCaseOrchestrator,
            ObjectMapper objectMapper
    ) {
        this.opportunityRepository = opportunityRepository;
        this.estimationAnalysisRepository = estimationAnalysisRepository;
        this.estimationUseCaseRepository = estimationUseCaseRepository;
        this.geminiTestCaseOrchestrator = geminiTestCaseOrchestrator;
        this.objectMapper = objectMapper;
    }

    public TestCaseGenerationResponse generateTestCases(Long opportunityId) {

        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Opportunity not found"));

        EstimationAnalysis estimationAnalysis =
                estimationAnalysisRepository.findByOpportunityId(opportunityId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Estimation analysis not found for this opportunity"));

        List<EstimationUseCase> useCases =
                estimationUseCaseRepository.findByEstimationAnalysisId(
                        estimationAnalysis.getId()
                );
        
        if (useCases.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No use cases found for this estimation analysis"
            );
        }

        String response = geminiTestCaseOrchestrator.generate(
                opportunity,
                useCases
        );

        try {
            return objectMapper.readValue(
                    response,
                    TestCaseGenerationResponse.class
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse Gemini test case response",
                    e
            );
        }
    }
}