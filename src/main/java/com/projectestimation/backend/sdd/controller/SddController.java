package com.projectestimation.backend.sdd.controller;

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
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.service.SddService;
import com.projectestimation.backend.srs.service.SrsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/sdd")
@RequiredArgsConstructor
public class SddController {

    private final SddService sddService;
    private final SrsService srsService;
    
    @PostMapping("/generate")
    public ResponseEntity<SddDto> generateSdd(
            @PathVariable Long opportunityId) {

        SddDto generatedSdd = sddService.generateSdd(opportunityId);

        return ResponseEntity.ok(generatedSdd);
    }
    
    @GetMapping
    public ResponseEntity<SddDto> getSdd(
            @PathVariable Long opportunityId) {

        SddDto sddDto = sddService.getGeneratedSdd(opportunityId);

        if (sddDto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(sddDto);
    }
    
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadSdd(
            @PathVariable Long opportunityId) throws IOException {

        byte[] document = sddService.downloadSdd(opportunityId);

        Opportunity opportunity = srsService.getOpportunity(opportunityId);

        String fileName = opportunity.getOpportunityName()
                + "_sdd.docx";

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