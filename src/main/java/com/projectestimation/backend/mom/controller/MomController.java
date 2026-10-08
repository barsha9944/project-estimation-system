package com.projectestimation.backend.mom.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.mom.dto.MomDto;
import com.projectestimation.backend.mom.dto.MomGenerateRequest;
import com.projectestimation.backend.mom.dto.MomGenerationResponse;
import com.projectestimation.backend.mom.service.MomService;

@RestController
@RequestMapping("/api/v1/opportunities")
public class MomController {

    private final MomService momService;

    public MomController(
            MomService momService
    ) {
        this.momService = momService;
    }

    @PostMapping("/{opportunityId}/mom/generate")
    public ResponseEntity<ApiResponse<MomGenerationResponse>> generateMoms(
            @PathVariable Long opportunityId,
            @RequestBody MomGenerateRequest request
    ) {

        MomGenerationResponse response =
                momService.generateAllMoms(
                        opportunityId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "MOMs generated successfully",
                        response
                )
        );
    }

    @GetMapping("/{opportunityId}/mom")
    public ResponseEntity<ApiResponse<List<MomDto>>> getMoms(
            @PathVariable Long opportunityId
    ) {

        List<MomDto> response =
                momService.getAllMoms(
                        opportunityId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "MOMs retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/{opportunityId}/mom/{momId}/download")
    public ResponseEntity<byte[]> downloadMom(
            @PathVariable Long opportunityId,
            @PathVariable Long momId
    ) {

        byte[] document =
                momService.downloadMom(
                        momId
                );

        String documentName =
                momService.getMomDocumentName(momId);
        
        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_OCTET_STREAM
        );

        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(documentName)
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(document);
    }
}