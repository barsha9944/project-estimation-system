package com.projectestimation.backend.proposal.service;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.enums.ProposalType;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.estimation.repository.EstimateResultRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.parameters.model.Parameters;
import com.projectestimation.backend.parameters.repository.ParametersRepository;
import com.projectestimation.backend.proposal.ai.AiProposalResult;
import com.projectestimation.backend.proposal.ai.GeminiProposalOrchestrator;
import com.projectestimation.backend.proposal.dto.ProposalGenerateRequest;
import com.projectestimation.backend.proposal.dto.ProposalGenerateResponse;
import com.projectestimation.backend.proposal.dto.ProposalListResponse;
import com.projectestimation.backend.proposal.dto.ProposalResponse;
import com.projectestimation.backend.proposal.model.Proposal;
import com.projectestimation.backend.proposal.repository.ProposalRepository;

@Service
public class ProposalService {

    private static final String DOCX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final ProposalRepository proposalRepository;
    private final EstimateResultRepository estimateResultRepository;
    private final OpportunityRepository opportunityRepository;
    private final ParametersRepository parametersRepository;
    private final GeminiProposalOrchestrator geminiProposalOrchestrator;
    private final PandocDocxConverter pandocDocxConverter;

    public ProposalService(ProposalRepository proposalRepository,
                           EstimateResultRepository estimateResultRepository,
                           OpportunityRepository opportunityRepository,
                           ParametersRepository parametersRepository,
                           GeminiProposalOrchestrator geminiProposalOrchestrator,
                           PandocDocxConverter pandocDocxConverter) {
        this.proposalRepository = proposalRepository;
        this.estimateResultRepository = estimateResultRepository;
        this.opportunityRepository = opportunityRepository;
        this.parametersRepository = parametersRepository;
        this.geminiProposalOrchestrator = geminiProposalOrchestrator;
        this.pandocDocxConverter = pandocDocxConverter;
    }

    /**
     * Primary opportunity-driven AI proposal generation workflow.
     * Gemini produces Markdown; DOCX is generated on download via Pandoc.
     */
    @Transactional
    public ProposalResponse generateForOpportunity(Long opportunityId, ProposalType proposalType, User user) {
        Opportunity opportunity = loadOpportunity(opportunityId);
        Parameters parameters = loadParameters(opportunityId);
        EstimateResult estimate = loadLatestEstimate(opportunityId);

        int nextVersion = resolveNextVersion(opportunityId);

        String title = opportunity.getOpportunityName() + " - Proposal v" + nextVersion;
        String fileBaseName = "proposal-" + opportunityId + "-v" + nextVersion;
        
        AiProposalResult aiResult = geminiProposalOrchestrator.generateProposal(opportunity, parameters, estimate,  proposalType, fileBaseName);
        

        Proposal proposal = new Proposal();
        proposal.setOpportunity(opportunity);
        proposal.setEstimateResult(estimate);
        proposal.setTitle(title);
        proposal.setMarkdownContent(aiResult.markdownContent());
        proposal.setArchitectureHtml(
                aiResult.architectureHtml()
        );

        proposal.setProcessFlowHtml(
                String.join(
                        "\n---FLOW---\n",
                        aiResult.processFlowHtmls()
                )
        );
        proposal.setSummaryText(aiResult.markdownContent());
        proposal.setGeneratedByAI(true);
        proposal.setVersion(nextVersion);
        proposal.setFileName(fileBaseName + ".docx");
        proposal.setFileType(DOCX_MEDIA_TYPE);
        proposal.setGeneratedBy(user);
        proposal.setProposalType(proposalType);

        Proposal saved = proposalRepository.save(proposal);

        opportunity.setStatus(OpportunityStatus.PROPOSAL_GENERATED);
        opportunityRepository.save(opportunity);

        return toResponse(saved);
    }

    public ProposalResponse getLatestProposalByOpportunityId(Long opportunityId) {
        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException("Opportunity not found");
        }

        Proposal proposal = proposalRepository.findFirstByOpportunity_IdOrderByVersionDesc(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found for this opportunity"));

        return toResponse(proposal);
    }

    public ResponseEntity<byte[]> downloadLatestByOpportunityId(Long opportunityId) {
        Proposal proposal = proposalRepository.findFirstByOpportunity_IdOrderByVersionDesc(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found for this opportunity"));

        return buildDocxDownloadResponse(proposal);
    }

    /**
     * Legacy estimate-driven proposal generation (backward compatibility).
     */
    public ProposalGenerateResponse generate(ProposalGenerateRequest request, User user) {
        EstimateResult estimateResult = estimateResultRepository.findById(request.estimateId())
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

        String summary = buildLegacySummary(request, estimateResult);
        String fileName = "proposal-" + estimateResult.getId() + ".txt";

        Proposal proposal = new Proposal();
        proposal.setEstimateResult(estimateResult);
        proposal.setTitle(request.proposalTitle());
        proposal.setMarkdownContent(summary);
        proposal.setSummaryText(summary);
        proposal.setFileName(fileName);
        proposal.setFileType(MediaType.TEXT_PLAIN_VALUE);
        proposal.setFileContent(summary.getBytes(StandardCharsets.UTF_8));
        proposal.setGeneratedByAI(false);
        proposal.setVersion(1);
        proposal.setGeneratedBy(user);

        if (estimateResult.getOpportunity() != null) {
            proposal.setOpportunity(estimateResult.getOpportunity());
        }

        Proposal saved = proposalRepository.save(proposal);

        return new ProposalGenerateResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getGeneratedAt(),
                "/api/v1/proposals/" + saved.getId() + "/download"
        );
    }

    public ResponseEntity<byte[]> download(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (proposal.isGeneratedByAI()) {
            return buildDocxDownloadResponse(proposal);
        }

        return buildLegacyDownloadResponse(proposal);
    }

    private ResponseEntity<byte[]> buildDocxDownloadResponse(Proposal proposal) {
        if (proposal.getMarkdownContent() == null || proposal.getMarkdownContent().isBlank()) {
            throw new ProposalFailedException("Proposal Markdown content is not available for conversion");
        }

        String baseFileName = proposal.getFileName() != null
                ? proposal.getFileName().replace(".docx", "")
                : "proposal-" + proposal.getId() + "-v" + proposal.getVersion();

        PandocDocxConverter.ConversionResult conversion = pandocDocxConverter.convertMarkdownToDocx(
                proposal.getMarkdownContent(),
                baseFileName,
                proposal.getArchitectureHtml(),
                java.util.Arrays.asList(
                        proposal.getProcessFlowHtml()
                                .split("\n---FLOW---\n")
                )
        );

        proposal.setGeneratedDocPath(conversion.generatedDocPath());
        proposalRepository.save(proposal);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(DOCX_MEDIA_TYPE));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(baseFileName + ".docx")
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(conversion.docxBytes());
    }

    private ResponseEntity<byte[]> buildLegacyDownloadResponse(Proposal proposal) {
        if (proposal.getFileContent() == null) {
            throw new ProposalFailedException("Proposal file content is not available");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(proposal.getFileType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(proposal.getFileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(proposal.getFileContent());
    }

    private Opportunity loadOpportunity(Long opportunityId) {
        return opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }

    private Parameters loadParameters(Long opportunityId) {
        return parametersRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Parameters not found for this opportunity"));
    }

    private EstimateResult loadLatestEstimate(Long opportunityId) {
        return estimateResultRepository.findFirstByOpportunity_IdOrderByCreatedAtDesc(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found for this opportunity"));
    }

    private int resolveNextVersion(Long opportunityId) {
        return proposalRepository.findFirstByOpportunity_IdOrderByVersionDesc(opportunityId)
                .map(proposal -> proposal.getVersion() + 1)
                .orElse(1);
    }

    private ProposalResponse toResponse(Proposal saved) {
        Long opportunityId = saved.getOpportunity() != null ? saved.getOpportunity().getId() : null;
        String downloadUrl = opportunityId != null
                ? "/api/v1/opportunities/" + opportunityId + "/proposal/download"
                : "/api/v1/proposals/" + saved.getId() + "/download";

        return new ProposalResponse(
                saved.getId(),
                opportunityId,
                saved.getVersion(),
                saved.getTitle(),
                saved.getMarkdownContent(),
                saved.isGeneratedByAI(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                downloadUrl
        );
    }

    private String buildLegacySummary(ProposalGenerateRequest request, EstimateResult estimateResult) {
        String notes = request.notes() == null || request.notes().isBlank()
                ? "No additional notes provided."
                : request.notes();

        DecimalFormat df = new DecimalFormat("#.##");
        StringBuilder proposal = new StringBuilder();

        proposal.append("====================================================\n");
        proposal.append("                PROJECT WORK PROPOSAL               \n");
        proposal.append("====================================================\n\n");
        proposal.append("Proposal Title: ").append(request.proposalTitle()).append("\n");
        proposal.append("Project Name: ").append(estimateResult.getProjectName()).append("\n");
        proposal.append("Generated On: ").append(java.time.LocalDate.now()).append("\n\n");
        proposal.append("EXECUTIVE SUMMARY\n\n");
        proposal.append(estimateResult.getRequirementSummary()).append("\n\n");
        proposal.append("ESTIMATION SUMMARY\n\n");
        proposal.append("Total Effort Hours: ").append(df.format(estimateResult.getTotalEffortHours())).append("\n");
        proposal.append("Estimated Cost: ").append(estimateResult.getEstimatedCost()).append("\n");
        proposal.append("Estimated Timeline (weeks): ").append(df.format(estimateResult.getTimelineWeeks())).append("\n");
        proposal.append("Confidence Score: ").append(df.format(estimateResult.getConfidenceScore())).append("\n\n");
        proposal.append("ADDITIONAL NOTES\n\n").append(notes).append("\n");

        return proposal.toString();
    }
    
    @Transactional(readOnly = true)
    public List<ProposalListResponse> getAllProposals() {

    	List<Proposal> proposals = proposalRepository.findAllByOrderByCreatedAtDesc();
    	
        return proposals
                .stream()
                .map(this::toListResponse)
                .toList();
    }
    
    private ProposalListResponse toListResponse(
            Proposal proposal
    ) {

        return new ProposalListResponse(

                proposal.getId(),

                proposal.getOpportunity().getId(),

                proposal.getTitle(),

                proposal.getOpportunity().getClientName(),

                proposal.getProposalType() != null
                        ? proposal.getProposalType().name()
                        : "UNKNOWN",

                proposal.getCreatedAt()
        );
    }
    
    @Transactional(readOnly = true)
    public ProposalResponse getProposal(
            Long proposalId
    ) {

        Proposal proposal =
                proposalRepository.findById(proposalId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Proposal not found: " + proposalId
                                )
                        );

        return toResponse(proposal);
    }
    
    
    public ResponseEntity<byte[]> downloadByProposalId(Long proposalId) {

        Proposal proposal = proposalRepository
                .findById(proposalId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Proposal not found"
                        ));

        return buildDocxDownloadResponse(proposal);
    }
}
