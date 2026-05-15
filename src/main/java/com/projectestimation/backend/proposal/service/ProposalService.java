package com.projectestimation.backend.proposal.service;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

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

    private String buildSummary(
            ProposalGenerateRequest request,
            EstimateResult estimateResult
    ) {

        String notes =
                request.notes() == null || request.notes().isBlank()
                        ? "No additional notes provided."
                        : request.notes();

        DecimalFormat df = new DecimalFormat("#.##");
        
        StringBuilder proposal = new StringBuilder();

        proposal.append("====================================================\n");
        proposal.append("                PROJECT WORK PROPOSAL               \n");
        proposal.append("====================================================\n\n");

        proposal.append("Proposal Title: ")
                .append(request.proposalTitle())
                .append("\n");

        proposal.append("Project Name: ")
                .append(estimateResult.getProjectName())
                .append("\n");

        proposal.append("Generated On: ")
                .append(java.time.LocalDate.now())
                .append("\n\n");

        proposal.append("====================================================\n");
        proposal.append("EXECUTIVE SUMMARY\n");
        proposal.append("====================================================\n\n");

        proposal.append(
                estimateResult.getRequirementSummary()
        ).append("\n\n");

        proposal.append("====================================================\n");
        proposal.append("PROJECT OVERVIEW\n");
        proposal.append("====================================================\n\n");

        proposal.append(
                "This proposal outlines the estimated effort, "
                + "timeline, and implementation approach for the project.\n\n"
        );

        proposal.append("====================================================\n");
        proposal.append("ESTIMATION SUMMARY\n");
        proposal.append("====================================================\n\n");

        proposal.append("Total Effort Hours: ")
                .append(df.format(estimateResult.getTotalEffortHours()))
                .append("\n");

        proposal.append("Estimated Cost: ")
                .append(estimateResult.getEstimatedCost())
                .append("\n");

        proposal.append("Estimated Timeline (weeks): ")
                .append(df.format(estimateResult.getTimelineWeeks()))
                .append("\n");

        proposal.append("Confidence Score: ")
                .append(df.format(estimateResult.getConfidenceScore()))
                .append("\n\n");

        proposal.append("====================================================\n");
        proposal.append("TECHNICAL BREAKDOWN\n");
        proposal.append("====================================================\n\n");

        String breakdown = estimateResult.getBreakdown();

        if (breakdown != null && !breakdown.isBlank()) {

            String[] items = breakdown.split(",");

            for (String item : items) {

                String[] pair = item.split("=");

                if (pair.length == 2) {

                    String key = pair[0].trim();
                    String value = pair[1].trim();

                    String formattedKey =
                            key.replaceAll("([A-Z])", " $1");

                    formattedKey =
                            formattedKey.substring(0, 1).toUpperCase()
                                    + formattedKey.substring(1);

                    proposal.append(formattedKey)
                            .append(": ")
                            .append(value)
                            .append("\n");
                }
            }
        }

        proposal.append("\n");

        proposal.append("====================================================\n");
        proposal.append("RISKS & ASSUMPTIONS\n");
        proposal.append("====================================================\n\n");

        proposal.append("- Scope changes may impact timeline and effort.\n");
        proposal.append("- Third-party integrations may introduce complexity.\n");
        proposal.append("- Infrastructure and deployment readiness is assumed.\n\n");

        proposal.append("====================================================\n");
        proposal.append("ADDITIONAL NOTES\n");
        proposal.append("====================================================\n\n");

        proposal.append(notes).append("\n\n");

        proposal.append("====================================================\n");
        proposal.append("CONCLUSION\n");
        proposal.append("====================================================\n\n");

        proposal.append(
                "This proposal provides a high-level estimation and "
                + "implementation overview for successful project delivery."
        );

        return proposal.toString();
    }
    
    private byte[] generatePdf(
            ProposalGenerateRequest request,
            EstimateResult estimateResult
    ) {

        try {

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            Document document = new Document();

            PdfWriter.getInstance(document, out);

            document.open();

            Font titleFont =
                    new Font(Font.HELVETICA, 18, Font.BOLD);

            Font headingFont =
                    new Font(Font.HELVETICA, 14, Font.BOLD);

            Font bodyFont =
                    new Font(Font.HELVETICA, 12);

            document.add(
                    new Paragraph(
                            "PROJECT WORK PROPOSAL",
                            titleFont
                    )
            );

            document.add(new Paragraph(" "));

            document.add(
                    new Paragraph(
                            "Proposal Title: "
                                    + request.proposalTitle(),
                            bodyFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Project Name: "
                                    + estimateResult.getProjectName(),
                            bodyFont
                    )
            );

            document.add(new Paragraph(" "));

            document.add(
                    new Paragraph(
                            "EXECUTIVE SUMMARY",
                            headingFont
                    )
            );

            document.add(
                    new Paragraph(
                            estimateResult.getRequirementSummary(),
                            bodyFont
                    )
            );

            document.add(new Paragraph(" "));

            document.add(
                    new Paragraph(
                            "ESTIMATION SUMMARY",
                            headingFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Total Effort Hours: "
                                    + estimateResult.getTotalEffortHours(),
                            bodyFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Estimated Cost: "
                                    + estimateResult.getEstimatedCost(),
                            bodyFont
                    )
            );

            document.add(
                    new Paragraph(
                            "Timeline (weeks): "
                                    + estimateResult.getTimelineWeeks(),
                            bodyFont
                    )
            );

            document.add(new Paragraph(" "));

            document.add(
                    new Paragraph(
                            "TECHNICAL BREAKDOWN",
                            headingFont
                    )
            );

            document.add(
                    new Paragraph(
                            estimateResult.getBreakdown(),
                            bodyFont
                    )
            );

            document.add(new Paragraph(" "));

            document.add(
                    new Paragraph(
                            "RISKS & ASSUMPTIONS",
                            headingFont
                    )
            );

            document.add(
                    new Paragraph(
                            "- Scope changes may impact timeline.\n"
                                    + "- Third-party integrations may introduce complexity.\n"
                                    + "- Infrastructure readiness is assumed.",
                            bodyFont
                    )
            );

            document.add(new Paragraph(" "));

            document.add(
                    new Paragraph(
                            "CONCLUSION",
                            headingFont
                    )
            );

            document.add(
                    new Paragraph(
                            "This proposal provides a high-level estimation and implementation overview for successful project delivery.",
                            bodyFont
                    )
            );

            document.close();

            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate PDF proposal",
                    e
            );
        }
    }
}
