package com.projectestimation.backend.srs.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.estimation.repository.EstimationUseCaseRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.srs.ai.GeminiSrsOrchestrator;
import com.projectestimation.backend.srs.dto.BusinessRuleFieldDto;
import com.projectestimation.backend.srs.dto.DataRequirementItemDto;
import com.projectestimation.backend.srs.dto.DataRequirementsDto;
import com.projectestimation.backend.srs.dto.DocumentInformationDto;
import com.projectestimation.backend.srs.dto.EnvironmentDto;
import com.projectestimation.backend.srs.dto.EnvironmentItemDto;
import com.projectestimation.backend.srs.dto.ExternalInterfaceRequirementsDto;
import com.projectestimation.backend.srs.dto.FunctionalRequirementDto;
import com.projectestimation.backend.srs.dto.InterfaceRequirementDto;
import com.projectestimation.backend.srs.dto.IntroductionDto;
import com.projectestimation.backend.srs.dto.NonFunctionalRequirementsDto;
import com.projectestimation.backend.srs.dto.OverallProductRequirementsDto;
import com.projectestimation.backend.srs.dto.ProposedSystemEnvironmentsDto;
import com.projectestimation.backend.srs.dto.SrsDto;
import com.projectestimation.backend.srs.dto.TraceabilityMatrixEntryDto;
import com.projectestimation.backend.srs.model.Srs;
import com.projectestimation.backend.srs.repository.SrsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SrsService {

    private final OpportunityRepository opportunityRepository;
    private final SrsRepository srsRepository;
    private final GeminiSrsOrchestrator geminiSrsOrchestrator;
    private final ObjectMapper objectMapper;
    private final EstimationAnalysisRepository estimationAnalysisRepository;
    private final EstimationUseCaseRepository estimationUseCaseRepository;

    public Srs getByOpportunityId(Long opportunityId) {
        return srsRepository.findByOpportunityId(opportunityId)
                .orElse(null);
    }

    public Srs save(Srs srs) {
        return srsRepository.save(srs);
    }

    public boolean existsByOpportunityId(Long opportunityId) {
        return srsRepository.existsByOpportunityId(opportunityId);
    }

    public Opportunity getOpportunity(Long opportunityId) {
        return opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }
    
    public SrsDto generateSrs(Long opportunityId) {

	    Opportunity opportunity = getOpportunity(opportunityId);
	
	    EstimationAnalysis analysis = estimationAnalysisRepository
	            .findByOpportunityId(opportunityId)
	            .orElseThrow(() -> new ResourceNotFoundException(
	                    "Estimation analysis not found for this opportunity"));
	
	    List<EstimationUseCase> useCases = estimationUseCaseRepository
	            .findByEstimationAnalysisId(analysis.getId());
	
	    if (useCases == null || useCases.isEmpty()) {
	        throw new ResourceNotFoundException(
	                "No use cases found for this estimation analysis");
	    }
	
	    try {
	        SrsDto generatedSrs =
	                geminiSrsOrchestrator.generate(opportunity, useCases);

	        Srs srs = srsRepository.findByOpportunityId(opportunityId)
	                .orElseGet(() -> Srs.builder()
	                        .opportunity(opportunity)
	                        .build());

	        srs.setSrsData(objectMapper.writeValueAsString(generatedSrs));

	        srsRepository.save(srs);

	        return generatedSrs;

	    } catch (Exception e) {
	        throw new IllegalStateException(
	                "Failed while generating SRS from Gemini: " + e.getMessage(), e);
	    }
    }
    
    public SrsDto getGeneratedSrs(Long opportunityId) {
        Srs srs = srsRepository.findByOpportunityId(opportunityId)
                .orElse(null);

        if (srs == null) {
            return null;
        }

        try {
            return objectMapper.readValue(
                    srs.getSrsData(),
                    SrsDto.class
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse saved SRS data", e);
        }
    }

    public byte[] downloadSrs(Long opportunityId) throws IOException {

        SrsDto srsDto = getGeneratedSrs(opportunityId);

        if (srsDto == null) {
            throw new ResourceNotFoundException(
                    "SRS not found for this opportunity");
        }

        try (
                XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

        	generateSrsDocument(document, srsDto);
        	
        	document.write(out);

            return out.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate SRS document", e);
        }
    }
    
    private void generateSrsDocument(
            XWPFDocument document,
            SrsDto srsDto) {

        if (srsDto == null) {
            return;
        }

        addTitle(document, "SOFTWARE REQUIREMENTS SPECIFICATION");

        if (srsDto.documentInformation() != null) {
            addDocumentInformation(
                    document,
                    srsDto.documentInformation()
            );
        }

        addSectionHeading(document, "Document Release History");
        addText(document, "Initial version of the Software Requirements Specification.");

        addSectionHeading(document, "Circulation Details");
        addText(document, "For project stakeholders and authorized users.");

        addSectionHeading(document, "List of Amendments");
        addText(document, "Initial version.");

        addTableOfContents(document);

        document.createParagraph();

        addSectionHeading(document, "1.0 Introduction");

        if (srsDto.introduction() != null) {
            addIntroduction(
                    document,
                    srsDto.introduction()
            );
        }

        addSectionHeading(
                document,
                "2.0 Overall Product Requirements"
        );

        if (srsDto.overallProductRequirements() != null) {
            addOverallProductRequirements(
                    document,
                    srsDto.overallProductRequirements()
            );
        }

        addSectionHeading(
                document,
                "3.0 External Interface Requirements"
        );

        if (srsDto.externalInterfaceRequirements() != null) {
            addExternalInterfaceRequirements(
                    document,
                    srsDto.externalInterfaceRequirements()
            );
        }

        addSectionHeading(
                document,
                "4.0 Proposed System Environments"
        );

        if (srsDto.proposedSystemEnvironments() != null) {
            addProposedSystemEnvironments(
                    document,
                    srsDto.proposedSystemEnvironments()
            );
        }

        addSectionHeading(
                document,
                "5.0 Detailed Functional Requirements"
        );

        addFunctionalRequirements(
                document,
                srsDto.functionalRequirements()
        );

        addSectionHeading(
                document,
                "6.0 Non-Functional Requirements"
        );

        if (srsDto.nonFunctionalRequirements() != null) {
            addNonFunctionalRequirements(
                    document,
                    srsDto.nonFunctionalRequirements()
            );
        }

        addSectionHeading(
                document,
                "7.0 Data Requirements"
        );

        if (srsDto.dataRequirements() != null) {
            addDataRequirements(
                    document,
                    srsDto.dataRequirements()
            );
        }

        addSectionHeading(
                document,
                "8.0 Requirements Traceability Matrix"
        );

        addTraceabilityMatrix(
                document,
                srsDto.requirementsTraceabilityMatrix()
        );
    }
    
    private void addTitle(
            XWPFDocument document,
            String text) {

        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(200);

        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily("Arial");
        run.setFontSize(20);
    }
    
    private void addDocumentInformation(
        XWPFDocument document,
        DocumentInformationDto dto) {

    addSubHeading(document, "Document Information");

    XWPFTable table = document.createTable(4, 4);

    setCellText(
            table.getRow(0).getCell(0),
            "Document Title",
            true
    );
    setCellText(
            table.getRow(0).getCell(1),
            dto.documentTitle(),
            false
    );
    setCellText(
            table.getRow(0).getCell(2),
            "Version",
            true
    );
    setCellText(
            table.getRow(0).getCell(3),
            dto.version(),
            false
    );

    setCellText(
            table.getRow(1).getCell(0),
            "Project Name",
            true
    );
    setCellText(
            table.getRow(1).getCell(1),
            dto.projectName(),
            false
    );
    setCellText(
            table.getRow(1).getCell(2),
            "Date",
            true
    );
    setCellText(
            table.getRow(1).getCell(3),
            dto.date(),
            false
    );

    setCellText(
            table.getRow(2).getCell(0),
            "Client Name",
            true
    );
    setCellText(
            table.getRow(2).getCell(1),
            dto.clientName(),
            false
    );
    setCellText(
            table.getRow(2).getCell(2),
            "Prepared By",
            true
    );
    setCellText(
            table.getRow(2).getCell(3),
            dto.preparedBy(),
            false
    );

    setCellText(
            table.getRow(3).getCell(0),
            "Reviewed By",
            true
    );
    setCellText(
            table.getRow(3).getCell(1),
            dto.reviewedBy(),
            false
    );
    setCellText(
            table.getRow(3).getCell(2),
            "Approved By",
            true
    );
    setCellText(
            table.getRow(3).getCell(3),
            dto.approvedBy(),
            false
    );
}
    
    private void addDocumentReleaseHistory(XWPFDocument document) {

        XWPFTable table = document.createTable(2, 4);

        setCellText(table.getRow(0).getCell(0), "Version", true);
        setCellText(table.getRow(0).getCell(1), "Date", true);
        setCellText(table.getRow(0).getCell(2), "Author", true);
        setCellText(table.getRow(0).getCell(3), "Description", true);

        DocumentInformationDto info = null;

        // Use the generated document information where available.
        // The actual values are populated from the SRS document information.
        setCellText(table.getRow(1).getCell(0), "1.0", false);
        setCellText(
                table.getRow(1).getCell(1),
                document.getProperties().getCoreProperties().getCreated() != null
                        ? document.getProperties().getCoreProperties().getCreated().toString()
                        : "",
                false
        );
        setCellText(table.getRow(1).getCell(2), "", false);
        setCellText(table.getRow(1).getCell(3), "Initial SRS generation", false);
    }
    
    private void addIntroduction(
            XWPFDocument document,
            IntroductionDto dto) {

        addSubHeading(document, "1.1 Purpose of this document");
        addText(document, dto.purpose());

        addSubHeading(document, "1.2 Background of Development");
        addText(document, dto.backgroundOfDevelopment());

        addSubHeading(document, "1.3 Scope of the System");
        addText(document, dto.scopeOfTheSystem());

        addSubHeading(document, "1.4 Assumptions and Dependencies");
        addListTable(
                document,
                "Assumptions and Dependencies",
                dto.assumptionsAndDependencies()
        );
    }
    
    private void addOverallProductRequirements(
            XWPFDocument document,
            OverallProductRequirementsDto dto) {

        addSubHeading(document, "2.1 Product Perspective");
        addText(document, dto.productPerspective());

        addSubHeading(document, "2.2 Product Components");
        addListTable(
                document,
                "Product Components",
                dto.productComponents()
        );
    }
    
    private void addExternalInterfaceRequirements(
            XWPFDocument document,
            ExternalInterfaceRequirementsDto dto) {

        addSubHeading(document, "3.1 Software Interfaces");

        if (dto.softwareInterfaces() != null
                && !dto.softwareInterfaces().isEmpty()) {

            XWPFTable table = document.createTable(
                    dto.softwareInterfaces().size() + 1,
                    2
            );

            setCellText(
                    table.getRow(0).getCell(0),
                    "Technology",
                    true
            );

            setCellText(
                    table.getRow(0).getCell(1),
                    "Description",
                    true
            );

            for (int i = 0; i < dto.softwareInterfaces().size(); i++) {

                InterfaceRequirementDto item =
                        dto.softwareInterfaces().get(i);

                setCellText(
                        table.getRow(i + 1).getCell(0),
                        item.technology(),
                        false
                );

                setCellText(
                        table.getRow(i + 1).getCell(1),
                        item.description(),
                        false
                );
            }
        }

        addSubHeading(document, "3.2 Hardware Interfaces");

        addListTable(
                document,
                "Hardware Interfaces",
                dto.hardwareInterfaces()
        );

        addSubHeading(document, "3.3 Communications Interfaces");

        addListTable(
                document,
                "Communications Interfaces",
                dto.communicationInterfaces()
        );

        addSubHeading(document, "3.4 User Interfaces");

        addText(
                document,
                dto.userInterfaces()
        );
    }
    
    private void addProposedSystemEnvironments(
            XWPFDocument document,
            ProposedSystemEnvironmentsDto dto) {

        addSubHeading(
                document,
                "4.1 Operational Scenario"
        );

        addText(
                document,
                dto.operationalScenario()
        );

        addSubHeading(
                document,
                "4.2 Development Environment"
        );

        addEnvironment(
                document,
                dto.developmentEnvironment()
        );

        addSubHeading(
                document,
                "4.3 Testing Environment"
        );

        addEnvironment(
                document,
                dto.testingEnvironment()
        );

        addSubHeading(
                document,
                "4.4 Deployment Environment"
        );

        addEnvironment(
                document,
                dto.deploymentEnvironment()
        );
    }
    
    private void addEnvironment(
            XWPFDocument document,
            EnvironmentDto environment) {

        if (environment == null) {
            return;
        }

        addSubHeading(
                document,
                "Software Needed"
        );

        if (environment.softwareNeeded() != null
                && !environment.softwareNeeded().isEmpty()) {

            XWPFTable table = document.createTable(
                    environment.softwareNeeded().size() + 1,
                    2
            );

            setCellText(
                    table.getRow(0).getCell(0),
                    "Technology Area",
                    true
            );

            setCellText(
                    table.getRow(0).getCell(1),
                    "Product, Service or Standard",
                    true
            );

            for (int i = 0;
                 i < environment.softwareNeeded().size();
                 i++) {

                EnvironmentItemDto item =
                        environment.softwareNeeded().get(i);

                setCellText(
                        table.getRow(i + 1).getCell(0),
                        item.technologyArea(),
                        false
                );

                setCellText(
                        table.getRow(i + 1).getCell(1),
                        item.productServiceOrStandard(),
                        false
                );
            }
        }

        addSubHeading(
                document,
                "Hardware Needed"
        );

        addListTable(
                document,
                "Hardware Needed",
                environment.hardwareNeeded()
        );

        addSubHeading(
                document,
                "Network Needed"
        );

        addListTable(
                document,
                "Network Needed",
                environment.networkNeeded()
        );
    }
    
    private void addFunctionalRequirements(
        XWPFDocument document,
        List<FunctionalRequirementDto> requirements) {

    if (requirements == null || requirements.isEmpty()) {
        addText(document, "No functional requirements were generated.");
        return;
    }

    for (FunctionalRequirementDto requirement : requirements) {

        if (requirement == null) {
            continue;
        }

        addSubHeading(
                document,
                safe(requirement.requirementId())
                        + " - "
                        + safe(requirement.requirementName())
        );

        XWPFTable table = document.createTable(10, 2);

        setCellText(
                table.getRow(0).getCell(0),
                "Module",
                true
        );
        setCellText(
                table.getRow(0).getCell(1),
                requirement.module(),
                false
        );

        setCellText(
                table.getRow(1).getCell(0),
                "Description",
                true
        );
        setCellText(
                table.getRow(1).getCell(1),
                requirement.description(),
                false
        );

        setCellText(
                table.getRow(2).getCell(0),
                "Primary Actor",
                true
        );
        setCellText(
                table.getRow(2).getCell(1),
                requirement.primaryActor(),
                false
        );

        setCellText(
                table.getRow(3).getCell(0),
                "Secondary Actor",
                true
        );
        setCellText(
                table.getRow(3).getCell(1),
                requirement.secondaryActor(),
                false
        );

        setCellText(
                table.getRow(4).getCell(0),
                "Preconditions",
                true
        );
        setNumberedText(
                table.getRow(4).getCell(1),
                requirement.preconditions()
        );

        setCellText(
                table.getRow(5).getCell(0),
                "Basic Flow",
                true
        );
        setNumberedText(
                table.getRow(5).getCell(1),
                requirement.basicFlow()
        );

        setCellText(
                table.getRow(6).getCell(0),
                "Business Rules",
                true
        );
        setNumberedText(
                table.getRow(6).getCell(1),
                requirement.businessRules()
        );

        setCellText(
                table.getRow(7).getCell(0),
                "Fields",
                true
        );
        addFieldsTable(
                table.getRow(7).getCell(1),
                requirement.fields()
        );

        setCellText(
                table.getRow(8).getCell(0),
                "Postconditions",
                true
        );
        setNumberedText(
                table.getRow(8).getCell(1),
                requirement.postconditions()
        );

        setCellText(
                table.getRow(9).getCell(0),
                "UI Design",
                true
        );

        setMarkdownCell(
                table.getRow(9).getCell(1),
                requirement.uiDesign()
        );
    }
}
    
    private void addBusinessRuleFields(
            XWPFDocument document,
            List<BusinessRuleFieldDto> fields) {

        if (fields == null || fields.isEmpty()) {
            addText(document, "No fields specified.");
            return;
        }

        XWPFTable table = document.createTable(
                fields.size() + 1,
                4
        );

        setCellText(
                table.getRow(0).getCell(0),
                "Field Name",
                true
        );

        setCellText(
                table.getRow(0).getCell(1),
                "Type",
                true
        );

        setCellText(
                table.getRow(0).getCell(2),
                "Length / Format",
                true
        );

        setCellText(
                table.getRow(0).getCell(3),
                "Mandatory",
                true
        );

        for (int i = 0; i < fields.size(); i++) {

            BusinessRuleFieldDto field = fields.get(i);

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    field.fieldName(),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    field.type(),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(2),
                    field.lengthOrFormat(),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(3),
                    field.mandatory() == null
                            ? ""
                            : field.mandatory().toString(),
                    false
            );
        }
    }
    
    private void addNumberedListTable(
            XWPFDocument document,
            String title,
            List<String> values) {

        if (values == null || values.isEmpty()) {
            addText(document, "None specified.");
            return;
        }

        XWPFTable table = document.createTable(
                values.size() + 1,
                2
        );

        setCellText(
                table.getRow(0).getCell(0),
                "Step",
                true
        );

        setCellText(
                table.getRow(0).getCell(1),
                title,
                true
        );

        for (int i = 0; i < values.size(); i++) {

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    String.valueOf(i + 1),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    values.get(i),
                    false
            );
        }
    }
    
    private void addNonFunctionalRequirements(
            XWPFDocument document,
            NonFunctionalRequirementsDto requirements) {

        addSubHeading(document, "6.1 Accuracy");
        addText(document, requirements.accuracy());

        addSubHeading(document, "6.2 Audit Trail");
        addText(document, requirements.auditTrail());

        addSubHeading(document, "6.3 Availability");
        addText(document, requirements.availability());

        addSubHeading(document, "6.4 Capacity Limits");
        addText(document, requirements.capacityLimits());

        addSubHeading(document, "6.5 Data Retention");
        addText(document, requirements.dataRetention());

        addSubHeading(document, "6.6 Performance");
        addText(document, requirements.performance());

        addSubHeading(document, "6.7 Portability");
        addText(document, requirements.portability());

        addSubHeading(document, "6.8 Recoverability");
        addText(document, requirements.recoverability());

        addSubHeading(document, "6.9 Reliability");
        addText(document, requirements.reliability());

        addSubHeading(document, "6.10 Security Requirements");
        addText(document, requirements.securityRequirements());

        addSubHeading(document, "6.11 Other Compliance Requirements");
        addText(document, requirements.otherComplianceRequirements());
    }
    
    private void addDataRequirements(
        XWPFDocument document,
        DataRequirementsDto requirements) {

    addSubHeading(document, "7.1 Data Structures and Relationships");

    addText(
            document,
            requirements.dataStructuresAndRelationships()
    );

    addSubHeading(document, "7.2 Input to the System");

    addDataRequirementItems(
            document,
            requirements.inputs()
    );

    addSubHeading(document, "7.3 Output from the System");

    addDataRequirementItems(
            document,
            requirements.outputs()
    );

    addSubHeading(document, "7.4 Inter-functional Data Definitions");

    addText(
            document,
            requirements.interFunctionalDataDefinitions()
    );

    addSubHeading(document, "7.5 Component Cross Reference");

    addText(
            document,
            requirements.componentCrossReference()
    );
}
    
    private void addDataRequirementItems(
            XWPFDocument document,
            List<DataRequirementItemDto> items) {

        if (items == null || items.isEmpty()) {
            addText(document, "None specified.");
            return;
        }

        XWPFTable table = document.createTable(
                items.size() + 1,
                2
        );

        setCellText(table.getRow(0).getCell(0), "Name", true);
        setCellText(table.getRow(0).getCell(1), "Description", true);

        for (int i = 0; i < items.size(); i++) {

            DataRequirementItemDto item = items.get(i);

            if (item == null) {
                continue;
            }

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    item.name(),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    item.description(),
                    false
            );
        }
    }
    
    private void addTraceabilityMatrix(
            XWPFDocument document,
            List<TraceabilityMatrixEntryDto> entries) {

        if (entries == null || entries.isEmpty()) {
            addText(document, "No traceability entries were generated.");
            return;
        }

        XWPFTable table = document.createTable(
                entries.size() + 1,
                6
        );

        setCellText(table.getRow(0).getCell(0), "Requirement ID", true);
        setCellText(table.getRow(0).getCell(1), "Requirement Description", true);
        setCellText(table.getRow(0).getCell(2), "Source", true);
        setCellText(table.getRow(0).getCell(3), "Module", true);
        setCellText(table.getRow(0).getCell(4), "Related Use Case", true);
        setCellText(table.getRow(0).getCell(5), "Validation Method", true);

        for (int i = 0; i < entries.size(); i++) {

            TraceabilityMatrixEntryDto entry = entries.get(i);

            if (entry == null) {
                continue;
            }

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    entry.requirementId(),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    entry.requirementDescription(),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(2),
                    entry.source(),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(3),
                    entry.module(),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(4),
                    entry.relatedUseCase(),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(5),
                    entry.validationMethod(),
                    false
            );
        }
    }
    
    private void addSectionHeading(
        XWPFDocument document,
        String text) {

	    XWPFParagraph paragraph = document.createParagraph();
	
	    paragraph.setStyle("Heading1");
	    paragraph.setSpacingBefore(200);
	    paragraph.setSpacingAfter(120);
	
	    XWPFRun run = paragraph.createRun();
	    run.setText(text);
	    run.setBold(true);
	    run.setFontFamily("Arial");
	    run.setFontSize(14);
    }
    
    private void addSubHeading(
        XWPFDocument document,
        String text) {

	    XWPFParagraph paragraph = document.createParagraph();
	
	    paragraph.setStyle("Heading2");
	    paragraph.setSpacingBefore(120);
	    paragraph.setSpacingAfter(80);
	
	    XWPFRun run = paragraph.createRun();
	    run.setText(text);
	    run.setBold(true);
	    run.setFontFamily("Arial");
	    run.setFontSize(12);
	}
    
    private void addText(
            XWPFDocument document,
            String text) {

        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(100);

        XWPFRun run = paragraph.createRun();
        run.setText(safe(text));
        run.setFontFamily("Arial");
        run.setFontSize(10);
    }
    
    private String safe(String value) {
        return value == null ? "" : value;
    }
    
    private void addListTable(
            XWPFDocument document,
            String title,
            List<String> values) {

        if (values == null || values.isEmpty()) {
            addText(document, "None specified.");
            return;
        }

        XWPFTable table = document.createTable(
                values.size() + 1,
                2
        );

        setCellText(
                table.getRow(0).getCell(0),
                "No.",
                true
        );

        setCellText(
                table.getRow(0).getCell(1),
                title,
                true
        );

        for (int i = 0; i < values.size(); i++) {

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    String.valueOf(i + 1),
                    false
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    values.get(i),
                    false
            );
        }
    }
    
    private void setCellText(
        XWPFTableCell cell,
        String text,
        boolean bold) {

    cell.removeParagraph(0);

    XWPFParagraph paragraph = cell.addParagraph();
    paragraph.setSpacingBefore(40);
    paragraph.setSpacingAfter(40);

    XWPFRun run = paragraph.createRun();
    run.setText(safe(text));
    run.setBold(bold);
    run.setFontFamily("Arial");
    run.setFontSize(10);

    if (bold) {
        CTShd shading = cell.getCTTc()
                .addNewTcPr()
                .addNewShd();

        shading.setFill("D9E1F2");
    }
}
    
    private void addLabelValueTable(
            XWPFDocument document,
            String[][] values) {

        if (values == null || values.length == 0) {
            return;
        }

        XWPFTable table = document.createTable(
                values.length,
                2
        );

        for (int i = 0; i < values.length; i++) {

            String label = values[i].length > 0
                    ? values[i][0]
                    : "";

            String value = values[i].length > 1
                    ? values[i][1]
                    : "";

            setCellText(
                    table.getRow(i).getCell(0),
                    label,
                    true
            );

            setCellText(
                    table.getRow(i).getCell(1),
                    value,
                    false
            );
        }
    }
    
    private void addTableOfContents(XWPFDocument document) {

	    XWPFParagraph paragraph = document.createParagraph();
	
	    XWPFRun run = paragraph.createRun();
	
	    run.setText("Table of Contents");
	    run.setBold(true);
	    run.setFontFamily("Arial");
	    run.setFontSize(12);
	
	    XWPFParagraph tocParagraph = document.createParagraph();
	
	    XWPFRun tocRun = tocParagraph.createRun();
	
	    tocRun.getCTR().addNewFldChar().setFldCharType(
	            org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.BEGIN
	    );
	
	    tocRun.getCTR().addNewInstrText()
	            .setStringValue("TOC \\o \"1-2\" \\h \\z \\u");
	
	    tocRun.getCTR().addNewFldChar().setFldCharType(
	            org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.SEPARATE
	    );
	
	    tocRun.setText(
	            "Right-click here and select \"Update Field\" to generate the Table of Contents."
	    );
	
	    tocRun.getCTR().addNewFldChar().setFldCharType(
	            org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.END
	    );
	}
    
    private void setNumberedText(
            XWPFTableCell cell,
            List<String> values) {

        if (values == null || values.isEmpty()) {
            setCellText(cell, "None specified.", false);
            return;
        }

        cell.removeParagraph(0);

        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setSpacingBefore(40);
        paragraph.setSpacingAfter(40);

        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Arial");
        run.setFontSize(10);

        for (int i = 0; i < values.size(); i++) {

            if (i > 0) {
                run.addBreak();
            }

            run.setText(
                    (i + 1)
                            + ". "
                            + safe(values.get(i))
            );
        }
    }
    
    private void addFieldsTable(
        XWPFTableCell cell,
        List<BusinessRuleFieldDto> fields) {

    if (fields == null || fields.isEmpty()) {
        setCellText(cell, "No fields specified.", false);
        return;
    }

    cell.removeParagraph(0);

    XWPFParagraph paragraph = cell.addParagraph();
    paragraph.setSpacingBefore(40);
    paragraph.setSpacingAfter(40);

    XWPFRun run = paragraph.createRun();
    run.setFontFamily("Arial");
    run.setFontSize(10);

    // Header
    run.setBold(true);
    run.setText(
            "Field Name    |    Type    |    Length / Format    |    Mandatory"
    );

    run.addBreak();

    // Field rows
    run.setBold(false);

    for (BusinessRuleFieldDto field : fields) {

        run.setText(
                safe(field.fieldName())
                        + "    |    "
                        + safe(field.type())
                        + "    |    "
                        + safe(field.lengthOrFormat())
                        + "    |    "
                        + (field.mandatory() == null
                                ? ""
                                : field.mandatory().toString())
        );

        run.addBreak();
    }
}
    
    private void setMarkdownCell(
            XWPFTableCell cell,
            String markdown) {

        cell.removeParagraph(0);

        if (markdown == null || markdown.isBlank()) {
            XWPFParagraph paragraph = cell.addParagraph();

            XWPFRun run = paragraph.createRun();
            run.setText("Not specified");
            run.setFontFamily("Arial");
            run.setFontSize(10);

            return;
        }

        String[] lines = markdown.split("\\r?\\n");

        for (String line : lines) {

            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                continue;
            }

            // ---------------------------------------------------------
            // Heading
            // ---------------------------------------------------------
            if (trimmedLine.startsWith("### ")) {

                XWPFParagraph paragraph = cell.addParagraph();
                paragraph.setSpacingBefore(100);
                paragraph.setSpacingAfter(80);

                XWPFRun run = paragraph.createRun();
                run.setText(trimmedLine.substring(4));
                run.setBold(true);
                run.setFontFamily("Arial");
                run.setFontSize(12);

                continue;
            }

            // ---------------------------------------------------------
            // Checkbox
            // Example:
            // [ ] Remember Me
            // ---------------------------------------------------------
            if (trimmedLine.startsWith("[ ] ")) {

                XWPFParagraph paragraph = cell.addParagraph();
                paragraph.setSpacingBefore(40);
                paragraph.setSpacingAfter(40);

                XWPFRun run = paragraph.createRun();
                run.setText("☐ " + trimmedLine.substring(4));
                run.setFontFamily("Arial");
                run.setFontSize(10);

                continue;
            }

            // ---------------------------------------------------------
            // Input field
            // Example:
            // [ Enter email ]
            // ---------------------------------------------------------
            if (trimmedLine.matches("\\[\\s*Enter .*\\s*\\]")) {

                String text = trimmedLine
                        .substring(1, trimmedLine.length() - 1)
                        .trim();

                XWPFParagraph paragraph = cell.addParagraph();
                paragraph.setSpacingBefore(40);
                paragraph.setSpacingAfter(60);

                addBoxBorder(paragraph, false);

                XWPFRun run = paragraph.createRun();
                run.setText("   " + text + "   ");
                run.setFontFamily("Arial");
                run.setFontSize(10);

                continue;
            }

            // ---------------------------------------------------------
            // Button
            // Example:
            // [ LOGIN ]
            // ---------------------------------------------------------
            if (trimmedLine.matches("\\[[^\\]]+\\]")) {

                String buttonText = trimmedLine
                        .substring(1, trimmedLine.length() - 1)
                        .trim();

                XWPFParagraph paragraph = cell.addParagraph();
                paragraph.setAlignment(
                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER
                );
                paragraph.setSpacingBefore(50);
                paragraph.setSpacingAfter(50);

                addBoxBorder(paragraph, true);

                XWPFRun run = paragraph.createRun();
                run.setText("  " + buttonText + "  ");
                run.setBold(true);
                run.setFontFamily("Arial");
                run.setFontSize(9);

                continue;
            }

            // ---------------------------------------------------------
            // Bold label
            // Example:
            // **Email**
            // ---------------------------------------------------------
            if (trimmedLine.startsWith("**")
                    && trimmedLine.endsWith("**")) {

                String label = trimmedLine.substring(
                        2,
                        trimmedLine.length() - 2
                );

                XWPFParagraph paragraph = cell.addParagraph();
                paragraph.setSpacingBefore(40);
                paragraph.setSpacingAfter(20);

                XWPFRun run = paragraph.createRun();
                run.setText(label);
                run.setBold(true);
                run.setFontFamily("Arial");
                run.setFontSize(10);

                continue;
            }

            // ---------------------------------------------------------
            // Bullet
            // ---------------------------------------------------------
            if (trimmedLine.startsWith("- ")) {

                XWPFParagraph paragraph = cell.addParagraph();
                paragraph.setSpacingBefore(20);
                paragraph.setSpacingAfter(20);

                XWPFRun run = paragraph.createRun();
                run.setText("• " + trimmedLine.substring(2));
                run.setFontFamily("Arial");
                run.setFontSize(10);

                continue;
            }

            // ---------------------------------------------------------
            // Normal text
            // ---------------------------------------------------------
            XWPFParagraph paragraph = cell.addParagraph();
            paragraph.setSpacingBefore(30);
            paragraph.setSpacingAfter(30);

            XWPFRun run = paragraph.createRun();
            run.setText(trimmedLine);
            run.setFontFamily("Arial");
            run.setFontSize(10);
        }
    }
    
    private void addBoxBorder(
            XWPFParagraph paragraph,
            boolean button) {

        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr pPr =
                paragraph.getCTP().isSetPPr()
                        ? paragraph.getCTP().getPPr()
                        : paragraph.getCTP().addNewPPr();

        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr pBdr =
                pPr.isSetPBdr()
                        ? pPr.getPBdr()
                        : pPr.addNewPBdr();

        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder border =
                pBdr.isSetTop()
                        ? pBdr.getTop()
                        : pBdr.addNewTop();

        border.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        border.setSz(BigInteger.valueOf(button ? 8 : 6));
        border.setSpace(BigInteger.valueOf(4));
        border.setColor("808080");

        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder bottom =
                pBdr.isSetBottom()
                        ? pBdr.getBottom()
                        : pBdr.addNewBottom();

        bottom.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        bottom.setSz(BigInteger.valueOf(button ? 8 : 6));
        bottom.setSpace(BigInteger.valueOf(4));
        bottom.setColor("808080");

        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder left =
                pBdr.isSetLeft()
                        ? pBdr.getLeft()
                        : pBdr.addNewLeft();

        left.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        left.setSz(BigInteger.valueOf(button ? 8 : 6));
        left.setSpace(BigInteger.valueOf(4));
        left.setColor("808080");

        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder right =
                pBdr.isSetRight()
                        ? pBdr.getRight()
                        : pBdr.addNewRight();

        right.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        right.setSz(BigInteger.valueOf(button ? 8 : 6));
        right.setSpace(BigInteger.valueOf(4));
        right.setColor("808080");
    }
}