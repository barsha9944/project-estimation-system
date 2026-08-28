package com.projectestimation.backend.testcase.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.testcase.dto.SaveTestCaseRequest;
import com.projectestimation.backend.testcase.dto.TestCaseGenerationResponse;
import com.projectestimation.backend.testcase.service.TestCaseService;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<TestCaseGenerationResponse>> generate(
            @PathVariable Long opportunityId
    ) {

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
            @PathVariable Long opportunityId
    ) {

        TestCaseGenerationResponse response =
                testCaseService.getTestCases(opportunityId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test cases retrieved successfully",
                        response
                )
        );
    }

    @PutMapping
    public ResponseEntity<ApiResponse<TestCaseGenerationResponse>> saveTestCases(
            @PathVariable Long opportunityId,
            @RequestBody SaveTestCaseRequest request
    ) {

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
}