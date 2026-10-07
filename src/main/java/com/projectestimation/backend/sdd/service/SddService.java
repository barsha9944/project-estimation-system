package com.projectestimation.backend.sdd.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.sdd.ai.GeminiSddOrchestrator;
import com.projectestimation.backend.sdd.dto.AmendmentDto;
import com.projectestimation.backend.sdd.dto.ApplicationComponentDetailsDto;
import com.projectestimation.backend.sdd.dto.ApplicationComponentDto;
import com.projectestimation.backend.sdd.dto.CirculationDetailsDto;
import com.projectestimation.backend.sdd.dto.CirculationDetailsEntryDto;
import com.projectestimation.backend.sdd.dto.DatabaseDesignDto;
import com.projectestimation.backend.sdd.dto.DesignAlternativeDto;
import com.projectestimation.backend.sdd.dto.DesignDetailDto;
import com.projectestimation.backend.sdd.dto.DocumentReleaseHistoryDto;
import com.projectestimation.backend.sdd.dto.DocumentReleaseHistoryEntryDto;
import com.projectestimation.backend.sdd.dto.EnvironmentDetailsDto;
import com.projectestimation.backend.sdd.dto.EnvironmentDto;
import com.projectestimation.backend.sdd.dto.EnvironmentItemDto;
import com.projectestimation.backend.sdd.dto.IntroductionSddDto;
import com.projectestimation.backend.sdd.dto.ScopeTaskDto;
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.dto.SddTraceabilityMatrixEntryDto;
import com.projectestimation.backend.sdd.dto.SpecialConsiderationsDto;
import com.projectestimation.backend.sdd.dto.SystemIntegrationStrategyDto;
import com.projectestimation.backend.sdd.dto.SystemPerspectiveDto;
import com.projectestimation.backend.sdd.model.Sdd;
import com.projectestimation.backend.sdd.repository.SddRepository;
import com.projectestimation.backend.srs.dto.SrsDto;
import com.projectestimation.backend.srs.service.SrsService;

import lombok.RequiredArgsConstructor;
import net.sourceforge.plantuml.SourceStringReader;

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
                    "Failed to parse saved SDD data",
                    e
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

    public byte[] downloadSdd(Long opportunityId) throws IOException {

        SddDto sddDto = getGeneratedSdd(opportunityId);

        if (sddDto == null) {

            throw new ResourceNotFoundException(
                    "SDD not found for this opportunity"
            );
        }

        try (
                XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            generateSddDocument(document, sddDto);

            document.write(out);

            return out.toByteArray();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to generate SDD document",
                    e
            );
        }
    }

    private void generateSddDocument(
            XWPFDocument document,
            SddDto sddDto) {

        if (sddDto == null) {
            return;
        }

        addTitle(
                document,
                "SOFTWARE DESIGN DOCUMENT"
        );

        /*
         * A. Document Release History
         */
        addDocumentReleaseHistory(
                document,
                sddDto.documentReleaseHistory()
        );

        /*
         * B. Circulation Details
         */
        addCirculationDetails(
                document,
                sddDto.circulationDetails()
        );

        /*
         * C. List of Amendments
         */
        addAmendments(
                document,
                sddDto.amendments()
        );

        /*
         * Table of Contents
         */
        addTableOfContents(document);

        document.createParagraph();

        /*
         * 1.0 Introduction
         */
        addIntroduction(
                document,
                sddDto.introduction()
        );

        /*
         * 2.0 The System Perspective
         */
        addSystemPerspective(
                document,
                sddDto.systemPerspective()
        );

        /*
         * 3.0 Specific Description of the Application Components
         */
        addApplicationComponents(
                document,
                sddDto.applicationComponents()
        );

        /*
         * 4.0 Proposed Database Design
         *
         */

        addDatabaseDesign(
                document,
                sddDto.databaseDesign()
        );
        
        /*
         * 5.0 Special Considerations
         */
        addSpecialConsiderations(
                document,
                sddDto.specialConsiderations()
        );

        /*
         * 6.0 System Integration Strategy
         */
        addSystemIntegrationStrategy(
                document,
                sddDto.systemIntegrationStrategy()
        );

        /*
         * 7.0 Requirements Traceability Matrix
         */
        addRequirementsTraceabilityMatrix(
                document,
                sddDto.requirementsTraceabilityMatrix()
        );
    }

    private void addTitle(
            XWPFDocument document,
            String title) {

        XWPFParagraph paragraph =
                document.createParagraph();

        paragraph.setAlignment(
                ParagraphAlignment.CENTER
        );

        XWPFRun run =
                paragraph.createRun();

        run.setText(title);
        run.setBold(true);
        run.setFontSize(20);
    }

    private void addSectionHeading(
            XWPFDocument document,
            String text) {

        XWPFParagraph paragraph =
                document.createParagraph();

        XWPFRun run =
                paragraph.createRun();

        run.setText(text);
        run.setBold(true);
        run.setFontSize(17);
    }

    private void addSubHeading(
            XWPFDocument document,
            String text) {

        XWPFParagraph paragraph =
                document.createParagraph();

        XWPFRun run =
                paragraph.createRun();

        run.setText(text);
        run.setBold(true);
        run.setFontSize(15);
    }

    private void addText(
            XWPFDocument document,
            String text) {

        if (text == null || text.isBlank()) {
            return;
        }

        XWPFParagraph paragraph =
                document.createParagraph();

        XWPFRun run =
                paragraph.createRun();

        run.setText(text);
        run.setFontSize(13);
    }

    private void setCellText(
            XWPFTableCell cell,
            String text) {

        cell.removeParagraph(0);

        XWPFParagraph paragraph =
                cell.addParagraph();

        XWPFRun run =
                paragraph.createRun();

        run.setText(
                text == null ? "" : text
        );

        run.setFontSize(12);
    }

    private void addTableOfContents(
            XWPFDocument document) {

        addSectionHeading(
                document,
                "Table of Contents"
        );

        XWPFParagraph paragraph =
                document.createParagraph();

        XWPFRun run =
                paragraph.createRun();

        run.setText(
                "Table of Contents will be generated here."
        );

        run.setFontSize(12);
    }

    private void addDocumentReleaseHistory(
            XWPFDocument document,
            DocumentReleaseHistoryDto releaseHistory) {

        addSectionHeading(
                document,
                "A. Document Release History"
        );

        if (releaseHistory == null
                || releaseHistory.entries() == null
                || releaseHistory.entries().isEmpty()) {

            addText(
                    document,
                    "No document release history available."
            );

            return;
        }

        XWPFTable table =
                document.createTable(
                        releaseHistory.entries().size() + 1,
                        6
                );

        String[] headers = {
                "Sl. No.",
                "Version Number",
                "Release Date",
                "Prepared By",
                "Reviewed & Approved By",
                "Reasons for this Release"
        };

        for (int i = 0; i < headers.length; i++) {

            setCellText(
                    table.getRow(0).getCell(i),
                    headers[i]
            );
        }

        for (int i = 0;
             i < releaseHistory.entries().size();
             i++) {

            DocumentReleaseHistoryEntryDto entry =
                    releaseHistory.entries().get(i);

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    entry.serialNumber() == null
                            ? ""
                            : String.valueOf(
                                    entry.serialNumber()
                            )
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    entry.versionNumber()
            );

            setCellText(
                    table.getRow(i + 1).getCell(2),
                    entry.releaseDate()
            );

            setCellText(
                    table.getRow(i + 1).getCell(3),
                    entry.preparedBy()
            );

            setCellText(
                    table.getRow(i + 1).getCell(4),
                    entry.reviewedAndApprovedBy()
            );

            setCellText(
                    table.getRow(i + 1).getCell(5),
                    entry.reasonsForRelease()
            );
        }
    }

    private void addCirculationDetails(
            XWPFDocument document,
            CirculationDetailsDto circulationDetails) {

        addSectionHeading(
                document,
                "B. Circulation Details"
        );

        if (circulationDetails == null) {

            addText(
                    document,
                    "No circulation details available."
            );

            return;
        }

        addText(
                document,
                circulationDetails.circulationInstructions()
        );

        if (circulationDetails.entries() == null
                || circulationDetails.entries().isEmpty()) {

            return;
        }

        XWPFTable table =
                document.createTable(
                        circulationDetails.entries().size() + 1,
                        3
                );

        String[] headers = {
                "Copy No.",
                "Designation of Copy Holder",
                "Location of the Copy"
        };

        for (int i = 0; i < headers.length; i++) {

            setCellText(
                    table.getRow(0).getCell(i),
                    headers[i]
            );
        }

        for (int i = 0;
             i < circulationDetails.entries().size();
             i++) {

            CirculationDetailsEntryDto entry =
                    circulationDetails.entries().get(i);

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    entry.copyNumber() == null
                            ? ""
                            : String.valueOf(
                                    entry.copyNumber()
                            )
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    entry.designationOfCopyHolder()
            );

            setCellText(
                    table.getRow(i + 1).getCell(2),
                    entry.locationOfCopy()
            );
        }
    }

    private void addAmendments(
            XWPFDocument document,
            List<AmendmentDto> amendments) {

        addSectionHeading(
                document,
                "C. List of Amendments Made On The Previous Version No. _____"
        );

        if (amendments == null
                || amendments.isEmpty()) {

            addText(
                    document,
                    "No amendments available."
            );

            return;
        }

        XWPFTable table =
                document.createTable(
                        amendments.size() + 1,
                        5
                );

        String[] headers = {
                "Sl. No.",
                "Section No./Page No.",
                "Description of the amendment",
                "Approved by",
                "Change Request No. & Date"
        };

        for (int i = 0; i < headers.length; i++) {

            setCellText(
                    table.getRow(0).getCell(i),
                    headers[i]
            );
        }

        for (int i = 0;
             i < amendments.size();
             i++) {

            AmendmentDto amendment =
                    amendments.get(i);

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    amendment.serialNumber() == null
                            ? ""
                            : String.valueOf(
                                    amendment.serialNumber()
                            )
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    amendment.sectionNoOrPageNo()
            );

            setCellText(
                    table.getRow(i + 1).getCell(2),
                    amendment.descriptionOfAmendment()
            );

            setCellText(
                    table.getRow(i + 1).getCell(3),
                    amendment.approvedBy()
            );

            setCellText(
                    table.getRow(i + 1).getCell(4),
                    amendment.changeRequestNoAndDate()
            );
        }
    }

    private void addIntroduction(
            XWPFDocument document,
            IntroductionSddDto introduction) {

        addSectionHeading(
                document,
                "1.0 Introduction"
        );

        if (introduction == null) {

            addText(
                    document,
                    "No introduction details available."
            );

            return;
        }

        addSubHeading(
                document,
                "1.1 Purpose of this document"
        );

        addText(
                document,
                introduction.purposeOfDocument()
        );

        addScopeOfProposedSystem(
                document,
                introduction.scopeOfProposedSystem()
        );

        addDesignAlternatives(
                document,
                introduction
        );

        addMakeBuyOrReuseDecisions(
                document,
                introduction
        );
    }

    private void addScopeOfProposedSystem(
            XWPFDocument document,
            List<ScopeTaskDto> scopeTasks) {

        addSubHeading(
                document,
                "1.2 Scope of the Proposed System"
        );

        if (scopeTasks == null
                || scopeTasks.isEmpty()) {

            addText(
                    document,
                    "No scope details available."
            );

            return;
        }

        XWPFTable table =
                document.createTable(
                        scopeTasks.size() + 1,
                        3
                );

        String[] headers = {
                "Task No.",
                "Task Name",
                "Task Details"
        };

        for (int i = 0; i < headers.length; i++) {

            setCellText(
                    table.getRow(0).getCell(i),
                    headers[i]
            );
        }

        for (int i = 0;
             i < scopeTasks.size();
             i++) {

            ScopeTaskDto task =
                    scopeTasks.get(i);

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    task.taskNo() == null
                            ? ""
                            : String.valueOf(
                                    task.taskNo()
                            )
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    task.taskName()
            );

            setCellText(
                    table.getRow(i + 1).getCell(2),
                    task.taskDetails()
            );
        }
    }

    private void addDesignAlternatives(
            XWPFDocument document,
            IntroductionSddDto introduction) {

        addSubHeading(
                document,
                "1.3 Design Alternatives Considered"
        );

        if (introduction == null) {

            addText(
                    document,
                    "No design alternatives available."
            );

            return;
        }

        List<DesignAlternativeDto> alternatives =
                introduction.designAlternativesConsidered();

        if (alternatives != null
                && !alternatives.isEmpty()) {

            XWPFTable table =
                    document.createTable(
                            alternatives.size() + 1,
                            2
                    );

            String[] headers = {
                    "Design Alternatives",
                    "Description"
            };

            for (int i = 0;
                 i < headers.length;
                 i++) {

                setCellText(
                        table.getRow(0).getCell(i),
                        headers[i]
                );
            }

            for (int i = 0;
                 i < alternatives.size();
                 i++) {

                DesignAlternativeDto alternative =
                        alternatives.get(i);

                setCellText(
                        table.getRow(i + 1).getCell(0),
                        alternative.designAlternative()
                );

                setCellText(
                        table.getRow(i + 1).getCell(1),
                        alternative.description()
                );
            }
        }

        addText(
                document,
                introduction.designAlternativesDecision()
        );
    }

    private void addMakeBuyOrReuseDecisions(
            XWPFDocument document,
            IntroductionSddDto introduction) {

        addSubHeading(
                document,
                "1.4 Make, Buy or Reuse Decisions Taken"
        );

        if (introduction == null) {

            addText(
                    document,
                    "No make, buy or reuse decision details available."
            );

            return;
        }

        addText(
                document,
                introduction.makeBuyOrReuseDecisionsTaken()
        );
    }

    private void addSystemPerspective(
            XWPFDocument document,
            SystemPerspectiveDto systemPerspective) {

        addSectionHeading(
                document,
                "2.0 The System Perspective"
        );

        if (systemPerspective == null) {

            addText(
                    document,
                    "No system perspective details available."
            );

            return;
        }

        addSubHeading(
                document,
                "2.1 The System Context"
        );

        addText(
                document,
                systemPerspective.systemContext()
        );
        
        addDevelopmentTestingDeploymentEnvironment(
                document,
                systemPerspective.developmentTestingDeploymentEnvironment()
        );
        
        addApplicationComponents(
                document,
                systemPerspective.applicationComponents()
        );
        
        addGeneralFeatures(
                document,
                systemPerspective.generalFeaturesOfProposedApplication()
        );
    }
    
    private void addDevelopmentTestingDeploymentEnvironment(
            XWPFDocument document,
            EnvironmentDto environment) {

        addSubHeading(
                document,
                "2.2 The Development, Testing, and Deployment Environment"
        );

        if (environment == null) {
            addText(
                    document,
                    "No development, testing, and deployment environment details available."
            );
            return;
        }

        addEnvironmentDetails(
                document,
                "2.2.1 Development Environment",
                "2.2.1.1 Software Needed",
                "2.2.1.2 Hardware Needed",
                "2.2.1.3 Network needed",
                environment.developmentEnvironment()
        );

        addEnvironmentDetails(
                document,
                "2.2.2 Testing Environment",
                "2.2.2.1 Software Needed",
                "2.2.2.2 Hardware Needed",
                "2.2.2.3 Network needed",
                environment.testingEnvironment()
        );

        addEnvironmentDetails(
                document,
                "2.2.3 Deployment Environment",
                "2.2.3.1 Software Needed",
                "2.2.3.2 Hardware Needed",
                "2.2.3.3 Network needed",
                environment.deploymentEnvironment()
        );
    }
    
    private void addEnvironmentDetails(
            XWPFDocument document,
            String environmentHeading,
            String softwareHeading,
            String hardwareHeading,
            String networkHeading,
            EnvironmentDetailsDto environmentDetails) {

        addSubHeading(
                document,
                environmentHeading
        );

        if (environmentDetails == null) {
            addText(
                    document,
                    "No environment details available."
            );
            return;
        }

        addSubHeading(
                document,
                softwareHeading
        );

        addEnvironmentSoftwareTable(
                document,
                environmentDetails.softwareNeeded()
        );

        addSubHeading(
                document,
                hardwareHeading
        );

        addText(
                document,
                environmentDetails.hardwareNeeded()
        );

        addSubHeading(
                document,
                networkHeading
        );

        addText(
                document,
                environmentDetails.networkNeeded()
        );
    }
    
    private void addEnvironmentSoftwareTable(
            XWPFDocument document,
            List<EnvironmentItemDto> softwareNeeded) {

        if (softwareNeeded == null
                || softwareNeeded.isEmpty()) {

            addText(
                    document,
                    "No software details available."
            );

            return;
        }

        XWPFTable table =
                document.createTable(
                        softwareNeeded.size() + 1,
                        2
                );

        setCellText(
                table.getRow(0).getCell(0),
                "Technology Area"
        );

        setCellText(
                table.getRow(0).getCell(1),
                "Softwares and Products, Services, or Standards"
        );

        for (int i = 0;
             i < softwareNeeded.size();
             i++) {

            EnvironmentItemDto item =
                    softwareNeeded.get(i);

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    item.technologyArea()
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    item.productServiceOrStandard()
            );
        }
    }
    
    private void addApplicationComponents(
            XWPFDocument document,
            String applicationComponents) {

        addSubHeading(
                document,
                "2.3 The Application Components"
        );

        addText(
                document,
                applicationComponents
        );
    }
    
    private void addGeneralFeatures(
            XWPFDocument document,
            String generalFeatures) {

        addSubHeading(
                document,
                "2.4 General Features of the Proposed Application"
        );

        addText(
                document,
                generalFeatures
        );
    }
    
    private void addApplicationComponents(
            XWPFDocument document,
            List<ApplicationComponentDto> applicationComponents) {

        addSectionHeading(
                document,
                "3.0 Specific Description of the Application Components"
        );

        if (applicationComponents == null
                || applicationComponents.isEmpty()) {

            addText(
                    document,
                    "No application components available."
            );

            return;
        }

        for (ApplicationComponentDto component
                : applicationComponents) {

            String componentNumber =
                    component.componentNumber();

            String componentName =
                    component.componentName();

            addSubHeading(
                    document,
                    componentNumber
                            + " Specific Description of Component "
                            + componentNumber
                            + ": "
                            + componentName
            );

            if (component.details() == null
                    || component.details().isEmpty()) {

                addText(
                        document,
                        "No component details available."
                );

                continue;
            }

            for (ApplicationComponentDetailsDto detail
                    : component.details()) {

                addApplicationComponentDetail(
                        document,
                        detail
                );
            }
        }
    }
    
    private void addApplicationComponentDetail(
            XWPFDocument document,
            ApplicationComponentDetailsDto detail) {

        addSubHeading(
                document,
                detail.subComponentNumber()
                        + " "
                        + detail.subComponentName()
        );

        addText(
                document,
                "Description: "
                        + safe(detail.description())
        );

        addText(
                document,
                "Primary Actor: "
                        + safe(detail.primaryActor())
        );

        addText(
                document,
                "Secondary Actor: "
                        + safe(detail.secondaryActor())
        );

        addText(
                document,
                "Precondition: "
                        + safe(detail.precondition())
        );

        addText(
                document,
                "Basic flow:"
        );

        addStringList(
                document,
                detail.basicFlow()
        );

        addText(
                document,
                "Business Rules:"
        );

        addStringList(
                document,
                detail.businessRules()
        );

        addText(
                document,
                "Post condition: "
                        + safe(detail.postCondition())
        );

        addText(
                document,
                "UI Design:"
        );

        addText(
                document,
                detail.uiDesign()
        );

        addDesignDetails(
                document,
                detail.designDetails()
        );
        
        addPlantUmlDiagram(
                document,
                detail.sequenceDiagram(),
                "Sequence Diagram – " + safe(detail.subComponentName())
        );
    }
    
    private void addStringList(
            XWPFDocument document,
            List<String> items) {

        if (items == null || items.isEmpty()) {
            addText(document, "Not specified");
            return;
        }

        for (String item : items) {

            if (item == null || item.isBlank()) {
                continue;
            }

            XWPFParagraph paragraph =
                    document.createParagraph();

            paragraph.setIndentationLeft(360);

            XWPFRun run =
                    paragraph.createRun();

            run.setText("• " + item);
            run.setFontSize(12);
        }
    }
    
    private String safe(String value) {

        return value == null || value.isBlank()
                ? "Not specified"
                : value;
    }
    
    private void addDesignDetails(
            XWPFDocument document,
            List<DesignDetailDto> designDetails) {

        addText(
                document,
                "Design Details:"
        );

        if (designDetails == null
                || designDetails.isEmpty()) {

            addText(
                    document,
                    "Not specified"
            );

            return;
        }

        XWPFTable table =
                document.createTable(
                        designDetails.size() + 1,
                        6
                );

        String[] headers = {
                "SL. No.",
                "Request Jsp",
                "Response Jsp",
                "Controller",
                "DAO",
                "Entity Name"
        };

        for (int i = 0; i < headers.length; i++) {

            setCellText(
                    table.getRow(0).getCell(i),
                    headers[i]
            );
        }

        for (int i = 0;
             i < designDetails.size();
             i++) {

            DesignDetailDto detail =
                    designDetails.get(i);

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    detail.serialNumber() == null
                            ? ""
                            : String.valueOf(
                                    detail.serialNumber()
                            )
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    safe(detail.requestJsp())
            );

            setCellText(
                    table.getRow(i + 1).getCell(2),
                    safe(detail.responseJsp())
            );

            setCellText(
                    table.getRow(i + 1).getCell(3),
                    safe(detail.controller())
            );

            setCellText(
                    table.getRow(i + 1).getCell(4),
                    safe(detail.dao())
            );

            setCellText(
                    table.getRow(i + 1).getCell(5),
                    safe(detail.entityName())
            );
        }
    }
    
    private void addDatabaseDesign(
            XWPFDocument document,
            DatabaseDesignDto databaseDesign) {

        addSectionHeading(
                document,
                "4.0 Proposed Database Design"
        );

        if (databaseDesign == null) {
            addText(
                    document,
                    "No database design details available."
            );
            return;
        }

        addText(
                document,
                databaseDesign.description()
        );
    }
    
    private void addSpecialConsiderations(
            XWPFDocument document,
            SpecialConsiderationsDto specialConsiderations) {

        addSectionHeading(
                document,
                "5.0 Special Considerations"
        );

        if (specialConsiderations == null) {
            addText(
                    document,
                    "No special considerations available."
            );
            return;
        }

        addSubHeading(
                document,
                "5.1 Design Rules/Criteria to Follow"
        );

        addText(
                document,
                specialConsiderations.designRulesCriteriaToFollow()
        );

        addSubHeading(
                document,
                "5.2 Programming Rules to Follow"
        );

        addText(
                document,
                specialConsiderations.programmingRulesToFollow()
        );

        addSubHeading(
                document,
                "5.3 Error Handling Procedures"
        );

        addText(
                document,
                specialConsiderations.errorHandlingProcedures()
        );

        addSubHeading(
                document,
                "5.4 Special Security Provisions"
        );

        addText(
                document,
                specialConsiderations.specialSecurityProvisions()
        );

        addSubHeading(
                document,
                "5.5 Special Recovery Procedure"
        );

        addText(
                document,
                specialConsiderations.specialRecoveryProcedure()
        );

        addSubHeading(
                document,
                "5.6 Audit tracing facility"
        );

        addText(
                document,
                specialConsiderations.auditTracingFacility()
        );

        addSubHeading(
                document,
                "5.7 System implementation procedures"
        );

        addText(
                document,
                specialConsiderations.systemImplementationProcedures()
        );

        addSubHeading(
                document,
                "5.8 Database administration procedure"
        );

        addText(
                document,
                specialConsiderations.databaseAdministrationProcedure()
        );
    }
    
    private void addSystemIntegrationStrategy(
            XWPFDocument document,
            SystemIntegrationStrategyDto integrationStrategy) {

        addSectionHeading(
                document,
                "6.0 System Integration Strategy"
        );

        if (integrationStrategy == null) {
            addText(
                    document,
                    "No system integration strategy available."
            );
            return;
        }

        addSubHeading(
                document,
                "6.1 Desired Strategy for Integration of Product Modules"
        );

        addText(
                document,
                integrationStrategy
                        .desiredStrategyForIntegrationOfProductModules()
        );

        addSubHeading(
                document,
                "6.2 Desired Sequence & Criteria for Integration Testing"
        );

        addText(
                document,
                integrationStrategy
                        .desiredSequenceAndCriteriaForIntegrationTesting()
        );
    }
    
    private void addRequirementsTraceabilityMatrix(
            XWPFDocument document,
            List<SddTraceabilityMatrixEntryDto> entries) {

        addSectionHeading(
                document,
                "7.0 Requirements Traceability Matrix"
        );

        if (entries == null || entries.isEmpty()) {

            addText(
                    document,
                    "No requirements traceability information available."
            );

            return;
        }

        XWPFTable table =
                document.createTable(
                        entries.size() + 1,
                        3
                );

        String[] headers = {
                "Requirement ID",
                "Requirement Description",
                "Design Component"
        };

        for (int i = 0; i < headers.length; i++) {

            setCellText(
                    table.getRow(0).getCell(i),
                    headers[i]
            );
        }

        for (int i = 0; i < entries.size(); i++) {

            SddTraceabilityMatrixEntryDto entry =
                    entries.get(i);

            setCellText(
                    table.getRow(i + 1).getCell(0),
                    safe(entry.requirementId())
            );

            setCellText(
                    table.getRow(i + 1).getCell(1),
                    safe(entry.requirementDescription())
            );

            setCellText(
                    table.getRow(i + 1).getCell(2),
                    safe(entry.designComponent())
            );
        }
    }
    
    private byte[] renderPlantUmlToPng(String plantUml) throws IOException {

        if (plantUml == null || plantUml.isBlank()) {
            return null;
        }

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        SourceStringReader reader =
                new SourceStringReader(plantUml);

        reader.outputImage(outputStream);

        return outputStream.toByteArray();
    }
    
    private void addPlantUmlDiagram(
            XWPFDocument document,
            String plantUml,
            String caption) {

        if (plantUml == null || plantUml.isBlank()) {
            return;
        }

        try {
            byte[] imageBytes = renderPlantUmlToPng(plantUml);

            if (imageBytes == null || imageBytes.length == 0) {
                return;
            }

            XWPFParagraph captionParagraph =
                    document.createParagraph();

            XWPFRun captionRun =
                    captionParagraph.createRun();

            captionRun.setBold(true);
            captionRun.setText(caption);

            XWPFParagraph imageParagraph =
                    document.createParagraph();

            XWPFRun imageRun =
                    imageParagraph.createRun();

            imageRun.addPicture(
                    new java.io.ByteArrayInputStream(imageBytes),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "sequence-diagram.png",
                    5000000,
                    3000000
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to render PlantUML diagram",
                    e
            );
        }
    }
}