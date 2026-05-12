package com.projectestimation.backend.proposal.service;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.estimation.repository.EstimateResultRepository;
import com.projectestimation.backend.proposal.dto.ProposalGenerateRequest;
import com.projectestimation.backend.proposal.dto.ProposalGenerateResponse;
import com.projectestimation.backend.proposal.model.Proposal;
import com.projectestimation.backend.proposal.repository.ProposalRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final EstimateResultRepository estimateResultRepository;

    public ProposalService(ProposalRepository proposalRepository, EstimateResultRepository estimateResultRepository) {
        this.proposalRepository = proposalRepository;
        this.estimateResultRepository = estimateResultRepository;
    }

    public ProposalGenerateResponse generate(ProposalGenerateRequest request, User user) {
        EstimateResult estimateResult = estimateResultRepository.findById(request.estimateId())
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

        String summary = buildSummary(request, estimateResult);
        String fileName = "proposal-" + estimateResult.getId() + ".txt";

        Proposal proposal = new Proposal();
        proposal.setEstimateResult(estimateResult);
        proposal.setTitle(request.proposalTitle());
        proposal.setSummaryText(summary);
        proposal.setFileName(fileName);
        proposal.setFileType(MediaType.TEXT_PLAIN_VALUE);
        proposal.setFileContent(summary.getBytes(StandardCharsets.UTF_8));
        proposal.setGeneratedBy(user);

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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(proposal.getFileType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(proposal.getFileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(proposal.getFileContent());
    }

    private String buildSummary(ProposalGenerateRequest request, EstimateResult estimateResult) {
        String notes = request.notes() == null ? "N/A" : request.notes();
        return "Proposal Title: " + request.proposalTitle() + "\n"
                + "Project: " + estimateResult.getProjectName() + "\n"
                + "Total Effort Hours: " + estimateResult.getTotalEffortHours() + "\n"
                + "Estimated Cost: " + estimateResult.getEstimatedCost() + "\n"
                + "Estimated Timeline (weeks): " + estimateResult.getTimelineWeeks() + "\n"
                + "Confidence Score: " + estimateResult.getConfidenceScore() + "\n"
                + "Estimation Breakdown: " + estimateResult.getBreakdown() + "\n"
                + "Additional Notes: " + notes + "\n";
    }
}
