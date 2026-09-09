package com.projectestimation.backend.testcase.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.testcase.dto.SaveTestCaseRequest;
import com.projectestimation.backend.testcase.dto.TestCaseGenerationResponse;
import com.projectestimation.backend.testcase.service.TestCaseService;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final OpportunityRepository opportunityRepository;

    public TestCaseController(
            TestCaseService testCaseService,
            OpportunityRepository opportunityRepository) {

        this.testCaseService = testCaseService;
        this.opportunityRepository = opportunityRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<TestCaseGenerationResponse>> generate(
            @PathVariable Long opportunityId) {

        TestCaseGenerationResponse response =
                testCaseService.generateTestCases(opportunityId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test cases generated successfully",
                        response
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TestCaseGenerationResponse>> getTestCases(
            @PathVariable Long opportunityId) {

        TestCaseGenerationResponse response =
                testCaseService.getTestCases(opportunityId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test cases retrieved successfully",
                        response
                )
        );
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<TestCaseGenerationResponse>> saveTestCases(
            @PathVariable Long opportunityId,
            @RequestBody SaveTestCaseRequest request) {

        TestCaseGenerationResponse response =
                testCaseService.saveTestCases(
                        opportunityId,
                        request.testCases()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test cases saved successfully",
                        response
                )
        );
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadTestCases(
            @PathVariable Long opportunityId) throws IOException {

        byte[] excel =
                testCaseService.downloadTestCases(opportunityId);

        String opportunityName =
                opportunityRepository.findById(opportunityId)
                        .map(opportunity -> opportunity.getOpportunityName())
                        .orElse("TestCases");

        /*
         * Remove characters that are not allowed
         * in Windows/Linux/macOS filenames.
         */
        String fileName = opportunityName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .trim();

        if (fileName.isBlank()) {
            fileName = "TestCases";
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + ".xlsx\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excel);
    }
}