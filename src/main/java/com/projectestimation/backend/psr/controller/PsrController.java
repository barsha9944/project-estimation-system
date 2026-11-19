package com.projectestimation.backend.psr.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.psr.dto.PsrResponse;
import com.projectestimation.backend.psr.service.PsrService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/opportunities/{opportunityId}/psr")
public class PsrController {

    private final PsrService psrService;

    @PostMapping("/generate")
    public ResponseEntity<List<PsrResponse>> generatePsrs(
            @PathVariable Long opportunityId) {

        return ResponseEntity.ok(
                psrService.generateAllPsrs(opportunityId)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PsrResponse>>> getPsrs(
            @PathVariable Long opportunityId) {

        List<PsrResponse> responses = psrService.getPsrs(opportunityId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "PSRs retrieved successfully",
                        responses
                )
        );
    }

    @GetMapping("/{psrId}/download")
    public ResponseEntity<byte[]> downloadPsr(
            @PathVariable Long opportunityId,
            @PathVariable Long psrId) throws IOException {

        byte[] docBytes = psrService.downloadPsr(opportunityId, psrId);
        String fileName = psrService.getPsrFileName(psrId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                )
                .body(docBytes);
    }
}