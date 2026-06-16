package com.projectestimation.backend.proposal.ai;

import java.util.List;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.common.enums.ProposalType;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.parameters.model.Parameters;
import com.projectestimation.backend.proposal.service.GeminiDiagramGenerationService;

@Service
public class GeminiProposalOrchestrator {

    private static final int PROPOSAL_MAX_OUTPUT_TOKENS = 8192;

    private final GeminiProposalPromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final GeminiProposalResponseParser responseParser;
    private final ProposalStaticContentProvider staticContentProvider;
    private final GeminiDiagramGenerationService diagramService;

    public GeminiProposalOrchestrator(
            GeminiProposalPromptBuilder promptBuilder,
            GeminiClient geminiClient,
            GeminiProposalResponseParser responseParser,
            ProposalStaticContentProvider staticContentProvider,
            GeminiDiagramGenerationService diagramService
    ) {
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.responseParser = responseParser;
        this.staticContentProvider = staticContentProvider;
        this.diagramService = diagramService;
    }

    public AiProposalResult generateProposal(Opportunity opportunity,
                                             Parameters parameters,
                                             EstimateResult estimate,
                                             ProposalType proposalType, String baseFileName) {
    	
    	String workflowResponse =
                diagramService
                        .identifyProcessFlows(
                                opportunity
                        );

        List<String> workflowNames =
                workflowResponse.lines()
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
        
        StringBuilder workflowPlaceholderRules =
                new StringBuilder();

        for (int i = 0; i < workflowNames.size(); i++) {

            workflowPlaceholderRules.append(
                    workflowNames.get(i)
            )
            .append(" -> {{PROCESS_FLOW_IMAGE_")
            .append(i + 1)
            .append("}}\n");
        }
        
        String workflowsSection =
                workflowNames.stream()
                        .map(w -> "- " + w)
                        .collect(
                                java.util.stream.Collectors.joining("\n")
                        );
        
        String prompt = promptBuilder.build(opportunity, parameters, estimate, proposalType, workflowsSection, workflowPlaceholderRules.toString());
        try {
            String rawResponse = geminiClient.generateContent(prompt, "text/plain", PROPOSAL_MAX_OUTPUT_TOKENS);
            AiProposalResult parsed =
                    responseParser.parse(
                            rawResponse,
                            proposalType
                    );

            String markdown =
                    parsed.markdownContent();
            
            String architectureHtml =
                    diagramService
                            .generateSolutionArchitectureHtml(
                                    opportunity
                            );

            
            List<String> processFlowHtmls =
                    workflowNames.stream()
                            .map(workflow ->
                                    diagramService
                                            .generateProcessFlowHtml(
                                                    opportunity,
                                                    workflow
                                            )
                            )
                            .toList();
            
            markdown =
                    injectStaticSections(
                            markdown,
                            proposalType,
                            baseFileName,
                            workflowNames.size()
                    );

            return new AiProposalResult(
                    markdown,
                    architectureHtml,
                    processFlowHtmls
            );
        } catch (AiGenerationFailedException ex) {
            throw new ProposalFailedException(ex.getMessage(), ex);
        }
    }
    
    private String injectStaticSections(
            String markdown,
            ProposalType proposalType,
            String baseFileName,
            int workflowCount
    ) {

    	String safeFileName =
    	        baseFileName.replaceAll("[^a-zA-Z0-9._-]", "_");

    	String architectureImageName =
    	        safeFileName + "-architecture.png";

//    	String processFlowImageName =
//    	        baseFileName + "-process-flow.png";
    	
        markdown += "\n\n";
        if(proposalType == ProposalType.BASIC){
        	markdown =  markdown.replace("{{COMPLETION_CRITERIA}}",staticContentProvider.load(
                    "CompletionCriteria.md")
            );
        	
        	markdown += "\n\n";
        			
        	markdown = markdown.replace("{{ORGANISATION_CAPABILITIES_BASIC}}",staticContentProvider.load(
                    "OrganisationCapabilitiesBasic.md")
            );
        	
//        	markdown += "\n\n";
//			
//        	markdown = markdown.replace("{{SOLUTION_ARCHITECHTURE}}",staticContentProvider.load(
//                    "SolutionArchitechture.md")
//            );
        	
        	markdown += "\n\n";
			
        	markdown = markdown.replace("{{EXECUTION_PLAN}}",staticContentProvider.load(
                    "ExecutionSchedule.md")
            );
        	
        	markdown += "\n\n";
        	
        	markdown = markdown.replace(
        	        "{{SOLUTION_ARCHITECTURE_IMAGE}}",
        	        "![](assets/images/" + architectureImageName + ")"
        	);

        }
        if (
                proposalType == ProposalType.INTERMEDIATE
                || proposalType == ProposalType.EXPERT
        ) {
        	
        	markdown = markdown.replace(
        	        "{{SOLUTION_ARCHITECTURE_IMAGE}}",
        	        "![](assets/images/" + architectureImageName + ")"
        	);

        	markdown += "\n\n";

            markdown = markdown.replace("{{QUALITY_ASSURANCE}}",staticContentProvider.load(
                    "QualityAssurance.md")
            );

            markdown += "\n\n";

            markdown = markdown.replace("{{COMPLETION_CRITERIA}}",staticContentProvider.load(
                    "CompletionCriteria.md")
            );
            
            markdown += "\n\n";
            
            markdown = markdown.replace("{{DATA_SECURITY}}",staticContentProvider.load(
                    "DataSecurity.md")
            );
            
        	markdown += "\n\n";
			
//        	markdown = markdown.replace("{{SOLUTION_ARCHITECHTURE}}",staticContentProvider.load(
//                    "SolutionArchitechture.md")
//            );
//            markdown += "\n\n";
            
            markdown = markdown.replace("{{ORGANISATION_CAPABILITIES_DETAILED}}",staticContentProvider.load(
                    "OrganisationCapabilitiesDetailed.md")
            );

            markdown += "\n\n";
			
        	markdown = markdown.replace("{{EXECUTION_PLAN}}",staticContentProvider.load(
                    "ExecutionSchedule.md")
            );
        	
        	markdown += "\n\n";
        }

        if (
                proposalType == ProposalType.EXPERT
        ) {

        	
        	markdown += "\n\n";
        	
            markdown = markdown.replace("{{ACCOUNTIBILITY_DISTRIBUTION}}",staticContentProvider.load(
                    "AccountibilityDistribution.md")
            );


            markdown += "\n\n";

            markdown = markdown.replace("{{TESTING_PROCESS}}",staticContentProvider.load(
                    "TestingProcess.md")
            );

            markdown += "\n\n";
        }

//        markdown = markdown.replace("{{ORGANISATION_CAPABILITIES_DETAILED}}",staticContentProvider.load(
//                "OrganisationCapabilitiesDetailed.md")
//        );
//
//        markdown += "\n\n";

        markdown = markdown.replace("{{TERMS_AND_CONDITIONS}}",staticContentProvider.load(
                "Terms&Conditions.md")
        );

        markdown += "\n\n";
        
        markdown = markdown.replace("{{CONFIGURATION_MANAGEMENT}}",staticContentProvider.load(
                "ConfigurationManagement.md")
        );
        
        for (int i = 1; i <= workflowCount; i++) {

//            markdown = markdown.replace(
//                    "{{PROCESS_FLOW_IMAGE_" + i + "}}",
//                    "![](assets/images/"
//                            + baseFileName
//                            + "-process-flow-"
//                            + i
//                            + ".png)"
//            );
        	
        	markdown = markdown.replace(
        	        "{{PROCESS_FLOW_IMAGE_" + i + "}}",
        	        "![](assets/images/"
        	                + safeFileName
        	                + "-process-flow-"
        	                + i
        	                + ".png)"
        	);
        }
        
        return markdown;
    }
    
}
