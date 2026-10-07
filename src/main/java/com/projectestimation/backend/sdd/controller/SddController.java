package com.projectestimation.backend.sdd.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.service.SddService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/sdd")
@RequiredArgsConstructor
public class SddController {

    private final SddService sddService;
    
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
            @PathVariable Long opportunityId) {

        try {

            byte[] document =
                    sddService.downloadSdd(opportunityId);

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_OCTET_STREAM
            );

            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("SDD.docx")
                            .build()
            );

            headers.setContentLength(
                    document.length
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(document);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .build();
        }
    }
}