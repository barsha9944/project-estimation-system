package com.projectestimation.backend.proposal.service;

import com.projectestimation.backend.auth.model.User;
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
import com.projectestimation.backend.proposal.dto.ProposalResponse;
import com.projectestimation.backend.proposal.model.Proposal;
import com.projectestimation.backend.proposal.repository.ProposalRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

@Service
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final EstimateResultRepository estimateResultRepository;
    private final OpportunityRepository opportunityRepository;
    private final ParametersRepository parametersRepository;
    private final GeminiProposalOrchestrator geminiProposalOrchestrator;
    private final ProposalPdfGenerator proposalPdfGenerator;

    public ProposalService(ProposalRepository proposalRepository,
                           EstimateResultRepository estimateResultRepository,
                           OpportunityRepository opportunityRepository,
                           ParametersRepository parametersRepository,
                           GeminiProposalOrchestrator geminiProposalOrchestrator,
                           ProposalPdfGenerator proposalPdfGenerator) {
        this.proposalRepository = proposalRepository;
        this.estimateResultRepository = estimateResultRepository;
        this.opportunityRepository = opportunityRepository;
        this.parametersRepository = parametersRepository;
        this.geminiProposalOrchestrator = geminiProposalOrchestrator;
        this.proposalPdfGenerator = proposalPdfGenerator;
    }

    /**
     * Primary opportunity-driven AI proposal generation workflow.
     */
    @Transactional
    public ProposalResponse generateForOpportunity(Long opportunityId, User user) {
        Opportunity opportunity = loadOpportunity(opportunityId);
        Parameters parameters = loadParameters(opportunityId);
        EstimateResult estimate = loadLatestEstimate(opportunityId);

        AiProposalResult aiResult = geminiProposalOrchestrator.generateProposal(opportunity, parameters, estimate);
        int nextVersion = resolveNextVersion(opportunityId);

        String title = opportunity.getOpportunityName() + " - Proposal v" + nextVersion;
        byte[] pdfContent = proposalPdfGenerator.generate(title, aiResult.proposalContent());

        Proposal proposal = new Proposal();
        proposal.setOpportunity(opportunity);
        proposal.setEstimateResult(estimate);
        proposal.setTitle(title);
        proposal.setProposalContent(aiResult.proposalContent());
        proposal.setSummaryText(aiResult.proposalContent());
        proposal.setGeneratedByAI(true);
        proposal.setVersion(nextVersion);
        proposal.setFileName("proposal-" + opportunityId + "-v" + nextVersion + ".pdf");
        proposal.setFileType(MediaType.APPLICATION_PDF_VALUE);
        proposal.setFileContent(pdfContent);
        proposal.setGeneratedBy(user);

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

        return buildDownloadResponse(proposal);
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
        proposal.setProposalContent(summary);
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

        return buildDownloadResponse(proposal);
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
                saved.getProposalContent(),
                saved.isGeneratedByAI(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                downloadUrl
        );
    }

    private ResponseEntity<byte[]> buildDownloadResponse(Proposal proposal) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(proposal.getFileType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(proposal.getFileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(proposal.getFileContent());
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
}
