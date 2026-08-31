package com.projectestimation.backend.proposal.ai;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.gateway.AiGateway;
import com.projectestimation.backend.common.enums.ProposalType;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.opportunity.dto.GeminiFile;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.OpportunityFile;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.opportunity.service.GeminiFileService;
import com.projectestimation.backend.prompt.AiPrompt;
import com.projectestimation.backend.proposal.service.GeminiDiagramGenerationService;

@Service
public class GeminiProposalOrchestrator {
	public static final Logger log = LogManager.getLogger(GeminiProposalOrchestrator.class);
	private static final int PROPOSAL_MAX_OUTPUT_TOKENS = 8192;

	private final GeminiProposalPromptBuilder promptBuilder;
	private final AiGateway aiGateway;
	private final GeminiFileService geminiFileService;
	private final GeminiProposalResponseParser responseParser;
	private final ProposalStaticContentProvider staticContentProvider;
	private final GeminiDiagramGenerationService diagramService;
	private final OpportunityRepository opportunityRepository;

	public GeminiProposalOrchestrator(GeminiProposalPromptBuilder promptBuilder, AiGateway aiGateway,
			GeminiProposalResponseParser responseParser, ProposalStaticContentProvider staticContentProvider,
			GeminiDiagramGenerationService diagramService, GeminiFileService geminiFileService,
			OpportunityRepository opportunityRepository) {
		this.promptBuilder = promptBuilder;
		this.aiGateway = aiGateway;
		this.responseParser = responseParser;
		this.staticContentProvider = staticContentProvider;
		this.diagramService = diagramService;
		this.geminiFileService = geminiFileService;
		this.opportunityRepository = opportunityRepository;
	}

	public AiProposalResult generateProposal(Opportunity opportunity, EstimationAnalysis analysis,
			ProposalType proposalType, String baseFileName) {

		List<String> workflowNames = List.of();

		String workflowsSection = "";

		String workflowPlaceholderRules = "";

		if (proposalType == ProposalType.EXPERT) {

			String workflowResponse = diagramService.identifyProcessFlows(opportunity);

			workflowNames = workflowResponse.lines().map(String::trim).filter(s -> !s.isBlank()).toList();

			StringBuilder placeholderBuilder = new StringBuilder();

			for (int i = 0; i < workflowNames.size(); i++) {

				placeholderBuilder.append(workflowNames.get(i)).append(" -> {{PROCESS_FLOW_IMAGE_").append(i + 1)
						.append("}}\n");
			}

			workflowPlaceholderRules = placeholderBuilder.toString();

			workflowsSection = workflowNames.stream().map(w -> "- " + w)
					.collect(java.util.stream.Collectors.joining("\n"));
		}
//		log.info("Requirment summery:: {}", opportunity.getRequirementSummary());
//		if (opportunity.getRequirementSummary() == null || opportunity.getRequirementSummary().isEmpty()) {
//			log.info("file storage :: {}", );
//		}

		String prompt = promptBuilder.build(opportunity, analysis, proposalType, workflowsSection,
				workflowPlaceholderRules.toString());
		try {
			if (opportunity.getRequirementSummary() == null || opportunity.getRequirementSummary().isEmpty()) {
				OpportunityFile oFile = opportunity.getOpportunityFile();

				Path filePath = Paths.get(oFile.getStoredLocation());
				GeminiFile fileUri = geminiFileService.uploadFile(filePath);
				String fileReaderPrompt = AiPrompt.CREATE_SUMMARY;

				String getReqSummeryFromDoc = aiGateway.generateRequirmentSunnary(fileReaderPrompt, fileUri,
						PROPOSAL_MAX_OUTPUT_TOKENS);
				if (getReqSummeryFromDoc != null && !getReqSummeryFromDoc.isEmpty()) {
					opportunity.setRequirementSummary(getReqSummeryFromDoc);
					Opportunity opportunityUpdate = updateOpportunity(getReqSummeryFromDoc, opportunity.getId());
					if (opportunityUpdate != null) {
						log.info("Requirment summary updated successfully");
					}
				} else {
					throw new ProposalFailedException("Failed to generate project scope from requirment document");
				}
				// log.info("getReqSummeryFromDoc :: {}", getReqSummeryFromDoc);
				// AiProposalResult parsed = responseParser.parse(getReqSummeryFromDoc,
				// proposalType);

				// String markdown = parsed.markdownContent();
			}

			String rawResponse = aiGateway.generateContent(prompt, "text/plain", PROPOSAL_MAX_OUTPUT_TOKENS);
			AiProposalResult parsed = responseParser.parse(rawResponse, proposalType);

			String markdown = parsed.markdownContent();

			String architectureHtml = diagramService.generateSolutionArchitectureHtml(opportunity);

			List<String> processFlowHtmls = List.of();

			if (proposalType == ProposalType.EXPERT) {

				processFlowHtmls = workflowNames.stream()
						.map(workflow -> diagramService.generateProcessFlowHtml(opportunity, workflow)).toList();
			}

			markdown = injectStaticSections(markdown, proposalType, baseFileName, workflowNames.size());

			return new AiProposalResult(markdown, architectureHtml, processFlowHtmls);
		} catch (AiGenerationFailedException ex) {
			throw new ProposalFailedException(ex.getMessage(), ex);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new ProposalFailedException(e.getMessage(), e);
		}
	}

	private Opportunity updateOpportunity(String scope, long opportunityId) {
		try {
			Opportunity opportunity = opportunityRepository.findById(opportunityId)
					.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
			opportunity.setRequirementSummary(scope);
			opportunity.setUpdatedAt(LocalDateTime.now());
			return opportunityRepository.save(opportunity);
		} catch (Exception e) {
			log.error(e);

		}
		return null;
	}

	private String injectStaticSections(String markdown, ProposalType proposalType, String baseFileName,
			int workflowCount) {

		String safeFileName = baseFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

		String architectureImageName = safeFileName + "-architecture.png";

//    	String processFlowImageName =
//    	        baseFileName + "-process-flow.png";

		markdown += "\n\n";
		if (proposalType == ProposalType.BASIC) {
			markdown = markdown.replace("{{COMPLETION_CRITERIA}}", staticContentProvider.load("CompletionCriteria.md"));

			markdown += "\n\n";

			markdown = markdown.replace("{{ORGANISATION_CAPABILITIES_BASIC}}",
					staticContentProvider.load("OrganisationCapabilitiesBasic.md"));

//        	markdown += "\n\n";
//			
//        	markdown = markdown.replace("{{SOLUTION_ARCHITECHTURE}}",staticContentProvider.load(
//                    "SolutionArchitechture.md")
//            );

			markdown += "\n\n";

			markdown = markdown.replace("{{EXECUTION_PLAN}}", staticContentProvider.load("ExecutionSchedule.md"));

			markdown += "\n\n";

			markdown = markdown.replace("{{SOLUTION_ARCHITECTURE_IMAGE}}",
					"![](assets/images/" + architectureImageName + ")");

		}
		if (proposalType == ProposalType.INTERMEDIATE || proposalType == ProposalType.EXPERT) {

			markdown = markdown.replace("{{SOLUTION_ARCHITECTURE_IMAGE}}",
					"![](assets/images/" + architectureImageName + ")");

			markdown += "\n\n";

			markdown = markdown.replace("{{QUALITY_ASSURANCE}}", staticContentProvider.load("QualityAssurance.md"));

			markdown += "\n\n";

			markdown = markdown.replace("{{COMPLETION_CRITERIA}}", staticContentProvider.load("CompletionCriteria.md"));

			markdown += "\n\n";

			markdown = markdown.replace("{{DATA_SECURITY}}", staticContentProvider.load("DataSecurity.md"));

			markdown += "\n\n";

//        	markdown = markdown.replace("{{SOLUTION_ARCHITECHTURE}}",staticContentProvider.load(
//                    "SolutionArchitechture.md")
//            );
//            markdown += "\n\n";

			markdown = markdown.replace("{{ORGANISATION_CAPABILITIES_DETAILED}}",
					staticContentProvider.load("OrganisationCapabilitiesDetailed.md"));

			markdown += "\n\n";

			markdown = markdown.replace("{{EXECUTION_PLAN}}", staticContentProvider.load("ExecutionSchedule.md"));

			markdown += "\n\n";
		}

		if (proposalType == ProposalType.EXPERT) {

			markdown += "\n\n";

			markdown = markdown.replace("{{ACCOUNTIBILITY_DISTRIBUTION}}",
					staticContentProvider.load("AccountibilityDistribution.md"));

			markdown += "\n\n";

			markdown = markdown.replace("{{TESTING_PROCESS}}", staticContentProvider.load("TestingProcess.md"));

			markdown += "\n\n";
		}

//        markdown = markdown.replace("{{ORGANISATION_CAPABILITIES_DETAILED}}",staticContentProvider.load(
//                "OrganisationCapabilitiesDetailed.md")
//        );
//
//        markdown += "\n\n";

		markdown = markdown.replace("{{TERMS_AND_CONDITIONS}}", staticContentProvider.load("Terms&Conditions.md"));

		markdown += "\n\n";

		markdown = markdown.replace("{{CONFIGURATION_MANAGEMENT}}",
				staticContentProvider.load("ConfigurationManagement.md"));

		for (int i = 1; i <= workflowCount; i++) {

//            markdown = markdown.replace(
//                    "{{PROCESS_FLOW_IMAGE_" + i + "}}",
//                    "![](assets/images/"
//                            + baseFileName
//                            + "-process-flow-"
//                            + i
//                            + ".png)"
//            );

			markdown = markdown.replace("{{PROCESS_FLOW_IMAGE_" + i + "}}",
					"![](assets/images/" + safeFileName + "-process-flow-" + i + ".png)");
		}

		return markdown;
	}

}
