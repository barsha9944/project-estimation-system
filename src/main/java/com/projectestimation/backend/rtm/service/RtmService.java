package com.projectestimation.backend.rtm.service;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.rtm.ai.GeminiRtmOrchestrator;
import com.projectestimation.backend.rtm.dto.RtmResponseDto;
import com.projectestimation.backend.rtm.dto.RtmRowDto;
import com.projectestimation.backend.rtm.model.Rtm;
import com.projectestimation.backend.rtm.repository.RtmRepository;
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.service.SddService;
import com.projectestimation.backend.srs.dto.SrsDto;
import com.projectestimation.backend.srs.service.SrsService;
import com.projectestimation.backend.testcase.model.TestCase;
import com.projectestimation.backend.testcase.repository.TestCaseRepository;

@Service
public class RtmService {

    private static final Logger log = LoggerFactory.getLogger(RtmService.class);

    private final OpportunityRepository opportunityRepository;
    private final TestCaseRepository testCaseRepository;
    private final SrsService srsService;
    private final SddService sddService;
    private final SrsSddDocumentParser documentParser;
    private final GeminiRtmOrchestrator geminiRtmOrchestrator;
    private final RtmExcelService rtmExcelService;
    private final RtmRepository rtmRepository;
    private final ObjectMapper objectMapper;

    public RtmService(
            OpportunityRepository opportunityRepository,
            TestCaseRepository testCaseRepository,
            SrsService srsService,
            SddService sddService,
            SrsSddDocumentParser documentParser,
            GeminiRtmOrchestrator geminiRtmOrchestrator,
            RtmExcelService rtmExcelService,
            RtmRepository rtmRepository,
            ObjectMapper objectMapper
    ) {
        this.opportunityRepository = opportunityRepository;
        this.testCaseRepository = testCaseRepository;
        this.srsService = srsService;
        this.sddService = sddService;
        this.documentParser = documentParser;
        this.geminiRtmOrchestrator = geminiRtmOrchestrator;
        this.rtmExcelService = rtmExcelService;
        this.rtmRepository = rtmRepository;
        this.objectMapper = objectMapper;
    }

    public boolean existsByOpportunityId(Long opportunityId) {
        return rtmRepository.existsByOpportunityId(opportunityId);
    }

    public Rtm getByOpportunityId(Long opportunityId) {
        return rtmRepository.findByOpportunityId(opportunityId).orElse(null);
    }

    @Transactional(readOnly = true)
    public RtmResponseDto getRtmData(Long opportunityId) {
        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException("Opportunity not found with id: " + opportunityId);
        }

        Rtm rtm = rtmRepository.findByOpportunityId(opportunityId).orElse(null);
        if (rtm == null || rtm.getRtmData() == null || rtm.getRtmData().isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(rtm.getRtmData(), RtmResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to parse saved RTM data for opportunity id {}: {}", opportunityId, e.getMessage(), e);
            throw new IllegalStateException("Failed to parse saved RTM data: " + e.getMessage(), e);
        }
    }

    @Transactional
    public RtmResponseDto generateRtm(Long opportunityId) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        // 1. Retrieve already-generated SRS belonging to THIS opportunity
        SrsDto srsDto = srsService.getGeneratedSrs(opportunityId);
        if (srsDto == null) {
            throw new ResourceNotFoundException(
                    "SRS document not found for opportunity id: " + opportunityId + ". Please generate the SRS first before generating RTM.");
        }

        // 2. Retrieve already-generated SDD belonging to THIS opportunity
        SddDto sddDto = sddService.getGeneratedSdd(opportunityId);
        if (sddDto == null) {
            throw new ResourceNotFoundException(
                    "SDD document not found for opportunity id: " + opportunityId + ". Please generate the SDD first before generating RTM.");
        }

        // 3. Extract requirements and SDD mappings for THIS opportunity
        List<RtmRowDto> rows = documentParser.parseRequirements(srsDto, sddDto);

        // 4. Identify Source Code References via Gemini analysis of actual project source code
        Map<String, String> sourceCodeReferences = geminiRtmOrchestrator.determineSourceCodeReferences(rows);

        // 5. Obtain existing Test Cases specifically for THIS opportunity
        List<TestCase> existingTestCases = testCaseRepository.findByOpportunityId(opportunityId);

        // 6. Map data into each row
        for (RtmRowDto row : rows) {
            // Source Code Reference from Gemini source code analysis
            String srcRef = sourceCodeReferences.get(row.getRequirementId());
            row.setSourceCodeReference(srcRef != null && !srcRef.isBlank() ? srcRef : "N.A.");

            // Test Case References for this opportunity
            String testCaseRef = findTestCaseReferences(row, existingTestCases);
            row.setUnitTestCaseReference(testCaseRef);
            // System Test Case Reference MUST contain EXACTLY the same reference/value as Unit Test Cases
            row.setSystemTestCaseReference(testCaseRef);

            // Hardcoded Acceptance Test Cases & Status from sample
            row.setAcceptanceTestCaseReference("N.A.");
            row.setStatus("Done");
        }

        RtmResponseDto responseDto = new RtmResponseDto(
                opportunity.getId(),
                opportunity.getOpportunityName(),
                opportunity.getClientName(),
                rows
        );

        // 7. Persist generated RTM into database
        try {
            Rtm rtm = rtmRepository.findByOpportunityId(opportunityId)
                    .orElseGet(() -> Rtm.builder().opportunity(opportunity).build());
            rtm.setOpportunity(opportunity);
            rtm.setRtmData(objectMapper.writeValueAsString(responseDto));
            rtmRepository.save(rtm);
            log.info("Successfully generated and saved RTM for opportunityId: {}", opportunityId);
        } catch (Exception e) {
            log.error("Failed to save RTM data for opportunity id {}: {}", opportunityId, e.getMessage(), e);
            throw new IllegalStateException("Failed to save RTM data: " + e.getMessage(), e);
        }

        return responseDto;
    }

    @Transactional
    public byte[] downloadRtm(Long opportunityId) throws IOException {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with id: " + opportunityId));

        RtmResponseDto response = getRtmData(opportunityId);
        if (response == null) {
            response = generateRtm(opportunityId);
        }

        return rtmExcelService.generateExcel(opportunity, response.rows());
    }

    private String findTestCaseReferences(RtmRowDto row, List<TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return "N.A.";
        }

        int reqNum = extractRequirementNumber(row.getRequirementId());
        String reqIdStr = row.getRequirementId(); // e.g. "FR-001"
        String reqName = row.getRequirementName() != null ? row.getRequirementName().toLowerCase() : "";

        Set<String> matchedTestCaseIds = new LinkedHashSet<>();

        for (TestCase tc : testCases) {
            if (tc == null) {
                continue;
            }

            String tcReqId = tc.getReqId();
            String tcId = tc.getTestCaseId();
            String tcName = tc.getTestCaseName() != null ? tc.getTestCaseName().toLowerCase() : "";

            if (tcId == null || tcId.isBlank()) {
                continue;
            }

            boolean matched = false;

            if (tcReqId != null && !tcReqId.isBlank()) {
                // Check direct equality (e.g. FR-001 == FR-001)
                if (tcReqId.equalsIgnoreCase(reqIdStr)) {
                    matched = true;
                } else {
                    // Extract number from test case reqId (e.g. REQ-001 -> 1, REQ-1 -> 1)
                    int tcNum = extractRequirementNumber(tcReqId);
                    if (tcNum == reqNum && reqNum > 0) {
                        matched = true;
                    }
                }
            }

            // Fallback match on name/description if reqId didn't match
            if (!matched && !reqName.isBlank() && !tcName.isBlank()) {
                if (reqName.contains(tcName) || tcName.contains(reqName)) {
                    matched = true;
                }
            }

            if (matched) {
                matchedTestCaseIds.add(tcId.trim());
            }
        }

        if (matchedTestCaseIds.isEmpty()) {
            return "N.A.";
        }

        return String.join(", ", matchedTestCaseIds);
    }

    private int extractRequirementNumber(String reqId) {
        if (reqId == null) {
            return -1;
        }
        String digits = reqId.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
