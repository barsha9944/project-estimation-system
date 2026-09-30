package com.projectestimation.backend.srs.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.srs.dto.FunctionalRequirementDto;
import com.projectestimation.backend.srs.dto.SrsDto;
import com.projectestimation.backend.srs.dto.TraceabilityMatrixEntryDto;

@Service
public class GeminiSrsOrchestrator {

    private static final int MAX_OUTPUT_TOKENS = 16384;

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiSrsOrchestrator(
            GeminiClient geminiClient,
            ObjectMapper objectMapper) {

        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public SrsDto generate(
            Opportunity opportunity,
            List<EstimationUseCase> useCases) {

        String prompt = buildPrompt(
                opportunity,
                useCases
        );

        String response = geminiClient.generateJsonContent(
                prompt,
                MAX_OUTPUT_TOKENS
        );

        return parseResponse(response, useCases);
    }

    private String buildPrompt(
            Opportunity opportunity,
            List<EstimationUseCase> useCases) {

        StringBuilder useCaseContext = new StringBuilder();

        if (useCases != null) {
            for (EstimationUseCase useCase : useCases) {

                useCaseContext
                        .append("- Use Case ID: ")
                        .append(safeText(useCase.getId()))
                        .append("\n")
                        .append("  Use Case Name: ")
                        .append(safeText(useCase.getUseCaseName()))
                        .append("\n")
                        .append("  Complexity: ")
                        .append(safeText(useCase.getComplexity()))
                        .append("\n");
            }
        }

        return """
                You are an expert Software Requirements Specification (SRS) analyst.

                Generate a complete Software Requirements Specification for the project
                using ONLY the project information provided below.

                The generated SRS must follow a professional SRS structure and must be
                suitable for rendering as a formal project document.

                IMPORTANT RULES:

                1. Return ONLY valid JSON.

                2. Do NOT return Markdown.

                3. Do NOT wrap the JSON inside ```json or any code block.

                4. Do NOT add explanations before or after the JSON.

                5. Do NOT invent specific project facts that are not supported by the
                   provided project information.

                6. If a value cannot be determined from the provided information,
                   use a reasonable descriptive value such as "Not specified" rather
                   than inventing a concrete technology, number, or requirement.

                7. The SRS is a requirements document, not a source-code document.

                8. Do not include UI design/mockup descriptions.

                9. Functional requirements must be derived primarily from the provided
                   estimation use cases and project components.

                10. Each functional requirement must have a unique requirementId.

                11. Keep the language formal, clear, and suitable for a business
                    requirements document.

                12. Do not copy project-specific content from any example SRS template.

                13. The reference structure may be followed, but the content must be
                    specific to this project.

                14. For functional requirements, use the provided estimation use case
                    name and complexity as the primary source for identifying the
                    requirement.

                15. The available estimation use case data contains only the use case
                    name and complexity. Do not assume that additional use-case-specific
                    facts were provided.

                16. Derive detailed descriptions, flows, business rules, fields,
                    preconditions, and postconditions only from the available project
                    information. Where sufficient information is not available, use
                    "Not specified" rather than inventing concrete project facts.

                PROJECT INFORMATION
                -------------------

                Opportunity Name:
                %s

                Client Name:
                %s

                Requirement Summary:
                %s

                Implementation Type:
                %s

                Platforms:
                %s

                Technology Categories:
                %s

                Enterprise Contexts:
                %s

                Components:
                %s

                Priority:
                %s

                Expected Delivery Date:
                %s

                Opportunity Status:
                %s


                ESTIMATION USE CASES
                --------------------
                %s


                REQUIRED SRS STRUCTURE
                ----------------------

                Generate the following sections:

                1. Document Information

                2. Introduction

                   - Purpose
                   - Background of Development
                   - Scope of the System
                   - Assumptions and Dependencies

                3. Overall Product Requirements

                   - Product Perspective
                   - Product Components

                   Use the provided project components to describe the actual product
                   components. Do not introduce components that are not supported by
                   the provided project information.

                4. External Interface Requirements

                   - Software Interfaces
                   - Hardware Interfaces
                   - Communication Interfaces
                   - User Interfaces

                   Do not create UI designs. Describe user interfaces only at a
                   requirements level.

                5. Proposed System Environments

                   - Operational Scenario
                   - Development Environment
                   - Testing Environment
                   - Deployment Environment

                   For each environment provide:

                   - Software Needed
                   - Hardware Needed
                   - Network Needed

                   Only include specific software, hardware, network products, services,
                   versions, or deployment technologies when they are supported by the
                   provided project information. Otherwise use "Not specified".

                   Do not assume infrastructure, cloud providers, operating systems,
                   servers, browsers, or network configurations.

                6. Detailed Functional Requirements

                   Create functional requirements based on the provided estimation
                   use cases and project information.

                   Each functional requirement must contain:

                   - requirementId
                   - module
                   - requirementName
                   - description
                   - primaryActor
                   - secondaryActor
                   - preconditions
                   - basicFlow
                   - businessRules
                   - fields
                   - postconditions

                   The fields section must contain:

                   - fieldName
                   - type
                   - lengthOrFormat
                   - mandatory

                   Use meaningful requirement IDs such as:

                   FR-001, FR-002, FR-003, etc.

                   Generate exactly one functional requirement for each provided
					estimation use case.
					
					Every estimation use case must be represented by a corresponding
					functional requirement unless the use case list is empty.
					
					Do not merge multiple estimation use cases into one functional
					requirement.
					
					Do not create additional functional requirements that are not
					associated with a provided estimation use case.

                   For each functional requirement derived from an estimation use case:

                   - Use the use case name to identify the business capability.

                   - Use the complexity only as supporting context; do not treat
                     complexity as a functional requirement.

                   - Describe what the system is required to accomplish rather than
                     describing implementation details.

                   - Keep the requirement focused on observable system behavior.

                   - Do not invent primary or secondary actors when they are not
                     supported by the provided project information. Use "Not specified"
                     when the actor cannot be determined.

                   - Do not invent specific screens, API endpoints, database tables,
                     field values, user names, performance numbers, or technologies
                     unless supported by the project information.

                7. Non-Functional Requirements

                   Provide requirements for:

                   - Accuracy
                   - Audit Trail
                   - Availability
                   - Capacity Limits
                   - Data Retention
                   - Performance
                   - Portability
                   - Recoverability
                   - Reliability
                   - Security Requirements
                   - Other Compliance Requirements

                   These must be project-specific where the available information
                   supports it.

                   Each non-functional requirement must describe an expected
                   characteristic or constraint of this specific system. Do not provide
                   generic textbook definitions of the NFR category. If a specific
                   requirement cannot be determined from the provided project
                   information, use "Not specified".

                   Do not invent exact performance numbers, uptime percentages,
                   retention periods, compliance certifications, or security standards
                   unless they are supported by the provided information.

                8. Data Requirements

                   Provide:

                   - Data Structures and Relationships
                   - Input to the System
                   - Output from the System
                   - Inter-functional Data Definitions
                   - Component Cross Reference

                   Inputs and outputs should identify meaningful data used by the
                   project rather than inventing database structures that were not
                   provided.

                   For Data Structures and Relationships, describe the logical data
                   entities and relationships only when supported by the provided
                   project information. Do not invent database table names, column
                   names, primary keys, foreign keys, or database schemas.

                9. Requirements Traceability Matrix

                   Create traceability entries connecting functional requirements to
                   their source/use case and validation method.

                   For requirements derived from an estimation use case:

                   - Use the corresponding estimation use case ID as part of the source.

                   - Use the exact estimation use case name in relatedUseCase.

                   - Keep the requirementId consistent with the functional requirement
                     being traced.

                   Do not create traceability entries for requirements that were not
                   actually generated in the functional requirements section.

                   Use requirement IDs from the functional requirements section.

                   Each traceability entry must contain:

                   - requirementId
                   - requirementDescription
                   - source
                   - module
                   - relatedUseCase
                   - validationMethod


                REQUIRED JSON SHAPE
                ------------------

                The response MUST deserialize into the following structure:

                {
                  "documentInformation": {
                    "documentTitle": "",
                    "projectName": "",
                    "clientName": "",
                    "version": "",
                    "date": "",
                    "preparedBy": "",
                    "reviewedBy": "",
                    "approvedBy": ""
                  },

                  "introduction": {
                    "purpose": "",
                    "backgroundOfDevelopment": "",
                    "scopeOfTheSystem": "",
                    "assumptionsAndDependencies": []
                  },

                  "overallProductRequirements": {
                    "productPerspective": "",
                    "productComponents": []
                  },

                  "externalInterfaceRequirements": {
					  "softwareInterfaces": [
					    {
					      "technology": "",
					      "description": ""
					    }
					  ],
					  "hardwareInterfaces": [],
					  "communicationInterfaces": [
					    ""
					  ],
					  "userInterfaces": ""
					},

                  "proposedSystemEnvironments": {
                    "operationalScenario": "",

                    "developmentEnvironment": {
                      "softwareNeeded": [
                        {
                          "technologyArea": "",
                          "productServiceOrStandard": ""
                        }
                      ],
                      "hardwareNeeded": [],
                      "networkNeeded": []
                    },

                    "testingEnvironment": {
                      "softwareNeeded": [
                        {
                          "technologyArea": "",
                          "productServiceOrStandard": ""
                        }
                      ],
                      "hardwareNeeded": [],
                      "networkNeeded": []
                    },

                    "deploymentEnvironment": {
                      "softwareNeeded": [
                        {
                          "technologyArea": "",
                          "productServiceOrStandard": ""
                        }
                      ],
                      "hardwareNeeded": [],
                      "networkNeeded": []
                    }
                  },

                  "functionalRequirements": [
                    {
                      "requirementId": "",
                      "module": "",
                      "requirementName": "",
                      "description": "",
                      "primaryActor": "",
                      "secondaryActor": "",
                      "preconditions": [],
                      "basicFlow": [],
                      "businessRules": [],
                      "fields": [
                        {
                          "fieldName": "",
                          "type": "",
                          "lengthOrFormat": "",
                          "mandatory": true
                        }
                      ],
                      "postconditions": []
                    }
                  ],

                  "nonFunctionalRequirements": {
                    "accuracy": "",
                    "auditTrail": "",
                    "availability": "",
                    "capacityLimits": "",
                    "dataRetention": "",
                    "performance": "",
                    "portability": "",
                    "recoverability": "",
                    "reliability": "",
                    "securityRequirements": "",
                    "otherComplianceRequirements": ""
                  },

                  "dataRequirements": {
                    "dataStructuresAndRelationships": "",

                    "inputs": [
                      {
                        "name": "",
                        "description": ""
                      }
                    ],

                    "outputs": [
                      {
                        "name": "",
                        "description": ""
                      }
                    ],

                    "interFunctionalDataDefinitions": "",
                    "componentCrossReference": ""
                  },

                  "requirementsTraceabilityMatrix": [
                    {
                      "requirementId": "",
                      "requirementDescription": "",
                      "source": "",
                      "module": "",
                      "relatedUseCase": "",
                      "validationMethod": ""
                    }
                  ]
                }

                NOW GENERATE THE SRS.

                """
                .formatted(
                        safeText(opportunity.getOpportunityName()),
                        safeText(opportunity.getClientName()),
                        safeText(opportunity.getRequirementSummary()),
                        safeText(opportunity.getImplementationType()),
                        safeText(opportunity.getPlatforms()),
                        safeText(opportunity.getTechnologyCategories()),
                        safeText(opportunity.getEnterpriseContexts()),
                        safeText(opportunity.getComponents()),
                        safeText(opportunity.getPriority()),
                        safeText(opportunity.getExpectedDeliveryDate()),
                        safeText(opportunity.getStatus()),
                        useCaseContext
                );
    }

    private String safeText(Object value) {
        return value == null ? "Not specified" : String.valueOf(value);
    }

    private SrsDto parseResponse(
            String response,
            List<EstimationUseCase> useCases) {

        try {
            SrsDto generatedSrs =
                    objectMapper.readValue(response, SrsDto.class);

            return normalizeTraceabilityMatrix(
                    generatedSrs,
                    useCases
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse Gemini SRS response",
                    e
            );
        }
    }

    private SrsDto normalizeTraceabilityMatrix(
            SrsDto srs,
            List<EstimationUseCase> useCases) {

        if (srs == null
                || srs.functionalRequirements() == null
                || srs.functionalRequirements().isEmpty()) {

            return srs;
        }

        List<TraceabilityMatrixEntryDto> existingEntries =
                srs.requirementsTraceabilityMatrix();

        Map<String, TraceabilityMatrixEntryDto> existingByRequirementId =
                new HashMap<>();

        if (existingEntries != null) {
            for (TraceabilityMatrixEntryDto entry : existingEntries) {

                if (entry == null
                        || entry.requirementId() == null
                        || entry.requirementId().isBlank()) {
                    continue;
                }

                existingByRequirementId.put(
                        entry.requirementId(),
                        entry
                );
            }
        }

        List<TraceabilityMatrixEntryDto> normalizedEntries =
                new ArrayList<>();

        Set<Long> usedUseCaseIds = new HashSet<>();

        for (int i = 0;
             i < srs.functionalRequirements().size();
             i++) {

            FunctionalRequirementDto requirement =
                    srs.functionalRequirements().get(i);

            if (requirement == null) {
                continue;
            }

            TraceabilityMatrixEntryDto existingEntry =
                    existingByRequirementId.get(
                            requirement.requirementId()
                    );

            EstimationUseCase matchedUseCase =
                    findMatchingUseCase(
                            requirement,
                            useCases,
                            usedUseCaseIds
                    );

            String source;
            String relatedUseCase;

            if (matchedUseCase != null) {

                source =
                        "Estimation Use Case ID: "
                                + safeText(matchedUseCase.getId());

                relatedUseCase =
                        safeText(matchedUseCase.getUseCaseName());

                if (matchedUseCase.getId() != null) {
                    usedUseCaseIds.add(
                            matchedUseCase.getId()
                    );
                }

            } else if (existingEntry != null) {

                source =
                        safeText(existingEntry.source());

                relatedUseCase =
                        safeText(existingEntry.relatedUseCase());

            } else {

                source = "Not specified";
                relatedUseCase = "Not specified";
            }

            String validationMethod =
                    existingEntry != null
                            ? safeText(existingEntry.validationMethod())
                            : "Not specified";

            normalizedEntries.add(
                    new TraceabilityMatrixEntryDto(
                            safeText(requirement.requirementId()),
                            safeText(requirement.description()),
                            source,
                            safeText(requirement.module()),
                            relatedUseCase,
                            validationMethod
                    )
            );
        }

        return new SrsDto(
                srs.documentInformation(),
                srs.introduction(),
                srs.overallProductRequirements(),
                srs.externalInterfaceRequirements(),
                srs.proposedSystemEnvironments(),
                srs.functionalRequirements(),
                srs.nonFunctionalRequirements(),
                srs.dataRequirements(),
                normalizedEntries
        );
    }

    private EstimationUseCase findMatchingUseCase(
            FunctionalRequirementDto requirement,
            List<EstimationUseCase> useCases,
            Set<Long> usedUseCaseIds) {

        if (useCases == null || useCases.isEmpty()) {
            return null;
        }

        String requirementName =
                normalizeText(requirement.requirementName());

        String description =
                normalizeText(requirement.description());

        String module =
                normalizeText(requirement.module());

        /*
         * First try to match the generated functional requirement
         * with an estimation use case by name.
         */
        for (EstimationUseCase useCase : useCases) {

            if (useCase == null
                    || isUseCaseAlreadyUsed(
                            useCase,
                            usedUseCaseIds)) {
                continue;
            }

            String useCaseName =
                    normalizeText(useCase.getUseCaseName());

            if (useCaseName.isBlank()) {
                continue;
            }

            if (containsMeaningfulMatch(
                    requirementName,
                    useCaseName)
                    || containsMeaningfulMatch(
                    description,
                    useCaseName)
                    || containsMeaningfulMatch(
                    module,
                    useCaseName)) {

                return useCase;
            }
        }

        /*
         * If the names do not match exactly, use the first
         * unused estimation use case as the fallback.
         *
         * This keeps the mapping deterministic while still
         * avoiding invented use-case information.
         */
        for (EstimationUseCase useCase : useCases) {

            if (useCase == null
                    || isUseCaseAlreadyUsed(
                            useCase,
                            usedUseCaseIds)) {
                continue;
            }

            return useCase;
        }

        return null;
    }

    private boolean isUseCaseAlreadyUsed(
            EstimationUseCase useCase,
            Set<Long> usedUseCaseIds) {

        if (useCase.getId() == null) {
            return false;
        }

        return usedUseCaseIds.contains(
                useCase.getId()
        );
    }

    private boolean containsMeaningfulMatch(
            String requirementText,
            String useCaseName) {

        if (requirementText == null
                || requirementText.isBlank()
                || useCaseName == null
                || useCaseName.isBlank()) {

            return false;
        }

        return requirementText.contains(useCaseName)
                || useCaseName.contains(requirementText);
    }

    private String normalizeText(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}