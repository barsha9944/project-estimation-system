package com.projectestimation.backend.dar.review.controller;

import com.projectestimation.backend.dar.review.dto.DarReviewDto;
import com.projectestimation.backend.dar.review.service.DarReviewService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/dar/review")
public class DarReviewController {

    private final DarReviewService darReviewService;

    public DarReviewController(DarReviewService darReviewService) {
        this.darReviewService = darReviewService;
    }

    /**
     * Generate a DAR Review using Gemini and the existing DAR.
     */
    @PostMapping("/ai-generate")
    public ResponseEntity<DarReviewDto> generateReview(
            @PathVariable Long opportunityId
    ) {
        return ResponseEntity.ok(
                darReviewService.generateReview(opportunityId)
        );
    }

    /**
     * Retrieve the saved DAR Review.
     */
    @GetMapping
    public ResponseEntity<DarReviewDto> getReview(
            @PathVariable Long opportunityId
    ) {
        return ResponseEntity.ok(
                darReviewService.getReview(opportunityId)
        );
    }

    /**
     * Save edits to an existing DAR Review.
     */
    @PutMapping
    public ResponseEntity<DarReviewDto> updateReview(
            @PathVariable Long opportunityId,
            @RequestBody DarReviewDto request
    ) {
        return ResponseEntity.ok(
                darReviewService.updateReview(opportunityId, request)
        );
    }

    /**
     * Download the DAR Review as a Word document.
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadReview(
            @PathVariable Long opportunityId
    ) throws IOException {

        byte[] document =
                darReviewService.downloadReview(opportunityId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(
                                "DAR_Review_" + opportunityId + ".docx",
                                StandardCharsets.UTF_8
                        )
                        .build()
        );
        headers.setContentLength(document.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(document);
    }
}

