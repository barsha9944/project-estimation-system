package com.projectestimation.backend.testcase.service;

import java.io.IOException;
import java.util.ArrayList;
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
import com.projectestimation.backend.testcase.dto.TestScenarioDto;
import com.projectestimation.backend.testcase.dto.TestStepDto;
import com.projectestimation.backend.testcase.model.TestCase;
import com.projectestimation.backend.testcase.model.TestCaseScenario;
import com.projectestimation.backend.testcase.model.TestCaseStep;
import com.projectestimation.backend.testcase.repository.TestCaseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final OpportunityRepository opportunityRepository;

    private final EstimationAnalysisRepository estimationAnalysisRepository;

    private final EstimationUseCaseRepository estimationUseCaseRepository;

    private final GeminiTestCaseOrchestrator geminiTestCaseOrchestrator;

    private final TestCaseRepository testCaseRepository;

    private final TestCaseExcelService testCaseExcelService;

    private final ObjectMapper objectMapper;


    // ============================================================
    // GENERATE TEST CASES
    // ============================================================

    @Transactional
    public TestCaseGenerationResponse generateTestCases(
            Long opportunityId
    ) {

        Opportunity opportunity =
                opportunityRepository.findById(opportunityId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Opportunity not found"
                                )
                        );

        EstimationAnalysis estimationAnalysis =
                estimationAnalysisRepository
                        .findByOpportunityId(opportunityId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Estimation analysis not found for this opportunity"
                                )
                        );

        List<EstimationUseCase> useCases =
                estimationUseCaseRepository
                        .findByEstimationAnalysisId(
                                estimationAnalysis.getId()
                        );

        if (useCases.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No use cases found for this estimation analysis"
            );
        }


        // ========================================================
        // GENERATE USING GEMINI
        // ========================================================

        String response =
                geminiTestCaseOrchestrator.generate(
                        opportunity,
                        useCases
                );


        try {

            TestCaseGenerationResponse generatedResponse =
                    objectMapper.readValue(
                            response,
                            TestCaseGenerationResponse.class
                    );


            // ====================================================
            // DELETE PREVIOUS TEST CASES
            // ====================================================

            testCaseRepository.deleteByOpportunityId(
                    opportunityId
            );


            // ====================================================
            // SAVE EACH TEST CASE
            // ====================================================

            for (TestCaseDto dto :
                    generatedResponse.testCases()) {

                TestCase testCase =
                        TestCase.builder()
                                .opportunity(opportunity)
                                .reqId(dto.reqId())
                                .testCaseId(dto.testCaseId())
                                .testCondition(dto.testCondition())
                                .testCaseName(dto.testCaseName())
                                .testCaseDescription(
                                        dto.testCaseDescription()
                                )
                                .testData(dto.testData())
                                .build();


                // =================================================
                // CREATE SCENARIOS
                // =================================================

                if (dto.testCaseScenario() != null) {

                    for (TestScenarioDto scenarioDto :
                            dto.testCaseScenario()) {

                        TestCaseScenario scenario =
                                TestCaseScenario.builder()
                                        .testCase(testCase)
                                        .scenarioId(
                                                scenarioDto.scenarioId()
                                        )
                                        .scenarioName(
                                                scenarioDto.scenarioName()
                                        )
                                        .scenarioType(
                                                scenarioDto.scenarioType()
                                        )
                                        .build();


                        // ==========================================
                        // CREATE STEPS FOR THIS SCENARIO
                        // ==========================================

                        if (scenarioDto.steps() != null) {

                            for (TestStepDto stepDto :
                                    scenarioDto.steps()) {

                                TestCaseStep step =
                                        TestCaseStep.builder()
                                                .testCase(testCase)
                                                .scenario(scenario)
                                                .stepNumber(
                                                        stepDto.stepNumber()
                                                )
                                                .stepDescription(
                                                        stepDto.stepDescription()
                                                )
                                                .expectedResult(
                                                        stepDto.expectedResult()
                                                )
                                                .actualResult(
                                                        stepDto.actualResult()
                                                )
                                                .testStatus(
                                                        stepDto.testStatus()
                                                )
                                                .passFail(
                                                        stepDto.passFail()
                                                )
                                                .defectId(
                                                        stepDto.defectId()
                                                )
                                                .severity(
                                                        stepDto.severity()
                                                )
                                                .defectType(
                                                        stepDto.defectType()
                                                )
                                                .rootCause(
                                                        stepDto.rootCause()
                                                )
                                                .phaseIntroduced(
                                                        stepDto.phaseIntroduced()
                                                )
                                                .build();

                                scenario.getSteps().add(step);
                            }
                        }


                        // Add scenario to test case
                        testCase
                                .getTestCaseScenario()
                                .add(scenario);
                    }
                }


                // =================================================
                // SAVE TEST CASE
                // =================================================

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


    // ============================================================
    // GET TEST CASES
    // ============================================================

    @Transactional(readOnly = true)
    public TestCaseGenerationResponse getTestCases(
            Long opportunityId
    ) {

        if (!opportunityRepository.existsById(opportunityId)) {

            throw new ResourceNotFoundException(
                    "Opportunity not found"
            );
        }


        List<TestCase> testCases =
                testCaseRepository.findByOpportunityId(
                        opportunityId
                );


        List<TestCaseDto> testCaseDtos =
                testCases.stream()
                        .map(this::mapToDto)
                        .toList();


        return new TestCaseGenerationResponse(
                testCaseDtos
        );
    }


    // ============================================================
    // MAP ENTITY → DTO
    // ============================================================

    private TestCaseDto mapToDto(
            TestCase testCase
    ) {

        List<TestScenarioDto> scenarios =
                new ArrayList<>();


        if (testCase.getTestCaseScenario() != null) {

            for (TestCaseScenario scenario :
                    testCase.getTestCaseScenario()) {

                List<TestStepDto> steps =
                        new ArrayList<>();


                if (scenario.getSteps() != null) {

                    for (TestCaseStep step :
                            scenario.getSteps()) {

                        TestStepDto stepDto =
                                new TestStepDto(
                                        step.getStepNumber(),
                                        step.getStepDescription(),
                                        step.getExpectedResult(),
                                        step.getActualResult(),
                                        step.getTestStatus(),
                                        step.getPassFail(),
                                        step.getDefectId(),
                                        step.getSeverity(),
                                        step.getDefectType(),
                                        step.getRootCause(),
                                        step.getPhaseIntroduced()
                                );

                        steps.add(stepDto);
                    }
                }


                TestScenarioDto scenarioDto =
                        new TestScenarioDto(
                                scenario.getScenarioId(),
                                scenario.getScenarioName(),
                                scenario.getScenarioType(),
                                steps
                        );


                scenarios.add(scenarioDto);
            }
        }


        return new TestCaseDto(
                testCase.getReqId(),
                testCase.getTestCaseId(),
                testCase.getTestCondition(),
                testCase.getTestCaseName(),
                scenarios,
                testCase.getTestCaseDescription(),
                testCase.getTestData()
        );
    }


    // ============================================================
    // SAVE TEST CASES
    // ============================================================

    @Transactional
    public TestCaseGenerationResponse saveTestCases(
            Long opportunityId,
            List<TestCaseDto> testCaseDtos
    ) {

        if (!opportunityRepository.existsById(opportunityId)) {

            throw new ResourceNotFoundException(
                    "Opportunity not found"
            );
        }


        // ========================================================
        // PROCESS EACH TEST CASE
        // ========================================================

        for (TestCaseDto dto : testCaseDtos) {

            TestCase testCase =
                    testCaseRepository
                            .findByOpportunityIdAndTestCaseId(
                                    opportunityId,
                                    dto.testCaseId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Test case not found: "
                                                    + dto.testCaseId()
                                    )
                            );


            // ====================================================
            // UPDATE TEST CASE FIELDS
            // ====================================================

            testCase.setReqId(
                    dto.reqId()
            );

            testCase.setTestCaseId(
                    dto.testCaseId()
            );

            testCase.setTestCondition(
                    dto.testCondition()
            );

            testCase.setTestCaseName(
                    dto.testCaseName()
            );

            testCase.setTestCaseDescription(
                    dto.testCaseDescription()
            );

            testCase.setTestData(
                    dto.testData()
            );


            // ====================================================
            // REMOVE OLD SCENARIOS
            // ====================================================

            testCase.getTestCaseScenario().clear();


            // ====================================================
            // ADD UPDATED SCENARIOS
            // ====================================================

            if (dto.testCaseScenario() != null) {

                for (TestScenarioDto scenarioDto :
                        dto.testCaseScenario()) {

                    TestCaseScenario scenario =
                            TestCaseScenario.builder()
                                    .testCase(testCase)
                                    .scenarioId(
                                            scenarioDto.scenarioId()
                                    )
                                    .scenarioName(
                                            scenarioDto.scenarioName()
                                    )
                                    .scenarioType(
                                            scenarioDto.scenarioType()
                                    )
                                    .build();


                    // ==========================================
                    // ADD STEPS
                    // ==========================================

                    if (scenarioDto.steps() != null) {

                        for (TestStepDto stepDto :
                                scenarioDto.steps()) {

                            TestCaseStep step =
                                    TestCaseStep.builder()
                                            .testCase(testCase)
                                            .scenario(scenario)
                                            .stepNumber(
                                                    stepDto.stepNumber()
                                            )
                                            .stepDescription(
                                                    stepDto.stepDescription()
                                            )
                                            .expectedResult(
                                                    stepDto.expectedResult()
                                            )
                                            .actualResult(
                                                    stepDto.actualResult()
                                            )
                                            .testStatus(
                                                    stepDto.testStatus()
                                            )
                                            .passFail(
                                                    stepDto.passFail()
                                            )
                                            .defectId(
                                                    stepDto.defectId()
                                            )
                                            .severity(
                                                    stepDto.severity()
                                            )
                                            .defectType(
                                                    stepDto.defectType()
                                            )
                                            .rootCause(
                                                    stepDto.rootCause()
                                            )
                                            .phaseIntroduced(
                                                    stepDto.phaseIntroduced()
                                            )
                                            .build();

                            scenario.getSteps().add(step);
                        }
                    }


                    // Add scenario to test case
                    testCase
                            .getTestCaseScenario()
                            .add(scenario);
                }
            }


            // ====================================================
            // SAVE
            // ====================================================

            testCaseRepository.save(
                    testCase
            );
        }


        // ========================================================
        // RETURN UPDATED DATA
        // ========================================================

        return getTestCases(
                opportunityId
        );
    }


    // ============================================================
    // DOWNLOAD EXCEL
    // ============================================================

    @Transactional(readOnly = true)
    public byte[] downloadTestCases(
            Long opportunityId
    ) throws IOException {

        if (!opportunityRepository.existsById(opportunityId)) {

            throw new ResourceNotFoundException(
                    "Opportunity not found"
            );
        }


        List<TestCase> testCases =
                testCaseRepository.findByOpportunityId(
                        opportunityId
                );


        if (testCases.isEmpty()) {

            throw new ResourceNotFoundException(
                    "No test cases found for this opportunity"
            );
        }


        return testCaseExcelService.generateExcel(
                testCases
        );
    }
}