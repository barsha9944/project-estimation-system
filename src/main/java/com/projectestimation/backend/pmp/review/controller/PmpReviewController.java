
package com.projectestimation.backend.pmp.review.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.projectestimation.backend.pmp.review.dto.PmpReviewResponseDto;
import com.projectestimation.backend.pmp.review.dto.PmpReviewStatusDto;
import com.projectestimation.backend.pmp.review.entity.PmpReviewDocument;
import com.projectestimation.backend.pmp.review.service.PmpReviewService;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/pmp/review")
public class PmpReviewController {

    private static final MediaType DOCX_MEDIA_TYPE =
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final MediaType XLS_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.ms-excel");

    private final PmpReviewService reviewService;

    public PmpReviewController(PmpReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/schedule")
    public ResponseEntity<PmpReviewResponseDto> schedule(
            @PathVariable Long opportunityId) {

        PmpReviewResponseDto response = reviewService.schedule(
                opportunityId,
                java.time.LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<PmpReviewStatusDto> getStatus(
            @PathVariable Long opportunityId) {

        return ResponseEntity.ok(
                reviewService.getStatus(opportunityId));
    }

    @GetMapping("/documents/{documentType}")
    public ResponseEntity<String> getDocument(
            @PathVariable Long opportunityId,
            @PathVariable String documentType) {

        PmpReviewDocument document = reviewService.getDocument(
                opportunityId,
                validateDocumentType(documentType));

        if (!"GENERATED".equals(document.getStatus())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(document.getDocumentContent());
    }

    /**
     * Downloads review notes as DOCX and defect tracking as XLS.
     */
    @GetMapping("/documents/{documentType}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long opportunityId,
            @PathVariable String documentType) throws IOException {

        String type = validateDocumentType(documentType);

        // Converts the stored generated text to the correct file format.
        byte[] content = reviewService.exportDocument(
                opportunityId, type);

        String extension;
        MediaType mediaType;

        if ("DEFECT_TRACKING".equals(type)) {
            extension = ".xls";
            mediaType = XLS_MEDIA_TYPE;
        } else {
            extension = ".docx";
            mediaType = DOCX_MEDIA_TYPE;
        }

        String filename = "PMP_" + type + "_"
                + opportunityId + extension;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentLength(content.length)
                .body(content);
    }
    

    /**
     * Upload the updated defect tracking sheet after review.
     */
    @PostMapping(
            value = "/defects/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> uploadUpdatedDefectTracking(
            @PathVariable Long opportunityId,
            @RequestParam("file") MultipartFile file)
            throws IOException {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please upload the updated defect tracking sheet.");
        }

        String filename = file.getOriginalFilename();

        if (filename == null
                || !filename.toLowerCase().endsWith(".xls")) {
            return ResponseEntity.badRequest()
                    .body("Please upload an Excel file with .xls format.");
        }

        reviewService.uploadUpdatedDefectTracking(opportunityId, file);

        return ResponseEntity.ok(
                "Updated defect tracking sheet uploaded successfully.");
    }


    private String validateDocumentType(String documentType) {
        if (documentType == null) {
            throw new IllegalArgumentException(
                    "Document type cannot be null.");
        }

        return switch (documentType.toUpperCase()) {
            case "REVIEW_NOTE", "DEFECT_TRACKING", "FOLLOW_UP" ->
                    documentType.toUpperCase();
            default -> throw new IllegalArgumentException(
                    "Unsupported PMP review document type: "
                            + documentType);
        };
    }
}
