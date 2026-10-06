package com.projectestimation.backend.sdd.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.sdd.ai.GeminiSddOrchestrator;
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.model.Sdd;
import com.projectestimation.backend.sdd.repository.SddRepository;
import com.projectestimation.backend.srs.dto.SrsDto;
import com.projectestimation.backend.srs.service.SrsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SddService {

    private final SddRepository sddRepository;
    
    private final ObjectMapper objectMapper;
    
    private final SrsService srsService;
    
    private final GeminiSddOrchestrator geminiSddOrchestrator;

    public Sdd getByOpportunityId(Long opportunityId) {
        return sddRepository.findByOpportunityId(opportunityId)
                .orElse(null);
    }

    public Sdd save(Sdd sdd) {
        return sddRepository.save(sdd);
    }

    public boolean existsByOpportunityId(Long opportunityId) {
        return sddRepository.existsByOpportunityId(opportunityId);
    }
    
    public SddDto getGeneratedSdd(Long opportunityId) {

        Sdd sdd = sddRepository.findByOpportunityId(opportunityId)
                .orElse(null);

        if (sdd == null) {
            return null;
        }

        try {
            return objectMapper.readValue(
                    sdd.getSddData(),
                    SddDto.class
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse saved SDD data", e
            );
        }
    }
    
    public SrsDto getSrsInput(Long opportunityId) {

        SrsDto srsDto = srsService.getGeneratedSrs(opportunityId);

        if (srsDto == null) {
            throw new ResourceNotFoundException(
                    "SRS not found for this opportunity. Generate SRS before generating SDD."
            );
        }

        return srsDto;
    }
    
    public SddDto generateSdd(Long opportunityId) {

        SrsDto srsDto = getSrsInput(opportunityId);

        try {
            SddDto generatedSdd =
                    geminiSddOrchestrator.generate(srsDto);

            Sdd sdd = sddRepository.findByOpportunityId(opportunityId)
            		.orElseGet(() -> Sdd.builder()
            		        .opportunity(srsService.getOpportunity(opportunityId))
            		        .build());

            sdd.setSddData(
                    objectMapper.writeValueAsString(generatedSdd)
            );

            sddRepository.save(sdd);

            return generatedSdd;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed while generating SDD from Gemini: "
                            + e.getMessage(),
                    e
            );
        }
    }
}