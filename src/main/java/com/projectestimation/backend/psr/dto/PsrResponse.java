package com.projectestimation.backend.psr.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PsrResponse {

    private Long id;

    private String fileName;

    private String fileLocation;

    private LocalDateTime generatedAt;

    private String status;

    private String markdownContent;
}