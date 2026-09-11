package com.projectestimation.backend.pmp.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.estimation.repository.EstimationUseCaseRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.pmp.ai.GeminiPmpOrchestrator;
import com.projectestimation.backend.pmp.dto.PmpDto;
import com.projectestimation.backend.pmp.dto.PmpGenerationResponse;
import com.projectestimation.backend.pmp.dto.PmpItemDto;
import com.projectestimation.backend.pmp.dto.ValidationPlanDto;
import com.projectestimation.backend.pmp.model.Pmp;
import com.projectestimation.backend.pmp.repository.PmpRepository;


@Service
public class PmpService {

    private static final String BLUE = "5B9BD5";
    private static final String WHITE = "FFFFFF";
    private static final String BLACK = "000000";

    private final OpportunityRepository opportunityRepository;
    private final EstimationAnalysisRepository estimationAnalysisRepository;
    private final EstimationUseCaseRepository estimationUseCaseRepository;
    private final GeminiPmpOrchestrator geminiPmpOrchestrator;
    private final PmpRepository pmpRepository;
    private final ObjectMapper objectMapper;

    public PmpService(
            OpportunityRepository opportunityRepository,
            EstimationAnalysisRepository estimationAnalysisRepository,
            EstimationUseCaseRepository estimationUseCaseRepository,
            GeminiPmpOrchestrator geminiPmpOrchestrator,
            PmpRepository pmpRepository,
            ObjectMapper objectMapper) {
        this.opportunityRepository = opportunityRepository;
        this.estimationAnalysisRepository = estimationAnalysisRepository;
        this.estimationUseCaseRepository = estimationUseCaseRepository;
        this.geminiPmpOrchestrator = geminiPmpOrchestrator;
        this.pmpRepository = pmpRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PmpGenerationResponse generatePmp(Long opportunityId) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        EstimationAnalysis analysis = estimationAnalysisRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estimation analysis not found for this opportunity"));

        List<EstimationUseCase> useCases = estimationUseCaseRepository
                .findByEstimationAnalysisId(analysis.getId());

        if (useCases == null || useCases.isEmpty()) {
            throw new ResourceNotFoundException("No use cases found for this estimation analysis");
        }

        String response;
        try {
            response = geminiPmpOrchestrator.generate(opportunity, useCases);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed while generating PMP from Gemini: " + e.getMessage(), e);
        }

        try {
            PmpGenerationResponse generated = objectMapper.readValue(
                    response, PmpGenerationResponse.class);
            Pmp pmp = pmpRepository.findByOpportunityId(opportunityId).orElseGet(Pmp::new);
            pmp.setOpportunity(opportunity);
            pmp.setPmpData(objectMapper.writeValueAsString(generated.pmp()));
            pmpRepository.save(pmp);
            return generated;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate and save PMP: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PmpGenerationResponse getPmp(Long opportunityId) {
        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException("Opportunity not found");
        }

        Pmp pmp = pmpRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PMP not found for this opportunity"));

        try {
            return new PmpGenerationResponse(objectMapper.readValue(
                    pmp.getPmpData(), PmpDto.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read PMP data", e);
        }
    }

    @Transactional
    public PmpGenerationResponse savePmp(Long opportunityId, PmpDto pmpDto) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        try {
            Pmp pmp = pmpRepository.findByOpportunityId(opportunityId).orElseGet(Pmp::new);
            pmp.setOpportunity(opportunity);
            pmp.setPmpData(objectMapper.writeValueAsString(pmpDto));
            pmpRepository.save(pmp);
            return new PmpGenerationResponse(pmpDto);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save PMP", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] downloadPmp(Long opportunityId) throws IOException {
        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException("Opportunity not found");
        }

        Pmp pmp = pmpRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PMP not found for this opportunity"));

        try {
            PmpDto dto = objectMapper.readValue(pmp.getPmpData(), PmpDto.class);
            return generatePmpDocument(dto);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PMP document", e);
        }
    }

    private byte[] generatePmpDocument(PmpDto pmpDto) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            configureDocument(document);

            // BEAS logo - first page only
            addBeasLogo(document);

            addTitle(document, "PROJECT MANAGEMENT PLAN");
            if (pmpDto != null && pmpDto.projectOverview() != null) {
                addCenteredSubtitle(document, pmpDto.projectOverview().projectName());
            }
            addPageBreak(document);

            addTableOfContents(document);
            addPageBreak(document);

            addDocumentControl(document, pmpDto);

            addSectionHeading(document, "1.0 Introduction");
            addIntroduction(document, pmpDto);

            addSectionHeading(document, "2.0 Project Objectives & Goals");
            addProjectGoals(document, pmpDto);

            addSectionHeading(document, "3.0 The Project's Defined Process");
            addDefinedProcess(document, pmpDto);

            addSectionHeading(document, "4.0 Project Environments");
            addProjectEnvironments(document, pmpDto);

            addSectionHeading(document, "5.0 Project Management Issues");
            addProjectManagementIssues(document, pmpDto);

            addSectionHeading(document, "6.0 Project Organization & Resources");
            addOrganizationAndResources(document, pmpDto);

            addSectionHeading(document, "7.0 Project Monitoring & Control Mechanism");
            addMonitoringAndControl(document, pmpDto);

            addSectionHeading(document, "8.0 Requirement of Inter Group Support & Co-ordination");
            addInterGroupSupport(document, pmpDto);

            addSectionHeading(document, "9.0 Estimated Size & Effort");
            addEstimatedSizeEffort(document, pmpDto);

            addSectionHeading(document, "10.0 Schedule");
            addSchedule(document, pmpDto);

            addSectionHeading(document, "11.0 Metrication Plan");
            addMetricationPlan(document, pmpDto);

            addSectionHeading(document, "12.0 Quality Control Plan");
            addQualityControlPlan(document, pmpDto);

            addSectionHeading(document, "13.0 Validation Plan");
            addValidationPlan(document, pmpDto);

            addSectionHeading(document, "14.0 Quality Audit Plan");
            addQualityAuditPlan(document, pmpDto);

            addSectionHeading(document, "15.0 Configuration Management Plan");
            addConfigurationManagement(document, pmpDto);

            document.write(out);
            return out.toByteArray();
        }
    }

    // ========================== TOC =============================

    private void addTableOfContents(XWPFDocument document) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        title.setSpacingAfter(180);

        XWPFRun titleRun = title.createRun();
        titleRun.setText("Table of Contents");
        titleRun.setBold(true);
        titleRun.setFontFamily("Arial");
        titleRun.setFontSize(16);

        // Visible TOC entries. Page numbers are intentionally left blank
        // until the final DOCX pagination is verified in Word.
        addTocEntry(document, "A.", "Document Release History");
        addTocEntry(document, "B.", "Circulation Details");
        addTocEntry(document, "C.", "List of Amendments Made on the Previous Version No.:");

        addTocEntry(document, "1.0", "Introduction", true);
        addTocEntry(document, "1.1", "Project Overview");
        addTocEntry(document, "1.2", "Customer Interface");
        addTocEntry(document, "1.3", "Scope of Work");
        addTocEntry(document, "1.4", "Project Compliance Requirements");
        addTocEntry(document, "1.5", "Project Deliverables to Customer");
        addTocEntry(document, "1.6", "List of Milestones");
        addTocEntry(document, "1.7", "Acceptance Criteria");

        addTocEntry(document, "2.0", "Project Goals and Quality Objectives", true);
        addTocEntry(document, "2.1", "Project Objectives");
        addTocEntry(document, "2.2", "Project Quality Objectives");
        addTocEntry(document, "2.3", "Quality Management Objectives");

        addTocEntry(document, "3.0", "The Project's Defined Process", true);
        addTocEntry(document, "3.1", "Project Life Cycle Phases");
        addTocEntry(document, "3.2", "Methodology");
        addTocEntry(document, "3.3", "Organization");
        addTocEntry(document, "3.4", "Resources");
        addTocEntry(document, "3.5", "Estimation");
        addTocEntry(document, "3.6", "Schedule Management");
        addTocEntry(document, "3.7", "Communication");
        addTocEntry(document, "3.8", "Configuration Management");

        addTocEntry(document, "4.0", "Project Environments", true);
        addTocEntry(document, "5.0", "Project Management Issues", true);

        addTocEntry(document, "6.0", "Organization and Resources", true);
        addTocEntry(document, "6.1", "Hardware and Networking");
        addTocEntry(document, "6.2", "Software and Tools");
        addTocEntry(document, "6.3", "Manpower and Competency");
        addTocEntry(document, "6.4", "Project Team");
        addTocEntry(document, "6.5", "Training Plan");

        addTocEntry(document, "7.0", "Project Monitoring & Control Mechanism", true);
        addTocEntry(document, "7.1", "Project Monitoring & Control Mechanism");
        addTocEntry(document, "7.2", "Quantitative Project Monitoring");

        addTocEntry(document, "8.0", "Requirement of Inter Group Support & Co-ordination", true);

        addTocEntry(document, "9.0", "Estimated Size & Effort", true);
        addTocEntry(document, "9.1", "Estimated Size Details of the Project");
        addTocEntry(document, "9.2", "Estimated Effort Details of the Project");

        addTocEntry(document, "10.0", "Schedule", true);

        addTocEntry(document, "11.0", "Metrication Plan", true);
        addTocEntry(document, "11.1", "Metrication Plan Measurements of Critical Processes / Sub-processes");
        addTocEntry(document, "11.2", "Other Metrics for the Project and Corresponding Goals");
        addTocEntry(document, "11.3", "Metrics Data Capturing");

        addTocEntry(document, "12.0", "Quality Control Plan", true);
        addTocEntry(document, "12.1", "Standards Applicable");
        addTocEntry(document, "12.2", "Product Review & Testing");

        addTocEntry(document, "13.0", "Validation Plan", true);
        addTocEntry(document, "14.0", "Quality Audit Plan", true);

        addTocEntry(document, "15.0", "Configuration Management Plan", true);
        addTocEntry(document, "15.1", "List of Configuration Items (CI)");
        addTocEntry(document, "15.2", "Procedure for Baselining a CI");
        addTocEntry(document, "15.3", "Release Procedure");
        addTocEntry(document, "15.4", "Version Control & Nomenclature");
        addTocEntry(document, "15.5", "CI Status Accounting & Reporting");
        addTocEntry(document, "15.6", "Configuration Management Audit");
        addTocEntry(document, "15.7", "Back-up Plan");
    }

    private void addTocEntry(
            XWPFDocument document,
            String number,
            String title) {
        addTocEntry(document, number, title, false);
    }

    private void addTocEntry(
            XWPFDocument document,
            String number,
            String title,
            boolean mainEntry) {

        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(mainEntry ? 55 : 35);

        XWPFRun numberRun = paragraph.createRun();
        numberRun.setText(number);
        numberRun.setBold(mainEntry);
        numberRun.setFontFamily("Arial");
        numberRun.setFontSize(10);

        XWPFRun spaceRun = paragraph.createRun();
        spaceRun.setText("    ");
        spaceRun.setFontFamily("Arial");
        spaceRun.setFontSize(10);

        XWPFRun titleRun = paragraph.createRun();
        titleRun.setText(title);
        titleRun.setBold(mainEntry);
        titleRun.setFontFamily("Arial");
        titleRun.setFontSize(10);

        XWPFRun dotsRun = paragraph.createRun();
        dotsRun.setText(" ........................................................................................................................");
        dotsRun.setFontFamily("Arial");
        dotsRun.setFontSize(10);
    }

    // ======================= DOCUMENT CONTROL ===================

    private void addDocumentControl(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.documentControl() == null) return;

        addSectionHeading(document, "A. Document Release History");
        addPmpItemsTable(document, "Document Release History",
                dto.documentControl().releaseHistory());

        addSectionHeading(document, "B. Circulation Details");
        addPmpItemsTable(document, "Circulation Details",
                dto.documentControl().circulationDetails());

        addSectionHeading(document,
                "C. List of Amendments Made on the Previous Version No.:");
        addPmpItemsTable(document, "List of Amendments",
                dto.documentControl().amendments());
    }

    // =========================== 1.0 =============================

    private void addIntroduction(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.projectOverview() == null) return;

        addSubHeading(document, "1.1 Project Overview");
        addLabelValueTable(document, new String[][] {
                {"Project Name", dto.projectOverview().projectName()},
                {"Project Description", dto.projectOverview().projectDescription()},
                {"Project Scope", dto.projectOverview().projectScope()}
        });

        addSubHeading(document, "1.2 Customer Interface");
        addDynamicRecordListTable(document, "Customer Interface",
                singletonObject(dto.projectOverview().customerInterface()));

        addSubHeading(document, "1.3 Scope of Work");
        addListTable(document, "Objectives", dto.projectOverview().objectives());
        addListTable(document, "Deliverables", dto.projectOverview().deliverables());
        addListTable(document, "Assumptions", dto.projectOverview().assumptions());
        addListTable(document, "Constraints", dto.projectOverview().constraints());

        addSubHeading(document, "1.4 Project Compliance Requirements");
        addListTable(document, "Compliance Requirements",
                dto.projectOverview().complianceRequirements());

        addSubHeading(document, "1.5 Project Deliverables to Customer");
        addDynamicRecordListTable(document, "Project Deliverables",
                dto.projectOverview().detailedDeliverables());

        addSubHeading(document, "1.6 List of Milestones");
        addDynamicRecordListTable(document, "Milestones",
                dto.projectOverview().milestones());

        addSubHeading(document, "1.7 Acceptance Criteria");
        addListTable(document, "Acceptance Criteria",
                dto.projectOverview().acceptanceCriteria());
    }

    // =========================== 2.0 =============================

    private void addProjectGoals(XWPFDocument document, PmpDto dto) {
        if (dto == null) return;

        if (dto.projectOverview() != null) {
            addSubHeading(document, "2.1 Organization's Business Objectives");
            addListTable(document, "Business Objectives",
                    dto.projectOverview().objectives());
        }
        if (dto.projectManagement() != null) {
            addSubHeading(document, "2.2 Project Quality Objectives");
            addListTable(document, "Project Quality Objectives",
                    dto.projectManagement().qualityObjectives());
        }
        if (dto.qualityManagement() != null) {
            addSubHeading(document, "2.3 Quality Management Objectives");
            addListTable(document, "Quality Objectives",
                    dto.qualityManagement().qualityObjectives());
        }
    }

    // =========================== 3.0 =============================

    private void addDefinedProcess(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.projectManagement() == null) return;

        addSubHeading(document, "3.1 Project Life Cycle");
        addListTable(document, "Lifecycle Phases",
                dto.projectManagement().lifecyclePhases());

        addSubHeading(document, "3.2 Methodology");
        addLabelValueTable(document, new String[][] {
                {"Methodology", dto.projectManagement().methodology()}
        });

        addSubHeading(document, "3.3 Organization");
        addPmpItemsTable(document, "Organization",
                dto.projectManagement().organization());

        addSubHeading(document, "3.4 Resources");
        addPmpItemsTable(document, "Resources",
                dto.projectManagement().resources());

        addSubHeading(document, "3.5 Estimation");
        addPmpItemsTable(document, "Estimation",
                dto.projectManagement().estimation());

        addSubHeading(document, "3.6 Schedule");
        addPmpItemsTable(document, "Schedule Management",
                dto.projectManagement().schedule());

        addSubHeading(document, "3.7 Communication");
        addPmpItemsTable(document, "Communication",
                dto.projectManagement().communication());

        addSubHeading(document, "3.8 Configuration Management");
        addPmpItemsTable(document, "Configuration Management",
                dto.projectManagement().configurationManagement());
    }

    // =========================== 4.0 =============================

    private void addProjectEnvironments(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.environment() == null) return;

        addSubHeading(document, "4.1 Development Environment");
        addSinglePmpItemTable(document, "Development Environment",
                dto.environment().development());

        addSubHeading(document, "4.2 Testing Environment");
        addSinglePmpItemTable(document, "Testing Environment",
                dto.environment().testing());

        addSubHeading(document, "4.3 Operational Environment");
        addSinglePmpItemTable(document, "Operational Environment",
                dto.environment().operation());
    }

    // =========================== 5.0 =============================

    private void addProjectManagementIssues(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.riskManagement() == null) return;

        addSubHeading(document, "5.1 Project Risks");
        addPmpItemsTable(document, "Risks", dto.riskManagement().risks());

        addSubHeading(document, "5.2 Risk Mitigation Strategies");
        addPmpItemsTable(document, "Mitigation Strategies",
                dto.riskManagement().mitigationStrategies());

        addSubHeading(document, "5.3 Contingency Plans");
        addPmpItemsTable(document, "Contingency Plans",
                dto.riskManagement().contingencyPlans());

        addSubHeading(document, "5.4 Dependencies");
        addPmpItemsTable(document, "Dependencies",
                dto.riskManagement().dependencies());

        addSubHeading(document, "5.5 Assumptions");
        addPmpItemsTable(document, "Assumptions",
                dto.riskManagement().assumptions());
    }

    // =========================== 6.0 =============================

    private void addOrganizationAndResources(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.organizationResources() == null) return;

        addSubHeading(document, "6.1 Hardware & Networking");
        addPmpItemsTable(document, "Hardware & Networking",
                dto.organizationResources().hardwareNetworking());

        addSubHeading(document, "6.2 Software & Tools");
        addPmpItemsTable(document, "Software & Tools",
                dto.organizationResources().softwareTools());

        addSubHeading(document, "6.3 Manpower & Competency");
        addPmpItemsTable(document, "Manpower & Competency",
                dto.organizationResources().manpowerCompetency());

        addSubHeading(document, "6.4 Project Team");
        addPmpItemsTable(document, "Project Team",
                dto.organizationResources().projectTeam());

        addSubHeading(document, "6.5 Training Plan");
        addPmpItemsTable(document, "Training Plan",
                dto.organizationResources().trainingPlan());
    }

    // =========================== 7.0 =============================

    private void addMonitoringAndControl(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.monitoringControl() == null) return;

        addSubHeading(document, "7.1 Project Monitoring & Control Mechanism");
        addPmpItemsTable(document, "Monitoring Mechanism",
                dto.monitoringControl().monitoringMechanism());

        addSubHeading(document, "7.2 Quantitative Project Monitoring");
        addPmpItemsTable(document, "Quantitative Monitoring",
                dto.monitoringControl().quantitativeMonitoring());
    }

    // =========================== 8.0 =============================

    private void addInterGroupSupport(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.interGroupSupport() == null) return;
        addPmpItemsTable(document, "Inter Group Support & Co-ordination",
                dto.interGroupSupport().supportItems());
    }

    // =========================== 9.0 =============================

    private void addEstimatedSizeEffort(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.estimatedSizeEffort() == null) return;

        addSubHeading(document, "9.1 Estimated Size Details of the Project");
        addPmpItemsTable(document, "Estimated Size",
                dto.estimatedSizeEffort().sizeDetails());

        addSubHeading(document, "9.2 Estimated Effort Details of the Project");
        addPmpItemsTable(document, "Estimated Effort",
                dto.estimatedSizeEffort().effortDetails());
    }

    // =========================== 10.0 ============================

    private void addSchedule(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.schedule() == null) return;
        addPmpItemsTable(document, "Project Schedule",
                dto.schedule().scheduleItems());
    }

    // =========================== 11.0 ============================

    private void addMetricationPlan(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.metricationPlan() == null) return;

        addSubHeading(document,
                "11.1 Metrication Plan Measurements of Critical Processes / Sub-processes");
        addPmpItemsTable(document, "Critical Process / Sub-process Metrics",
                dto.metricationPlan().criticalProcessMetrics());

        addSubHeading(document,
                "11.2 Other Metrics for the Project and Corresponding Goals");
        addPmpItemsTable(document, "Other Project Metrics",
                dto.metricationPlan().otherMetrics());

        addSubHeading(document, "11.3 Metrics Data Capturing");
        addPmpItemsTable(document, "Metrics Data Capturing",
                dto.metricationPlan().dataCapturing());
    }

    // =========================== 12.0 ============================

    private void addQualityControlPlan(XWPFDocument document, PmpDto dto) {
        if (dto == null) return;

        if (dto.qualityControlPlan() != null) {
            addSubHeading(document, "12.1 Standards Applicable");
            addPmpItemsTable(document, "Standards Applicable",
                    dto.qualityControlPlan().standardsApplicable());

            addSubHeading(document, "12.2 Product Review & Testing");
            addPmpItemsTable(document, "Product Review & Testing",
                    dto.qualityControlPlan().productReviewTesting());
        }

        if (dto.qualityManagement() != null) {
            addSubHeading(document, "12.3 Quality Standards");
            addPmpItemsTable(document, "Quality Standards",
                    dto.qualityManagement().qualityStandards());

            addSubHeading(document, "12.4 Reviews");
            addPmpItemsTable(document, "Reviews",
                    dto.qualityManagement().reviews());

            addSubHeading(document, "12.5 Testing");
            addPmpItemsTable(document, "Testing",
                    dto.qualityManagement().testing());

            addSubHeading(document, "12.6 Quality Metrics");
            addPmpItemsTable(document, "Quality Metrics",
                    dto.qualityManagement().metrics());

            addSubHeading(document, "12.7 Quality Audits");
            addPmpItemsTable(document, "Quality Audits",
                    dto.qualityManagement().audits());

            addSubHeading(document, "12.8 Product Reviews");
            addPmpItemsTable(document, "Product Reviews",
                    dto.qualityManagement().productReviews());
        }
    }

    // =========================== 13.0 ============================

    private void addValidationPlan(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.validationPlan() == null) return;

        ValidationPlanDto validation = dto.validationPlan();

        addLabelValueTable(document, new String[][] {
                {"Validation Name", validation.name()},
                {"Description", validation.description()},
                {"Responsible", validation.responsible()},
                {"Timing", validation.timing()},
                {"Target", validation.target()},
                {"Status", validation.status()}
        });

        addSubHeading(document, "13.1 Validation Activities");
        addPmpItemsTable(document, "Validation Activities",
                validation.activities());
    }

    // =========================== 14.0 ============================

    private void addQualityAuditPlan(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.qualityAuditPlan() == null) return;
        addPmpItemsTable(document, "Quality Audits",
                dto.qualityAuditPlan().audits());
    }

    // =========================== 15.0 ============================

    private void addConfigurationManagement(XWPFDocument document, PmpDto dto) {
        if (dto == null || dto.configurationManagementPlan() == null) return;

        addSubHeading(document, "15.1 List of Configuration Items (CI)");
        addPmpItemsTable(document, "Configuration Items",
                dto.configurationManagementPlan().configurationItems());

        addSubHeading(document, "15.2 Procedure for Baselining a CI");
        addText(document, dto.configurationManagementPlan().baselining());

        addSubHeading(document, "15.3 Release Procedure");
        addText(document, dto.configurationManagementPlan().releaseProcedure());

        addSubHeading(document, "15.4 Version Control & Nomenclature");
        addText(document, dto.configurationManagementPlan().versionControl());

        addSubHeading(document, "15.5 CI Status Accounting & Reporting");
        addText(document, dto.configurationManagementPlan().statusAccounting());

        addSubHeading(document, "15.6 Configuration Management Audit");
        addText(document, dto.configurationManagementPlan().audit());

        addSubHeading(document, "15.7 Back-up Plan");
        addText(document, dto.configurationManagementPlan().backup());
    }

    // ========================== PMP TABLE ========================

    private void addPmpItemsTable(
            XWPFDocument document,
            String title,
            List<PmpItemDto> items) {

        if (items == null || items.isEmpty()) return;

        List<PmpItemDto> valid = new ArrayList<>();
        for (PmpItemDto item : items) if (item != null) valid.add(item);
        if (valid.isEmpty()) return;

        addTableTitle(document, title);

        XWPFTable table = document.createTable(valid.size() + 1, 6);
        formatTable(table);

        String[] headers = {
                "Name", "Description", "Responsible", "Timing", "Target", "Status"
        };

        for (int i = 0; i < headers.length; i++) {
            setCellText(table.getRow(0).getCell(i), headers[i], true);
        }

        for (int i = 0; i < valid.size(); i++) {
            PmpItemDto item = valid.get(i);
            XWPFTableRow row = table.getRow(i + 1);

            setCellText(row.getCell(0), item.name(), false);
            setCellText(row.getCell(1), item.description(), false);
            setCellText(row.getCell(2), item.responsible(), false);
            setCellText(row.getCell(3), item.timing(), false);
            setCellText(row.getCell(4), item.target(), false);
            setCellText(row.getCell(5), item.status(), false);
        }

        addSpacer(document);
    }

    private void addSinglePmpItemTable(
            XWPFDocument document,
            String title,
            PmpItemDto item) {

        if (item == null) return;

        addTableTitle(document, title);

        XWPFTable table = document.createTable(7, 2);
        formatTable(table);

        setCellText(table.getRow(0).getCell(0), "Field", true);
        setCellText(table.getRow(0).getCell(1), "Details", true);

        String[][] values = {
                {"Name", item.name()},
                {"Description", item.description()},
                {"Responsible", item.responsible()},
                {"Timing", item.timing()},
                {"Target", item.target()},
                {"Status", item.status()}
        };

        for (int i = 0; i < values.length; i++) {
            setCellText(table.getRow(i + 1).getCell(0), values[i][0], false);
            setCellText(table.getRow(i + 1).getCell(1), values[i][1], false);
        }

        addSpacer(document);
    }

    // =========================== LIST TABLE ======================

    private void addListTable(
            XWPFDocument document,
            String title,
            List<String> values) {

        if (values == null || values.isEmpty()) return;

        List<String> valid = new ArrayList<>();
        for (String value : values) {
            if (!isBlank(value)) valid.add(value.trim());
        }
        if (valid.isEmpty()) return;

        addTableTitle(document, title);

        XWPFTable table = document.createTable(valid.size() + 1, 2);
        formatTable(table);

        setCellText(table.getRow(0).getCell(0), "Sl. No.", true);
        setCellText(table.getRow(0).getCell(1), title, true);

        for (int i = 0; i < valid.size(); i++) {
            setCellText(table.getRow(i + 1).getCell(0), String.valueOf(i + 1), false);
            setCellText(table.getRow(i + 1).getCell(1), valid.get(i), false);
        }

        addSpacer(document);
    }

    // ======================== LABEL VALUE ========================

    /*
     * IMPORTANT: String[][] is intentional. It avoids the previous
     * List.of(new String[]{...}) generic/varargs compilation error.
     */
    private void addLabelValueTable(
            XWPFDocument document,
            String[][] rows) {

        if (document == null || rows == null || rows.length == 0) return;

        List<String[]> valid = new ArrayList<>();
        for (String[] row : rows) {
            if (row != null && row.length >= 2) {
                valid.add(new String[] {safe(row[0]), safe(row[1])});
            }
        }
        if (valid.isEmpty()) return;

        XWPFTable table = document.createTable(valid.size(), 2);
        formatTable(table);

        for (int i = 0; i < valid.size(); i++) {
            setCellText(table.getRow(i).getCell(0), valid.get(i)[0], true);
            setCellText(table.getRow(i).getCell(1), valid.get(i)[1], false);
        }

        addSpacer(document);
    }

    // ====================== DYNAMIC DTO TABLE ====================

    private void addDynamicRecordListTable(
            XWPFDocument document,
            String title,
            List<?> objects) {

        if (objects == null || objects.isEmpty()) return;

        List<Object> valid = new ArrayList<>();
        for (Object object : objects) if (object != null) valid.add(object);
        if (valid.isEmpty()) return;

        List<String> fields = getFieldNames(valid.get(0).getClass());
        if (fields.isEmpty()) {
            addFallbackObjectTable(document, title, valid);
            return;
        }

        addTableTitle(document, title);

        XWPFTable table = document.createTable(valid.size() + 1, fields.size() + 1);
        formatTable(table);

        setCellText(table.getRow(0).getCell(0), "Sl. No.", true);
        for (int i = 0; i < fields.size(); i++) {
            setCellText(table.getRow(0).getCell(i + 1),
                    formatFieldName(fields.get(i)), true);
        }

        for (int rowIndex = 0; rowIndex < valid.size(); rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex + 1);
            Object object = valid.get(rowIndex);

            setCellText(row.getCell(0), String.valueOf(rowIndex + 1), false);
            for (int column = 0; column < fields.size(); column++) {
                setCellText(row.getCell(column + 1),
                        readProperty(object, fields.get(column)), false);
            }
        }

        addSpacer(document);
    }

    private void addFallbackObjectTable(
            XWPFDocument document,
            String title,
            List<?> objects) {

        addTableTitle(document, title);
        XWPFTable table = document.createTable(objects.size() + 1, 2);
        formatTable(table);

        setCellText(table.getRow(0).getCell(0), "Sl. No.", true);
        setCellText(table.getRow(0).getCell(1), title, true);

        for (int i = 0; i < objects.size(); i++) {
            setCellText(table.getRow(i + 1).getCell(0), String.valueOf(i + 1), false);
            setCellText(table.getRow(i + 1).getCell(1), objectToText(objects.get(i)), false);
        }
        addSpacer(document);
    }

    // ======================= REFLECTION ==========================

    private List<String> getFieldNames(Class<?> type) {
        if (type == null) return Collections.emptyList();

        List<String> fields = new ArrayList<>();

        try {
            if (type.isRecord()) {
                for (RecordComponent component : type.getRecordComponents()) {
                    fields.add(component.getName());
                }
                return fields;
            }

            for (Method method : type.getMethods()) {
                if (method.getParameterCount() != 0) continue;
                String name = method.getName();
                if ("getClass".equals(name)) continue;
                if (name.startsWith("get") && name.length() > 3) {
                    fields.add(Character.toLowerCase(name.charAt(3)) + name.substring(4));
                }
            }
        } catch (Exception ignored) {
            // Keep fields already discovered.
        }

        return fields;
    }

    private String readProperty(Object object, String propertyName) {
        if (object == null || isBlank(propertyName)) return "";

        try {
            Class<?> type = object.getClass();

            if (type.isRecord()) {
                for (RecordComponent component : type.getRecordComponents()) {
                    if (component.getName().equals(propertyName)) {
                        return objectToText(component.getAccessor().invoke(object));
                    }
                }
            }

            String suffix = Character.toUpperCase(propertyName.charAt(0))
                    + propertyName.substring(1);
            Method getter = type.getMethod("get" + suffix);
            return objectToText(getter.invoke(object));

        } catch (Exception ignored) {
            return "";
        }
    }

    private String objectToText(Object value) {
        if (value == null) return "";

        if (value instanceof List<?>) {
            StringBuilder builder = new StringBuilder();
            for (Object item : (List<?>) value) {
                if (item == null) continue;
                if (builder.length() > 0) builder.append(", ");
                builder.append(objectToText(item));
            }
            return builder.toString();
        }

        if (value.getClass().isArray()) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                return String.valueOf(value);
            }
        }

        if (value.getClass().isRecord()) {
            StringBuilder builder = new StringBuilder();
            for (String field : getFieldNames(value.getClass())) {
                String fieldValue = readProperty(value, field);
                if (isBlank(fieldValue)) continue;
                if (builder.length() > 0) builder.append("\n");
                builder.append(formatFieldName(field)).append(": ").append(fieldValue);
            }
            if (builder.length() > 0) return builder.toString();
        }

        return String.valueOf(value);
    }

    private List<?> singletonObject(Object object) {
        if (object == null) return Collections.emptyList();
        List<Object> result = new ArrayList<>();
        result.add(object);
        return result;
    }

    // =========================== FORMATTING =====================

    private void configureDocument(XWPFDocument document) {
        try {
            if (document.getDocument().getBody().getSectPr() != null
                    && document.getDocument().getBody().getSectPr().getPgMar() != null) {
                document.getDocument().getBody().getSectPr().getPgMar().setTop(720);
                document.getDocument().getBody().getSectPr().getPgMar().setBottom(720);
                document.getDocument().getBody().getSectPr().getPgMar().setLeft(720);
                document.getDocument().getBody().getSectPr().getPgMar().setRight(720);
            }
        } catch (Exception ignored) {
            // Use Word defaults if page margins cannot be changed.
        }
    }

    private void addBeasLogo(XWPFDocument document) throws IOException {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(80);

        XWPFRun run = paragraph.createRun();

        try (InputStream logoStream =
                getClass().getClassLoader()
                        .getResourceAsStream("psr/beas-logo.png")) {

            if (logoStream == null) {
                throw new IOException(
                        "BEAS logo not found: src/main/resources/psr/beas-logo.png");
            }

            try {
                run.addPicture(
                        logoStream,
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "beas-logo.png",
                        Units.toEMU(158),
                        Units.toEMU(24)
                );
            } catch (Exception e) {
                throw new IOException("Failed to add BEAS logo to PMP document", e);
            }
        }
    }

    private void addSectionHeading(XWPFDocument document, String text) {
        if (isBlank(text)) return;
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setStyle("Heading1");
        paragraph.setSpacingBefore(180);
        paragraph.setSpacingAfter(90);

        XWPFRun run = paragraph.createRun();
        run.setText(safe(text));
        run.setBold(true);
        run.setFontFamily("Arial");
        run.setFontSize(15);
        run.setColor(BLUE);
    }

    private void addSubHeading(XWPFDocument document, String text) {
        if (isBlank(text)) return;
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setStyle("Heading2");
        paragraph.setSpacingBefore(100);
        paragraph.setSpacingAfter(50);

        XWPFRun run = paragraph.createRun();
        run.setText(safe(text));
        run.setBold(true);
        run.setFontFamily("Arial");
        run.setFontSize(11);
        run.setColor(BLACK);
    }

    private void addTableTitle(XWPFDocument document, String text) {
        if (isBlank(text)) return;
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(50);
        paragraph.setSpacingAfter(35);
        XWPFRun run = paragraph.createRun();
        run.setText(safe(text));
        run.setBold(true);
        run.setFontFamily("Arial");
        run.setFontSize(10);
        run.setColor(BLUE);
    }

    private void addText(XWPFDocument document, String text) {
        if (isBlank(text)) return;
        for (String part : text.split("\\r?\\n")) {
            if (isBlank(part)) continue;
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setSpacingAfter(60);
            XWPFRun run = paragraph.createRun();
            run.setText(part.trim());
            run.setFontFamily("Arial");
            run.setFontSize(10);
            run.setColor(BLACK);
        }
    }

    private void addTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(30);
        XWPFRun run = paragraph.createRun();
        run.setText(safe(text));
        run.setBold(true);
        run.setFontFamily("Arial");
        run.setFontSize(22);
        run.setColor(BLACK);
    }

    private void addCenteredSubtitle(XWPFDocument document, String text) {
        if (isBlank(text)) return;
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText(safe(text));
        run.setBold(true);
        run.setFontFamily("Arial");
        run.setFontSize(13);
        run.setColor(BLUE);
    }

    private void formatTable(XWPFTable table) {
        if (table == null) return;
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);
    }

    private void setCellText(
            XWPFTableCell cell,
            String text,
            boolean header) {

        if (cell == null) return;

        XWPFParagraph paragraph = cell.getParagraphs().isEmpty()
                ? cell.addParagraph()
                : cell.getParagraphs().get(0);

        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        // Add vertical breathing room so table rows are slightly larger
        // without increasing the table text size.
        paragraph.setSpacingBefore(20);
        paragraph.setSpacingAfter(60);

        XWPFRun run = paragraph.createRun();
        run.setText(safe(text));
        run.setFontFamily("Arial");
        run.setFontSize(9);
        run.setBold(header);

        if (header) {
            run.setColor(WHITE);
            cell.setColor(BLUE);
        } else {
            run.setColor(BLACK);
            cell.setColor(WHITE);
        }
    }

    private void addPageBreak(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.createRun().addBreak(BreakType.PAGE);
    }

    private void addSpacer(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(40);
    }

    private String formatFieldName(String value) {
        if (isBlank(value)) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) result.append(' ');
            result.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return result.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
