package com.projectestimation.backend.testcase.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;

import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.model.EstimationUseCase;

import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.estimation.repository.EstimationUseCaseRepository;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;

import com.projectestimation.backend.testcase.ai.GeminiTestCaseOrchestrator;
import com.projectestimation.backend.testcase.dto.TestCaseDto;
import com.projectestimation.backend.testcase.dto.TestCaseGenerationResponse;
import com.projectestimation.backend.testcase.dto.TestStepDto;
import com.projectestimation.backend.testcase.model.TestCase;
import com.projectestimation.backend.testcase.model.TestCaseStep;
import com.projectestimation.backend.testcase.repository.TestCaseRepository;

@Service
public class TestCaseService {

    private final OpportunityRepository opportunityRepository;

    private final EstimationAnalysisRepository estimationAnalysisRepository;

    private final EstimationUseCaseRepository estimationUseCaseRepository;

    private final GeminiTestCaseOrchestrator geminiTestCaseOrchestrator;

    private final TestCaseRepository testCaseRepository;

    private final ObjectMapper objectMapper;

    public TestCaseService(

            OpportunityRepository opportunityRepository,

            EstimationAnalysisRepository estimationAnalysisRepository,

            EstimationUseCaseRepository estimationUseCaseRepository,

            GeminiTestCaseOrchestrator geminiTestCaseOrchestrator,

            TestCaseRepository testCaseRepository,

            ObjectMapper objectMapper

    ) {

        this.opportunityRepository = opportunityRepository;

        this.estimationAnalysisRepository = estimationAnalysisRepository;

        this.estimationUseCaseRepository = estimationUseCaseRepository;

        this.geminiTestCaseOrchestrator = geminiTestCaseOrchestrator;

        this.testCaseRepository = testCaseRepository;

        this.objectMapper = objectMapper;

    }

    @Transactional
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

            TestCaseGenerationResponse generatedResponse =
                    objectMapper.readValue(
                            response,
                            TestCaseGenerationResponse.class
                    );

            // Delete previously generated test cases
            testCaseRepository.deleteByOpportunityId(opportunityId);

            for (TestCaseDto dto : generatedResponse.testCases()) {

                TestCase testCase = TestCase.builder()
                        .opportunity(opportunity)
                        .reqId(dto.reqId())
                        .testCaseId(dto.testCaseId())
                        .testCaseName(dto.testCaseName())
                        .testCaseDescription(dto.testCaseDescription())
                        .testData(dto.testData())
                        .build();

                if (dto.steps() != null) {

                    for (TestStepDto stepDto : dto.steps()) {

                        TestCaseStep step = TestCaseStep.builder()
                                .testCase(testCase)
                                .stepNumber(stepDto.stepNumber())
                                .stepDescription(stepDto.stepDescription())
                                .expectedResult(stepDto.expectedResult())
                                .build();

                        testCase.getSteps().add(step);
                    }
                }

                testCaseRepository.save(testCase);
            }

            return generatedResponse;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to generate and save Gemini test cases",
                    e
            );
        }
    }
    @Transactional(readOnly = true)
    public TestCaseGenerationResponse getTestCases(Long opportunityId) {

        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException("Opportunity not found");
        }

        List<TestCase> testCases =
                testCaseRepository.findByOpportunityId(opportunityId);

        List<TestCaseDto> testCaseDtos = testCases.stream()
                .map(testCase -> new TestCaseDto(
                        testCase.getReqId(),
                        testCase.getTestCaseId(),
                        testCase.getTestCaseName(),
                        testCase.getTestCaseDescription(),
                        testCase.getTestData(),
                        testCase.getSteps().stream()
                                .map(step -> new TestStepDto(
                                        step.getStepNumber(),
                                        step.getStepDescription(),
                                        step.getExpectedResult()
                                ))
                                .toList()
                ))
                .toList();

        return new TestCaseGenerationResponse(testCaseDtos);
    }
    @Transactional
    public TestCaseGenerationResponse saveTestCases(
            Long opportunityId,
            List<TestCaseDto> testCaseDtos
    ) {

        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Opportunity not found"));

        for (TestCaseDto dto : testCaseDtos) {

            TestCase testCase =
                    testCaseRepository
                            .findByOpportunityIdAndTestCaseId(
                                    opportunityId,
                                    dto.testCaseId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Test case not found: " + dto.testCaseId()
                                    ));

            // Update test case fields
            testCase.setReqId(dto.reqId());
            testCase.setTestCaseId(dto.testCaseId());
            testCase.setTestCaseName(dto.testCaseName());
            testCase.setTestCaseDescription(dto.testCaseDescription());
            testCase.setTestData(dto.testData());

            // Remove existing steps
            testCase.getSteps().clear();

            // Add updated steps
            if (dto.steps() != null) {

                for (TestStepDto stepDto : dto.steps()) {

                    TestCaseStep step = TestCaseStep.builder()
                            .testCase(testCase)
                            .stepNumber(stepDto.stepNumber())
                            .stepDescription(stepDto.stepDescription())
                            .expectedResult(stepDto.expectedResult())
                            .build();

                    testCase.getSteps().add(step);
                }
            }

            testCaseRepository.save(testCase);
        }

        return getTestCases(opportunityId);
    }
}