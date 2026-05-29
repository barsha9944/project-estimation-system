package com.projectestimation.backend.proposal.controller;

import java.nio.file.Path;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.proposal.dto.DiagramGenerationResponse;
import com.projectestimation.backend.proposal.service.GeminiDiagramGenerationService;
import com.projectestimation.backend.proposal.service.HtmlToImageRenderer;

@RestController
@RequestMapping("/api/v1/diagram")
public class ProposalDiagramController {

    private final GeminiDiagramGenerationService diagramService;
    private final OpportunityRepository opportunityRepository;
    private final HtmlToImageRenderer htmlToImageRenderer;

    public ProposalDiagramController(
            GeminiDiagramGenerationService diagramService,
            OpportunityRepository opportunityRepository,
            HtmlToImageRenderer htmlToImageRenderer
    ) {
        this.diagramService = diagramService;
        this.opportunityRepository = opportunityRepository;
        this.htmlToImageRenderer = htmlToImageRenderer;
    }

    @GetMapping("/{opportunityId}/architecture")
    public ResponseEntity<ApiResponse<DiagramGenerationResponse>>
    generateArchitecture(
            @PathVariable Long opportunityId
    ) {

        Opportunity opportunity =
                opportunityRepository.findById(opportunityId)
                        .orElseThrow();

        String html =
                diagramService.generateSolutionArchitectureHtml(
                        opportunity
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Architecture diagram generated successfully",
                        new DiagramGenerationResponse(html)
                )
        );
    }

    @GetMapping("/{opportunityId}/process-flow")
    public ResponseEntity<ApiResponse<DiagramGenerationResponse>>
    generateProcessFlow(
            @PathVariable Long opportunityId
    ) {

        Opportunity opportunity =
                opportunityRepository.findById(opportunityId)
                        .orElseThrow();

        String html =
                diagramService.generateProcessFlowHtml(
                        opportunity
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Process flow generated successfully",
                        new DiagramGenerationResponse(html)
                )
        );
    }
    
    @GetMapping("/test-image")
    public String testImage() {

        String html = """
            <div style="
                display:flex;
                gap:30px;
                align-items:center;
                padding:50px;
                font-family:Arial;
            ">

                <div style="
                    padding:30px;
                    background:#4A90E2;
                    color:white;
                    border-radius:12px;
                ">
                    React Frontend
                </div>

                <div style="font-size:40px">
                    →
                </div>

                <div style="
                    padding:30px;
                    background:#50E3C2;
                    color:white;
                    border-radius:12px;
                ">
                    Spring Boot APIs
                </div>

                <div style="font-size:40px">
                    →
                </div>

                <div style="
                    padding:30px;
                    background:#F5A623;
                    color:white;
                    border-radius:12px;
                ">
                    MySQL Database
                </div>

            </div>
            """;

        try {

            htmlToImageRenderer.renderHtmlToImage(
                    html,
                    Path.of("test-architecture.png")
            );

            return "Image generated successfully";

        } catch (Exception ex) {

            return ex.getMessage();
        }
    }
}