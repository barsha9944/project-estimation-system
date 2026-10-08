package com.projectestimation.backend.rtm.ai;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.rtm.dto.RtmRowDto;
import com.projectestimation.backend.rtm.dto.RtmSourceCodeReferenceDto;

@Service
public class GeminiRtmOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(GeminiRtmOrchestrator.class);
    private static final int MAX_OUTPUT_TOKENS = 2048;

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    public GeminiRtmOrchestrator(
            GeminiClient geminiClient,
            ObjectMapper objectMapper,
            ApplicationContext applicationContext
    ) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
    }

    public Map<String, String> determineSourceCodeReferences(List<RtmRowDto> requirements) {
        try {
            String prompt = buildPrompt(requirements);

            log.info("Sending RTM source code reference prompt to Gemini");
            String response = geminiClient.generateJsonContent(prompt, MAX_OUTPUT_TOKENS);

            Map<String, String> mapping = parseResponse(response);
            if (mapping != null && !mapping.isEmpty()) {
                log.info("Successfully determined source code references from Gemini: {}", mapping);
                return mapping;
            }
        } catch (Exception e) {
            log.warn(
                    "Gemini call failed or timed out during RTM source code reference generation: {}. Using graceful fallback.",
                    e.getMessage()
            );
        }

        return getFallbackMapping();
    }

    private String inspectActualControllers() {
        StringBuilder sb = new StringBuilder();
        try {
            Map<String, Object> restControllers = applicationContext.getBeansWithAnnotation(RestController.class);
            List<String> controllerNames = new ArrayList<>(restControllers.keySet());
            Collections.sort(controllerNames);

            int index = 1;
            for (String beanName : controllerNames) {
                Object bean = restControllers.get(beanName);
                Class<?> clazz = bean.getClass();
                // If it's a CGLIB/Spring proxy, get the superclass
                if (clazz.getName().contains("$$")) {
                    clazz = clazz.getSuperclass();
                }

                String simpleName = clazz.getSimpleName() + ".java";
                String mappingPath = "";
                RequestMapping reqMapping = clazz.getAnnotation(RequestMapping.class);
                if (reqMapping != null && reqMapping.value().length > 0) {
                    mappingPath = String.join(", ", reqMapping.value());
                }

                sb.append(index++)
                        .append(". ")
                        .append(simpleName);
                if (!mappingPath.isEmpty()) {
                    sb.append(" - Request Path: ").append(mappingPath);
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.warn("Error inspecting controllers via ApplicationContext: {}", e.getMessage());
        }

        if (sb.length() == 0) {
            // Default known controllers from the project source code
            sb.append("1. AuthController.java - /api/v1/auth (User authentication, login, credentials, token issuance)\n")
              .append("2. CalculationController.java - /api/v1/calculations (Estimation formula calculations)\n")
              .append("3. DashboardController.java - /api/v1/dashboard (Dashboard statistics)\n")
              .append("4. EstimationController.java - /api/v1/estimates (Use case estimation)\n")
              .append("5. MomController.java - /api/v1/mom (Minutes of meeting generation)\n")
              .append("6. OpportunityController.java - /api/v1/opportunities (Opportunity management)\n")
              .append("7. OpportunityEstimationController.java - /api/v1/opportunities/{id}/estimate (Opportunity estimation)\n")
              .append("8. OpportunityProposalController.java - /api/v1/proposals (Opportunity proposals)\n")
              .append("9. ParametersController.java - /api/v1/parameters (Parameters)\n")
              .append("10. PmpController.java - /api/v1/opportunities/{id}/pmp (PMP document generation)\n")
              .append("11. ProjectMetricsController.java - /api/v1/project-metrics (Project data metrics)\n")
              .append("12. ProjectScheduleController.java - /api/v1/opportunities/{id}/schedule (Project schedule)\n")
              .append("13. ProposalController.java - /api/v1/proposals (Proposals)\n")
              .append("14. ProposalDiagramController.java - /api/v1/diagram (Architecture diagrams)\n")
              .append("15. PsrController.java - /api/v1/psr (Project status reports)\n")
              .append("16. SddController.java - /api/v1/opportunities/{id}/sdd (Software Design Document)\n")
              .append("17. SrsController.java - /api/v1/opportunities/{id}/srs (Software Requirements Specification)\n")
              .append("18. TestCaseController.java - /api/v1/opportunities/{id}/test-cases (Test cases)\n");
        }

        return sb.toString();
    }

    private String buildPrompt(List<RtmRowDto> requirements) {
    StringBuilder reqSb = new StringBuilder();

    for (RtmRowDto req : requirements) {
        reqSb.append("- ")
                .append(req.getRequirementId())
                .append(": ")
                .append(req.getRequirementName())
                .append(" - ")
                .append(req.getRequirementDescription())
                .append("\n");
    }

    return """
            You are a senior software architect preparing the Source Code Reference
            column of a Requirements Traceability Matrix (RTM) for the application
            described by the Functional Requirements below.

            Analyze each Functional Requirement independently.

            FUNCTIONAL REQUIREMENTS:
            %s

            SOURCE CODE REFERENCE RULES:

            1. For each functional requirement, PROPOSE the Controller class that
               would implement or handle that specific requirement in the application
               being documented.

            2. The Controller name must be derived from the business responsibility
               and functionality of the requirement.

            3. DO NOT use, inspect, or select controllers from the Project Estimation
               System that is executing this RTM generation.

            4. The Controller name is a PROPOSED source-code reference for the
               application being documented. It does not have to currently exist
               in the Project Estimation System.

            5. For each requirement, also PROPOSE the DAO implementation class
               responsible for the corresponding data-access responsibility.

            6. The DAO implementation name must follow:
               <DomainName>DaoImpl.java

            7. The Controller name must follow:
               <DomainName>Controller.java

            8. Controller and DAOImpl names must be based on the specific
               requirement. Do not use the same generic Controller or DAOImpl
               for unrelated requirements.

            9. The Source Code Reference MUST use exactly this format:

               <ProposedController>.java, <ProposedDaoImpl>.java

            10. Example:

                OpportunityController.java, OpportunityDaoImpl.java

                SupplierController.java, SupplierDaoImpl.java

                BidController.java, BidDaoImpl.java

            11. The Controller and DAOImpl are proposed references only.
                Do NOT claim that these classes already exist.

            12. Do NOT create or modify any source-code classes.

            13. Preserve every requirement ID exactly as provided.

            14. Return one mapping for every functional requirement provided.

            15. Return ONLY a valid JSON array with:
                "requirementId"
                "sourceCodeReference"

            Example:
            [
              {
                "requirementId": "FR-001",
                "sourceCodeReference": "AuthController.java, AuthDaoImpl.java"
              },
              {
                "requirementId": "FR-002",
                "sourceCodeReference": "TenantController.java, TenantDaoImpl.java"
              }
            ]

            Do NOT wrap the JSON inside markdown code blocks.
            """.formatted(reqSb.toString());
}

    private Map<String, String> parseResponse(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        try {
            List<RtmSourceCodeReferenceDto> list = objectMapper.readValue(
                    cleaned,
                    new TypeReference<List<RtmSourceCodeReferenceDto>>() {}
            );

            Map<String, String> map = new HashMap<>();
            for (RtmSourceCodeReferenceDto item : list) {
                if (item.requirementId() != null && item.sourceCodeReference() != null) {
                    map.put(item.requirementId().trim(), item.sourceCodeReference().trim());
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("Failed to parse Gemini RTM response JSON: {}", e.getMessage());
            return null;
        }
    }

//    private Map<String, String> getFallbackMapping() {
//        Map<String, String> fallback = new HashMap<>();
//
//        fallback.put("FR-001", "AuthController.java, UserDaoImpl.java");
//        fallback.put("FR-002", "N.A., OpportunityDaoImpl.java");
//        fallback.put("FR-003", "N.A., EstimationDaoImpl.java");
//        fallback.put("FR-004", "N.A., ProposalDaoImpl.java");
//        fallback.put("FR-005", "N.A., ProjectMetricsDaoImpl.java");
//        fallback.put("FR-006", "N.A., ProjectScheduleDaoImpl.java");
//        fallback.put("FR-007", "N.A., PsrDaoImpl.java");
//        fallback.put("FR-008", "N.A., SrsDaoImpl.java");
//        fallback.put("FR-009", "N.A., SddDaoImpl.java");
//        fallback.put("FR-010", "N.A., TestCaseDaoImpl.java");
//        fallback.put("FR-011", "N.A., UserDaoImpl.java");
//        fallback.put("FR-012", "N.A., OpportunityDaoImpl.java");
//
//        return fallback;
//    }
    
    private Map<String, String> getFallbackMapping() {
        Map<String, String> fallback = new HashMap<>();

        for (int i = 1; i <= 12; i++) {
            fallback.put(
                    String.format("FR-%03d", i),
                    "N.A., N.A."
            );
        }

        return fallback;
    }
}
