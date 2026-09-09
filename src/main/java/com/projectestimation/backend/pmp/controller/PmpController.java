package com.projectestimation.backend.pmp.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.pmp.dto.PmpDto;
import com.projectestimation.backend.pmp.dto.PmpGenerationResponse;
import com.projectestimation.backend.pmp.service.PmpService;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/pmp")
public class PmpController {

    private final PmpService pmpService;

    public PmpController(PmpService pmpService) {
        this.pmpService = pmpService;
    }

    /**
     * Generate PMP using Gemini
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<PmpGenerationResponse>> generatePmp(
            @PathVariable Long opportunityId) {

        PmpGenerationResponse response =
                pmpService.generatePmp(opportunityId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "PMP generated successfully",
                        response
                )
        );
    }

    /**
     * Get existing PMP
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PmpGenerationResponse>> getPmp(
            @PathVariable Long opportunityId) {

        PmpGenerationResponse response =
                pmpService.getPmp(opportunityId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "PMP retrieved successfully",
                        response
                )
        );
    }

    /**
     * Save / update PMP
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<PmpGenerationResponse>> savePmp(
            @PathVariable Long opportunityId,
            @RequestBody PmpDto request) {

        PmpGenerationResponse response =
                pmpService.savePmp(
                        opportunityId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "PMP saved successfully",
                        response
                )
        );
    }

    /**
     * Download PMP as Word document
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadPmp(
            @PathVariable Long opportunityId) throws IOException {

        byte[] document =
                pmpService.downloadPmp(opportunityId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"PMP.docx\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                )
                .body(document);
    }
}