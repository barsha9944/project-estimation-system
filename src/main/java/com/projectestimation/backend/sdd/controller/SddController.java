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
import com.projectestimation.backend.review.dto.DocumentReviewStatusDto;
import com.projectestimation.backend.review.model.DocumentReviewType;
import com.projectestimation.backend.review.service.DocumentReviewService;
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
    private final DocumentReviewService documentReviewService;
    
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

    @GetMapping("/reviews/status")
    public ResponseEntity<DocumentReviewStatusDto> getReviewStatus(
            @PathVariable Long opportunityId) {

        DocumentReviewStatusDto status = documentReviewService.getReviewStatus(
                opportunityId,
                DocumentReviewType.SDD
        );

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(status);
    }

    @GetMapping("/reviews/cycle1/download")
    public ResponseEntity<byte[]> downloadReviewCycle1(
            @PathVariable Long opportunityId) throws IOException {

        byte[] docBytes = documentReviewService.downloadCycle1Note(
                opportunityId,
                DocumentReviewType.SDD
        );

        String fileName = documentReviewService.getReview1FileName(
                opportunityId,
                DocumentReviewType.SDD
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(docBytes);
    }

    @GetMapping("/reviews/cycle2/download")
    public ResponseEntity<byte[]> downloadReviewCycle2(
            @PathVariable Long opportunityId) throws IOException {

        byte[] docBytes = documentReviewService.downloadCycle2Note(
                opportunityId,
                DocumentReviewType.SDD
        );

        String fileName = documentReviewService.getReview2FileName(
                opportunityId,
                DocumentReviewType.SDD
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(docBytes);
    }

    @GetMapping("/reviews/datasheet/download")
    public ResponseEntity<byte[]> downloadReviewDataSheet(
            @PathVariable Long opportunityId) throws IOException {

        byte[] sheetBytes = documentReviewService.downloadDataSheet(
                opportunityId,
                DocumentReviewType.SDD
        );

        String fileName = documentReviewService.getDataSheetFileName(
                opportunityId,
                DocumentReviewType.SDD
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(sheetBytes);
    }
}