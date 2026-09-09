package com.projectestimation.backend.pmp.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
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
            ObjectMapper objectMapper
    ) {
        this.opportunityRepository = opportunityRepository;
        this.estimationAnalysisRepository = estimationAnalysisRepository;
        this.estimationUseCaseRepository = estimationUseCaseRepository;
        this.geminiPmpOrchestrator = geminiPmpOrchestrator;
        this.pmpRepository = pmpRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PmpGenerationResponse generatePmp(Long opportunityId) {

        // 1. Get opportunity
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Opportunity not found"));

        // 2. Get estimation analysis
        EstimationAnalysis estimationAnalysis =
                estimationAnalysisRepository.findByOpportunityId(opportunityId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Estimation analysis not found for this opportunity"
                                ));

        // 3. Get estimation use cases
        List<EstimationUseCase> useCases =
                estimationUseCaseRepository.findByEstimationAnalysisId(
                        estimationAnalysis.getId()
                );

        // 4. Validate use cases
        if (useCases.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No use cases found for this estimation analysis"
            );
        }

        // 5. Generate PMP using Gemini
        String response = geminiPmpOrchestrator.generate(
                opportunity,
                useCases
        );

        try {

            // 6. Convert Gemini response to DTO
            PmpGenerationResponse generatedResponse =
                    objectMapper.readValue(
                            response,
                            PmpGenerationResponse.class
                    );

            // 7. Convert PMP DTO to JSON
            String pmpJson =
                    objectMapper.writeValueAsString(
                            generatedResponse.pmp()
                    );

            // 8. Find existing PMP or create new one
            Pmp pmp =
                    pmpRepository.findByOpportunityId(opportunityId)
                            .orElseGet(Pmp::new);

            // 9. Set opportunity
            pmp.setOpportunity(opportunity);

            // 10. Save PMP JSON
            pmp.setPmpData(pmpJson);

            pmpRepository.save(pmp);

            // 11. Return response
            return generatedResponse;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to generate and save PMP",
                    e
            );
        }
    }

    @Transactional(readOnly = true)
    public PmpGenerationResponse getPmp(Long opportunityId) {

        // 1. Check opportunity
        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException(
                    "Opportunity not found"
            );
        }

        // 2. Find PMP
        Pmp pmp =
                pmpRepository.findByOpportunityId(opportunityId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "PMP not found for this opportunity"
                                )
                        );

        try {

            // 3. Convert stored JSON back to DTO
            PmpDto pmpDto =
                    objectMapper.readValue(
                            pmp.getPmpData(),
                            PmpDto.class
                    );

            return new PmpGenerationResponse(pmpDto);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to read PMP data",
                    e
            );
        }
    }

    @Transactional
    public PmpGenerationResponse savePmp(
            Long opportunityId,
            PmpDto pmpDto
    ) {

        // 1. Get opportunity
        Opportunity opportunity =
                opportunityRepository.findById(opportunityId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Opportunity not found"
                                )
                        );

        try {

            // 2. Convert DTO to JSON
            String pmpJson =
                    objectMapper.writeValueAsString(pmpDto);

            // 3. Find existing PMP or create new one
            Pmp pmp =
                    pmpRepository.findByOpportunityId(opportunityId)
                            .orElseGet(Pmp::new);

            // 4. Set opportunity
            pmp.setOpportunity(opportunity);

            // 5. Update PMP data
            pmp.setPmpData(pmpJson);

            // 6. Save
            pmpRepository.save(pmp);

            // 7. Return updated PMP
            return new PmpGenerationResponse(pmpDto);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to save PMP",
                    e
            );
        }
    }

    /**
     * Download PMP as Word document
     */
    @Transactional(readOnly = true)
    public byte[] downloadPmp(Long opportunityId) throws IOException {

        // 1. Check opportunity
        if (!opportunityRepository.existsById(opportunityId)) {
            throw new ResourceNotFoundException(
                    "Opportunity not found"
            );
        }

        // 2. Find PMP
        Pmp pmp =
                pmpRepository.findByOpportunityId(opportunityId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "PMP not found for this opportunity"
                                )
                        );

        try {

            // 3. Convert stored JSON to DTO
            PmpDto pmpDto =
                    objectMapper.readValue(
                            pmp.getPmpData(),
                            PmpDto.class
                    );

            // 4. Generate Word document
            return generatePmpDocument(pmpDto);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to generate PMP document",
                    e
            );
        }
    }

    /**
     * Generate PMP Word document
     */
    private byte[] generatePmpDocument(PmpDto pmpDto)
            throws IOException {

        try (
                XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            /*
             * =========================================================
             * TITLE
             * =========================================================
             */

            XWPFParagraph title =
                    document.createParagraph();

            title.setAlignment(
                    org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER
            );

            XWPFRun titleRun =
                    title.createRun();

            titleRun.setText("PROJECT MANAGEMENT PLAN");

            titleRun.setBold(true);

            titleRun.setFontSize(22);

            /*
             * =========================================================
             * PROJECT OVERVIEW
             * =========================================================
             */

            addHeading(
                    document,
                    "1. Project Overview",
                    1
            );

            if (pmpDto.projectOverview() != null) {

                addField(
                        document,
                        "Project Name",
                        pmpDto.projectOverview().projectName()
                );

                addField(
                        document,
                        "Project Description",
                        pmpDto.projectOverview().projectDescription()
                );

                addField(
                        document,
                        "Project Scope",
                        pmpDto.projectOverview().projectScope()
                );

                addList(
                        document,
                        "Objectives",
                        pmpDto.projectOverview().objectives()
                );

                addList(
                        document,
                        "Deliverables",
                        pmpDto.projectOverview().deliverables()
                );

                addList(
                        document,
                        "Assumptions",
                        pmpDto.projectOverview().assumptions()
                );

                addList(
                        document,
                        "Constraints",
                        pmpDto.projectOverview().constraints()
                );

                addList(
                        document,
                        "Acceptance Criteria",
                        pmpDto.projectOverview().acceptanceCriteria()
                );
            }

            /*
             * =========================================================
             * PROJECT MANAGEMENT
             * =========================================================
             */

            addHeading(
                    document,
                    "2. Project Management",
                    1
            );

            if (pmpDto.projectManagement() != null) {

                addField(
                        document,
                        "Methodology",
                        pmpDto.projectManagement().methodology()
                );

                addList(
                        document,
                        "Lifecycle Phases",
                        pmpDto.projectManagement().lifecyclePhases()
                );

                addItems(
                        document,
                        "Organization",
                        pmpDto.projectManagement().organization()
                );

                addItems(
                        document,
                        "Resources",
                        pmpDto.projectManagement().resources()
                );

                addItems(
                        document,
                        "Estimation",
                        pmpDto.projectManagement().estimation()
                );

                addItems(
                        document,
                        "Schedule",
                        pmpDto.projectManagement().schedule()
                );

                addItems(
                        document,
                        "Communication",
                        pmpDto.projectManagement().communication()
                );

                addItems(
                        document,
                        "Configuration Management",
                        pmpDto.projectManagement().configurationManagement()
                );
            }

            /*
             * =========================================================
             * QUALITY MANAGEMENT
             * =========================================================
             */

            addHeading(
                    document,
                    "3. Quality Management",
                    1
            );

            if (pmpDto.qualityManagement() != null) {

                addItems(
                        document,
                        "Quality Standards",
                        pmpDto.qualityManagement().qualityStandards()
                );

                addItems(
                        document,
                        "Reviews",
                        pmpDto.qualityManagement().reviews()
                );

                addItems(
                        document,
                        "Testing",
                        pmpDto.qualityManagement().testing()
                );

                addItems(
                        document,
                        "Quality Metrics",
                        pmpDto.qualityManagement().metrics()
                );
            }

            /*
             * =========================================================
             * RISK MANAGEMENT
             * =========================================================
             */

            addHeading(
                    document,
                    "4. Risk Management",
                    1
            );

            if (pmpDto.riskManagement() != null) {

                addItems(
                        document,
                        "Risks",
                        pmpDto.riskManagement().risks()
                );

                addItems(
                        document,
                        "Mitigation Strategies",
                        pmpDto.riskManagement().mitigationStrategies()
                );

                addItems(
                        document,
                        "Contingency Plans",
                        pmpDto.riskManagement().contingencyPlans()
                );
            }

            /*
             * =========================================================
             * VALIDATION PLAN
             * =========================================================
             */

            addHeading(
                    document,
                    "5. Validation Plan",
                    1
            );

            if (pmpDto.validationPlan() != null) {

                addValidationPlan(
                        document,
                        pmpDto.validationPlan()
                );
            }

            /*
             * =========================================================
             * WRITE DOCUMENT
             * =========================================================
             */

            document.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    /**
     * Add heading
     */
    private void addHeading(
            XWPFDocument document,
            String text,
            int level
    ) {

        XWPFParagraph paragraph =
                document.createParagraph();

        XWPFRun run =
                paragraph.createRun();

        run.setText(text);

        run.setBold(true);

        if (level == 1) {

            run.setFontSize(16);

        } else {

            run.setFontSize(14);
        }
    }

    /**
     * Add simple field
     */
    private void addField(
            XWPFDocument document,
            String label,
            String value
    ) {

        if (value == null || value.isBlank()) {
            return;
        }

        XWPFParagraph paragraph =
                document.createParagraph();

        XWPFRun labelRun =
                paragraph.createRun();

        labelRun.setText(label + ": ");

        labelRun.setBold(true);

        XWPFRun valueRun =
                paragraph.createRun();

        valueRun.setText(value);
    }

    /**
     * Add list
     */
    private void addList(
            XWPFDocument document,
            String heading,
            List<String> items
    ) {

        if (items == null || items.isEmpty()) {
            return;
        }

        addHeading(
                document,
                heading,
                2
        );

        for (String item : items) {

            if (item == null || item.isBlank()) {
                continue;
            }

            XWPFParagraph paragraph =
                    document.createParagraph();

            paragraph.setStyle("List Bullet");

            XWPFRun run =
                    paragraph.createRun();

            run.setText(item);
        }
    }

    /**
     * Add list of PMP items
     */
    private void addItems(
            XWPFDocument document,
            String heading,
            List<PmpItemDto> items
    ) {

        if (items == null || items.isEmpty()) {
            return;
        }

        addHeading(
                document,
                heading,
                2
        );

        for (PmpItemDto item : items) {

            if (item == null) {
                continue;
            }

            addItem(
                    document,
                    item
            );
        }
    }

    /**
     * Add one PMP item
     */
    private void addItem(
            XWPFDocument document,
            PmpItemDto item
    ) {

        if (item.name() != null && !item.name().isBlank()) {

            XWPFParagraph paragraph =
                    document.createParagraph();

            XWPFRun run =
                    paragraph.createRun();

            run.setText(item.name());

            run.setBold(true);
        }

        addField(
                document,
                "Description",
                item.description()
        );

        addField(
                document,
                "Responsible",
                item.responsible()
        );

        addField(
                document,
                "Timing",
                item.timing()
        );

        addField(
                document,
                "Target",
                item.target()
        );

        addField(
                document,
                "Status",
                item.status()
        );

        document.createParagraph();
    }

    /**
     * Add Validation Plan
     *
     * ValidationPlanDto is different from PmpItemDto,
     * so it must not be passed to addItem().
     */
    private void addValidationPlan(
            XWPFDocument document,
            ValidationPlanDto validationPlan
    ) {

        if (validationPlan == null) {
            return;
        }

        if (validationPlan.name() != null
                && !validationPlan.name().isBlank()) {

            XWPFParagraph paragraph =
                    document.createParagraph();

            XWPFRun run =
                    paragraph.createRun();

            run.setText(validationPlan.name());

            run.setBold(true);
        }

        addField(
                document,
                "Description",
                validationPlan.description()
        );

        addField(
                document,
                "Responsible",
                validationPlan.responsible()
        );

        addField(
                document,
                "Timing",
                validationPlan.timing()
        );

        addField(
                document,
                "Target",
                validationPlan.target()
        );

        addField(
                document,
                "Status",
                validationPlan.status()
        );

        document.createParagraph();
    }
}