package com.projectestimation.backend.rtm.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.rtm.dto.RtmResponseDto;
import com.projectestimation.backend.rtm.service.RtmService;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/rtm")
public class RtmController {

    private final RtmService rtmService;
    private final OpportunityRepository opportunityRepository;

    public RtmController(
            RtmService rtmService,
            OpportunityRepository opportunityRepository
    ) {
        this.rtmService = rtmService;
        this.opportunityRepository = opportunityRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RtmResponseDto>> getRtm(
            @PathVariable Long opportunityId
    ) {
        RtmResponseDto response = rtmService.getRtmData(opportunityId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        response != null ? "RTM retrieved successfully" : "No RTM generated yet",
                        response
                )
        );
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<RtmResponseDto>> generateRtm(
            @PathVariable Long opportunityId
    ) {
        RtmResponseDto response = rtmService.generateRtm(opportunityId);
        return ResponseEntity.ok(
                ApiResponse.success("RTM generated successfully", response)
        );
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadRtm(
            @PathVariable Long opportunityId
    ) throws IOException {
        byte[] excelBytes = rtmService.downloadRtm(opportunityId);

        String opportunityName = opportunityRepository.findById(opportunityId)
                .map(opp -> opp.getOpportunityName())
                .orElse("Requirement_Traceability_Matrix");

        String safeName = opportunityName
                .replaceAll("[\\\\/:*?\"<>|\\s]+", "_")
                .trim();

        if (safeName.isBlank()) {
            safeName = "Requirement_Traceability_Matrix";
        }

        String fileName = safeName + "_RTM.xls";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(
                        MediaType.parseMediaType("application/vnd.ms-excel")
                )
                .body(excelBytes);
    }
}
