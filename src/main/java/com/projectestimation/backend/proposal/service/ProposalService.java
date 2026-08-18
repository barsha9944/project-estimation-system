package com.projectestimation.backend.proposal.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;

import java.util.Arrays;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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
import com.projectestimation.backend.estimation.ai.GeminiEstimationClient;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.parameters.model.Parameters;
import com.projectestimation.backend.parameters.repository.ParametersRepository;
import com.projectestimation.backend.proposal.ai.AiProposalResult;
import com.projectestimation.backend.proposal.ai.GeminiProposalOrchestrator;
import com.projectestimation.backend.proposal.dto.OpportunityProposalDto;
import com.projectestimation.backend.proposal.dto.ProposalDetailWithCountDto;
import com.projectestimation.backend.proposal.dto.ProposalResponse;
import com.projectestimation.backend.proposal.model.Proposal;
import com.projectestimation.backend.proposal.repository.ProposalRepository;

@Service
public class ProposalService {
	private static final Logger log = LogManager.getLogger(ProposalService.class);
	private static final String DOCX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

	@Value("${proposal.storage.path}")
	private String proposalStoragePath;

	private final ProposalRepository proposalRepository;
	private final EstimationAnalysisRepository estimationAnalysisRepository;
	private final OpportunityRepository opportunityRepository;
	private final ParametersRepository parametersRepository;
	private final GeminiProposalOrchestrator geminiProposalOrchestrator;
	private final PandocDocxConverter pandocDocxConverter;

	public ProposalService(ProposalRepository proposalRepository,
			EstimationAnalysisRepository estimationAnalysisRepository, OpportunityRepository opportunityRepository,
			ParametersRepository parametersRepository, GeminiProposalOrchestrator geminiProposalOrchestrator,
			PandocDocxConverter pandocDocxConverter) {
		this.proposalRepository = proposalRepository;
		this.estimationAnalysisRepository = estimationAnalysisRepository;
		this.opportunityRepository = opportunityRepository;
		this.parametersRepository = parametersRepository;
		this.geminiProposalOrchestrator = geminiProposalOrchestrator;
		this.pandocDocxConverter = pandocDocxConverter;
	}

	/**
	 * Primary opportunity-driven AI proposal generation workflow. Gemini produces
	 * Markdown; DOCX is generated on download via Pandoc.
	 */
	@Transactional
	public ProposalResponse generateForOpportunity(Long opportunityId, ProposalType proposalType, User user) {
		Opportunity opportunity = loadOpportunity(opportunityId);
//		Parameters parameters = loadParameters(opportunityId);
		EstimationAnalysis analysis = loadEstimationAnalysis(opportunityId);

		int nextVersion = resolveNextVersion(opportunityId);

		String title = opportunity.getOpportunityName() + " - Proposal v" + nextVersion;
		String fileBaseName = "proposal-" + opportunity.getOpportunityName() + "-v" + nextVersion;

		Path proposalDir = Paths.get(proposalStoragePath, "opportunity-" + opportunityId, "proposal-v" + nextVersion);

		try {
			Files.createDirectories(proposalDir);
		} catch (IOException e) {
			throw new ProposalFailedException("Failed to create proposal directory", e);
		}

		AiProposalResult aiResult = geminiProposalOrchestrator.generateProposal(opportunity, analysis,
				proposalType, fileBaseName);

		Path markdownFile = proposalDir.resolve("proposal.md");

		try {
			Files.writeString(markdownFile, aiResult.markdownContent());
		} catch (IOException e) {
			throw new ProposalFailedException("Failed to write proposal markdown file", e);
		}

		pandocDocxConverter.generateProposalImages(aiResult.architectureHtml(), aiResult.processFlowHtmls(),
				proposalDir, fileBaseName);

		Proposal proposal = new Proposal();
		proposal.setOpportunity(opportunity);
		proposal.setEstimationAnalysis(analysis);
		proposal.setTitle(title);
		proposal.setMarkdownContent(aiResult.markdownContent());
		proposal.setArchitectureHtml(aiResult.architectureHtml());

		proposal.setProcessFlowHtml(String.join("\n---FLOW---\n", aiResult.processFlowHtmls()));
		proposal.setSummaryText(aiResult.markdownContent());
		proposal.setGeneratedByAI(true);
		proposal.setVersion(nextVersion);
		proposal.setFileName(fileBaseName + ".docx");
		proposal.setFileType(DOCX_MEDIA_TYPE);
		proposal.setGeneratedBy(user);
		proposal.setProposalType(proposalType);

		proposal.setMarkdownFilePath(markdownFile.toString());

		proposal.setProposalDirectory(proposalDir.toString());

		proposal.setArchitectureImagePath(proposalDir.resolve(fileBaseName + "-architecture.png").toString());

		proposal.setProcessFlowDirectory(proposalDir.toString());
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

	@Transactional(readOnly = true)
	public ResponseEntity<byte[]> downloadLatestByOpportunityId(Long opportunityId) {
		Proposal proposal = proposalRepository.findFirstByOpportunity_IdOrderByVersionDesc(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Proposal not found for this opportunity"));

		return buildDocxDownloadResponse(proposal);
	}

	/**
	 * Legacy estimate-driven proposal generation (backward compatibility).
	 */

	public ResponseEntity<byte[]> download(Long proposalId) {
		Proposal proposal = proposalRepository.findById(proposalId)
				.orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

		return buildDocxDownloadResponse(proposal);
	}

	private ResponseEntity<byte[]> buildDocxDownloadResponse(Proposal proposal) {

		if (proposal.getMarkdownContent() == null || proposal.getMarkdownContent().isBlank()) {
			throw new ProposalFailedException("Proposal Markdown content is not available for conversion");
		}

//        String baseFileName = proposal.getFileName() != null
//                ? proposal.getFileName().replace(".docx", "")
//                : "proposal-" + proposal.getId() + "-v" + proposal.getVersion();

//        String baseFileName =
//                proposal.getOpportunity()
//                        .getOpportunityName()
//                        .replaceAll("[^a-zA-Z0-9\\s-]", "")
//                        .trim()
//                        .replace(" ", "_")
//                + "_Proposal_v"
//                + proposal.getVersion();

		String baseFileName = proposal.getFileName().replace(".docx", "");

		String markdown = proposal.getMarkdownContent();

		if (proposal.getMarkdownFilePath() != null && !proposal.getMarkdownFilePath().isBlank()) {

			try {
				markdown = Files.readString(Path.of(proposal.getMarkdownFilePath()));
			} catch (IOException e) {
				throw new ProposalFailedException("Failed to read proposal markdown file", e);
			}
		}

//<<<<<<< HEAD
//		PandocDocxConverter.ConversionResult conversion = pandocDocxConverter.convertMarkdownToDocx(markdown,
//				baseFileName, proposal.getArchitectureHtml(),
//				java.util.Arrays.asList(proposal.getProcessFlowHtml().split("\n---FLOW---\n")));
//=======
		log.info("DOCX MARKDOWN");
		log.info(markdown);

		List<String> processFlows = proposal.getProcessFlowHtml() == null || proposal.getProcessFlowHtml().isBlank()
				? List.of()
				: Arrays.asList(proposal.getProcessFlowHtml().split("\n---FLOW---\n"));

		PandocDocxConverter.ConversionResult conversion = pandocDocxConverter.convertMarkdownToDocx(markdown,
				baseFileName, proposal.getArchitectureHtml(), processFlows);
//>>>>>>> 684294120c02f361669b6c3421b02dda460cdbb2

		proposal.setGeneratedDocPath(conversion.generatedDocPath());
		proposalRepository.save(proposal);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(DOCX_MEDIA_TYPE));
		headers.setContentDisposition(ContentDisposition.attachment().filename(baseFileName + ".docx").build());

		return ResponseEntity.ok().headers(headers).body(conversion.docxBytes());
	}

	private Opportunity loadOpportunity(Long opportunityId) {
		return opportunityRepository.findById(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
	}

	private Parameters loadParameters(Long opportunityId) {
		return parametersRepository.findByOpportunityId(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Parameters not found for this opportunity"));
	}

	private EstimationAnalysis loadEstimationAnalysis(Long opportunityId) {

		return estimationAnalysisRepository.findByOpportunityId(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Estimation Analysis not found for this opportunity"));

	}

	private int resolveNextVersion(Long opportunityId) {
		return proposalRepository.findFirstByOpportunity_IdOrderByVersionDesc(opportunityId)
				.map(proposal -> proposal.getVersion() + 1).orElse(1);
	}

	private ProposalResponse toResponse(Proposal saved) {
		Long opportunityId = saved.getOpportunity() != null ? saved.getOpportunity().getId() : null;
		String downloadUrl = opportunityId != null ? "/api/v1/opportunities/" + opportunityId + "/proposal/download"
				: "/api/v1/proposals/" + saved.getId() + "/download";

		return new ProposalResponse(saved.getId(), opportunityId, saved.getVersion(), saved.getTitle(),
				loadMarkdownContent(saved), saved.getArchitectureHtml(), saved.isGeneratedByAI(), saved.getCreatedAt(),
				saved.getUpdatedAt(), downloadUrl);
	}

	private String loadMarkdownContent(Proposal proposal) {

		try {

			if (proposal.getMarkdownFilePath() != null && !proposal.getMarkdownFilePath().isBlank()) {

				String markdown = Files.readString(Path.of(proposal.getMarkdownFilePath()));

				markdown = markdown.replace("assets/images/", "http://localhost:8080/api/v1/opportunities/"
						+ proposal.getOpportunity().getId() + "/proposal/" + proposal.getId() + "/images/");

				return markdown;
			}

			return proposal.getMarkdownContent();

		} catch (Exception ex) {

			throw new ProposalFailedException("Failed to read markdown file", ex);
		}
	}

	@Transactional(readOnly = true)
	public List<OpportunityProposalDto> getAllProposals() {

		List<Object[]> rows = proposalRepository.findProposalDetailsWithCount();

		List<ProposalDetailWithCountDto> proposals = rows.stream()
				.map(row -> new ProposalDetailWithCountDto(((Number) row[0]).longValue(), ((Number) row[1]).longValue(),
						(String) row[2], (String) row[3],
						row[4] != null ? ProposalType.valueOf(row[4].toString()) : null,
						((Timestamp) row[5]).toLocalDateTime(), ((Number) row[6]).longValue()))
				.toList();

		return proposals.stream().collect(Collectors.groupingBy(ProposalDetailWithCountDto::opportunityId)).entrySet()
				.stream()
				.map(entry -> new OpportunityProposalDto(entry.getKey(), entry.getValue().get(0).opportunityName(),
						entry.getValue().get(0).clientName(), entry.getValue().get(0).proposalCount(),
						entry.getValue()))
				.toList();
	}

	@Transactional(readOnly = true)
	public ProposalResponse getProposal(Long proposalId) {

		Proposal proposal = proposalRepository.findById(proposalId)
				.orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + proposalId));

		return toResponse(proposal);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<byte[]> downloadByProposalId(Long proposalId) {

		Proposal proposal = proposalRepository.findById(proposalId)
				.orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

		return buildDocxDownloadResponse(proposal);
	}

	public ResponseEntity<Resource> getProposalImage(Long proposalId, String fileName) {

		Proposal proposal = proposalRepository.findById(proposalId)
				.orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

		Path imagePath = Path.of(proposal.getProposalDirectory()).resolve(fileName);

		Resource resource = new FileSystemResource(imagePath);

		return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(resource);
	}

}
