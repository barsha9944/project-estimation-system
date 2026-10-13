package com.projectestimation.backend.dar.controller;

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
import com.projectestimation.backend.dar.dto.DarResponseDto;
import com.projectestimation.backend.dar.service.DarService;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/dar")
public class DarController {

    private final DarService darService;

    public DarController(DarService darService) {
        this.darService = darService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<DarResponseDto>> generateDar(
            @PathVariable Long opportunityId
    ) {
        DarResponseDto response = darService.generateDar(opportunityId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "DAR generated successfully",
                        response
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DarResponseDto>> getDar(
            @PathVariable Long opportunityId
    ) {
        DarResponseDto response = darService.getDar(opportunityId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "DAR retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadDar(
            @PathVariable Long opportunityId
    ) throws IOException {

        DarResponseDto dar = darService.getDar(opportunityId);
        byte[] document = darService.downloadDar(opportunityId);

        String fileName = dar.documentName() == null
                ? "DAR.docx"
                : dar.documentName();

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
                .body(document);
    }
}