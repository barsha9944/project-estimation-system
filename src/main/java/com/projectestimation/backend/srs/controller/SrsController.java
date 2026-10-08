package com.projectestimation.backend.srs.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.srs.dto.SrsDto;
import com.projectestimation.backend.srs.service.SrsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/srs")
@RequiredArgsConstructor
public class SrsController {

    private final SrsService srsService;

    @GetMapping
    public ResponseEntity<SrsDto> getSrs(
            @PathVariable Long opportunityId) {

        SrsDto srs = srsService.getGeneratedSrs(opportunityId);

        if (srs == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(srs);
    }
    
    @PostMapping("/generate")
    public ResponseEntity<SrsDto> generateSrs(
            @PathVariable Long opportunityId) {

        SrsDto generatedSrs = srsService.generateSrs(opportunityId);

        return ResponseEntity.ok(generatedSrs);
    }
    
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadSrs(
            @PathVariable Long opportunityId) throws IOException {

        byte[] document = srsService.downloadSrs(opportunityId);

        Opportunity opportunity = srsService.getOpportunity(opportunityId);

        String fileName = opportunity.getOpportunityName()
                + "_srs.docx";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )
                .body(document);
    }
}