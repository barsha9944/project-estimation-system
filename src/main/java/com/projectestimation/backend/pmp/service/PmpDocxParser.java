package com.projectestimation.backend.pmp.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.pmp.dto.ConfigurationManagementPlanDto;
import com.projectestimation.backend.pmp.dto.CustomerInterfaceDto;
import com.projectestimation.backend.pmp.dto.DocumentControlDto;
import com.projectestimation.backend.pmp.dto.EnvironmentDto;
import com.projectestimation.backend.pmp.dto.EstimatedSizeEffortDto;
import com.projectestimation.backend.pmp.dto.InterGroupSupportDto;
import com.projectestimation.backend.pmp.dto.MetricationPlanDto;
import com.projectestimation.backend.pmp.dto.MonitoringControlDto;
import com.projectestimation.backend.pmp.dto.OrganizationResourcesDto;
import com.projectestimation.backend.pmp.dto.PmpDto;
import com.projectestimation.backend.pmp.dto.PmpItemDto;
import com.projectestimation.backend.pmp.dto.ProjectManagementDto;
import com.projectestimation.backend.pmp.dto.ProjectOverviewDto;
import com.projectestimation.backend.pmp.dto.QualityAuditPlanDto;
import com.projectestimation.backend.pmp.dto.QualityControlPlanDto;
import com.projectestimation.backend.pmp.dto.QualityManagementDto;
import com.projectestimation.backend.pmp.dto.RiskManagementDto;
import com.projectestimation.backend.pmp.dto.ScheduleDto;
import com.projectestimation.backend.pmp.dto.ValidationPlanDto;

@Component
public class PmpDocxParser {

    private static final Logger log = LogManager.getLogger(PmpDocxParser.class);
    private final ObjectMapper objectMapper;

    public PmpDocxParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PmpDto parse(byte[] docxBytes, PmpDto existingFallback) throws IOException {
        if (docxBytes == null || docxBytes.length == 0) {
            throw new IllegalArgumentException("DOCX byte array cannot be empty");
        }
        try (InputStream is = new ByteArrayInputStream(docxBytes)) {
            return parse(is, existingFallback);
        }
    }

    public PmpDto parse(Path docxPath, PmpDto existingFallback) throws IOException {
        try (InputStream is = Files.newInputStream(docxPath)) {
            return parse(is, existingFallback);
        }
    }

    public PmpDto parse(InputStream inputStream, PmpDto fallback) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            return parseDocument(document, fallback);
        }
    }

    private PmpDto parseDocument(XWPFDocument document, PmpDto fallback) {
        // Collect all tables by their preceding heading or table title
        Map<String, List<PmpItemDto>> pmpItemTables = new HashMap<>();
        Map<String, List<String>> listTables = new HashMap<>();
        Map<String, String> labelValues = new HashMap<>();

        String lastHeading = "";
        String lastTableTitle = "";

        for (IBodyElement elem : document.getBodyElements()) {
            if (elem instanceof XWPFParagraph para) {
                String text = para.getText().trim();
                if (text.isEmpty()) continue;

                if (text.startsWith("Heading") || text.matches("^(?:\\d+\\.\\d+|[A-Z]\\.).*")) {
                    lastHeading = text;
                } else if (para.getRuns().stream().anyMatch(r -> r.isBold() && r.getFontSize() <= 11)) {
                    lastTableTitle = text;
                }
            } else if (elem instanceof XWPFTable table) {
                String key = !lastTableTitle.isEmpty() ? lastTableTitle : lastHeading;
                if (is6ColumnPmpItemTable(table)) {
                    pmpItemTables.put(normalizeKey(key), parsePmpItemTable(table));
                } else if (is2ColumnListTable(table)) {
                    listTables.put(normalizeKey(key), parse2ColumnListTable(table));
                } else if (isLabelValueTable(table)) {
                    parseLabelValueTable(table, labelValues);
                }
                lastTableTitle = "";
            }
        }

        // Now merge with fallback (or create new if fallback is null)
        PmpDto base = fallback != null ? fallback : createEmptyPmpDto();

        // 1. Document Control
        List<PmpItemDto> releaseHistory = pmpItemTables.getOrDefault("document release history",
                base.documentControl() != null ? base.documentControl().releaseHistory() : Collections.emptyList());
        List<PmpItemDto> circulation = pmpItemTables.getOrDefault("circulation details",
                base.documentControl() != null ? base.documentControl().circulationDetails() : Collections.emptyList());
        List<PmpItemDto> amendments = pmpItemTables.getOrDefault("list of amendments",
                base.documentControl() != null ? base.documentControl().amendments() : Collections.emptyList());
        DocumentControlDto docControl = new DocumentControlDto(releaseHistory, circulation, amendments);

        // 2. Project Overview
        ProjectOverviewDto prevOverview = base.projectOverview();
        String projName = labelValues.getOrDefault("project name", prevOverview != null ? prevOverview.projectName() : "");
        String projDesc = labelValues.getOrDefault("project description", prevOverview != null ? prevOverview.projectDescription() : "");
        String projScope = labelValues.getOrDefault("project scope", prevOverview != null ? prevOverview.projectScope() : "");
        List<String> objectives = listTables.getOrDefault("objectives", prevOverview != null ? prevOverview.objectives() : Collections.emptyList());
        List<String> deliverables = listTables.getOrDefault("deliverables", prevOverview != null ? prevOverview.deliverables() : Collections.emptyList());
        List<String> assumptions = listTables.getOrDefault("assumptions", prevOverview != null ? prevOverview.assumptions() : Collections.emptyList());
        List<String> constraints = listTables.getOrDefault("constraints", prevOverview != null ? prevOverview.constraints() : Collections.emptyList());
        List<String> compliance = listTables.getOrDefault("compliance requirements", prevOverview != null ? prevOverview.complianceRequirements() : Collections.emptyList());

        ProjectOverviewDto projectOverview = new ProjectOverviewDto(
                projName,
                projDesc,
                prevOverview != null ? prevOverview.introduction() : "",
                projScope,
                objectives,
                deliverables,
                assumptions,
                constraints,
                prevOverview != null ? prevOverview.acceptanceCriteria() : Collections.emptyList(),
                prevOverview != null ? prevOverview.customerInterface() : null,
                compliance,
                prevOverview != null ? prevOverview.businessObjectives() : Collections.emptyList(),
                prevOverview != null ? prevOverview.detailedDeliverables() : Collections.emptyList(),
                prevOverview != null ? prevOverview.milestones() : Collections.emptyList()
        );

        // 3. Risk Management
        List<PmpItemDto> risks = pmpItemTables.getOrDefault("risks",
                base.riskManagement() != null ? base.riskManagement().risks() : Collections.emptyList());
        RiskManagementDto riskManagement = new RiskManagementDto(
                risks,
                base.riskManagement() != null ? base.riskManagement().mitigationStrategies() : Collections.emptyList(),
                base.riskManagement() != null ? base.riskManagement().contingencyPlans() : Collections.emptyList(),
                base.riskManagement() != null ? base.riskManagement().dependencies() : Collections.emptyList(),
                base.riskManagement() != null ? base.riskManagement().assumptions() : Collections.emptyList()
        );

        // 4. Schedule
        List<PmpItemDto> scheduleItems = pmpItemTables.getOrDefault("schedule",
                base.schedule() != null ? base.schedule().scheduleItems() : Collections.emptyList());
        ScheduleDto schedule = new ScheduleDto(scheduleItems);

        // 5. Validation Plan
        List<PmpItemDto> valActivities = pmpItemTables.getOrDefault("validation plan",
                base.validationPlan() != null ? base.validationPlan().activities() : Collections.emptyList());
        ValidationPlanDto validationPlan = new ValidationPlanDto(
                base.validationPlan() != null ? base.validationPlan().name() : "",
                base.validationPlan() != null ? base.validationPlan().description() : "",
                base.validationPlan() != null ? base.validationPlan().responsible() : "",
                base.validationPlan() != null ? base.validationPlan().timing() : "",
                base.validationPlan() != null ? base.validationPlan().target() : "",
                base.validationPlan() != null ? base.validationPlan().status() : "",
                valActivities
        );

        return new PmpDto(
                projectOverview,
                base.projectManagement(),
                base.qualityManagement(),
                riskManagement,
                validationPlan,
                docControl,
                base.environment(),
                base.organizationResources(),
                base.monitoringControl(),
                base.interGroupSupport(),
                base.estimatedSizeEffort(),
                schedule,
                base.metricationPlan(),
                base.qualityControlPlan(),
                base.qualityAuditPlan(),
                base.configurationManagementPlan()
        );
    }

    private String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase()
                .replaceAll("^[0-9a-z.]+\\s*", "")
                .trim();
    }

    private boolean is6ColumnPmpItemTable(XWPFTable table) {
        if (table.getRows().isEmpty()) return false;
        List<XWPFTableCell> cells = table.getRow(0).getTableCells();
        if (cells.size() == 6) {
            String c0 = cells.get(0).getText().trim().toLowerCase();
            String c1 = cells.get(1).getText().trim().toLowerCase();
            return c0.contains("name") || c1.contains("description");
        }
        return false;
    }

    private List<PmpItemDto> parsePmpItemTable(XWPFTable table) {
        List<PmpItemDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 6) {
                list.add(new PmpItemDto(
                        cells.get(0).getText().trim(),
                        cells.get(1).getText().trim(),
                        cells.get(2).getText().trim(),
                        cells.get(3).getText().trim(),
                        cells.get(4).getText().trim(),
                        cells.get(5).getText().trim()
                ));
            }
        }
        return list;
    }

    private boolean is2ColumnListTable(XWPFTable table) {
        if (table.getRows().isEmpty()) return false;
        List<XWPFTableCell> cells = table.getRow(0).getTableCells();
        if (cells.size() == 2) {
            String c0 = cells.get(0).getText().trim().toLowerCase();
            return c0.contains("sl") || c0.contains("no");
        }
        return false;
    }

    private List<String> parse2ColumnListTable(XWPFTable table) {
        List<String> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 2) {
                String val = cells.get(1).getText().trim();
                if (!val.isEmpty()) list.add(val);
            }
        }
        return list;
    }

    private boolean isLabelValueTable(XWPFTable table) {
        if (table.getRows().isEmpty()) return false;
        List<XWPFTableCell> cells = table.getRow(0).getTableCells();
        if (cells.size() == 2) {
            String c0 = cells.get(0).getText().trim().toLowerCase();
            return !c0.contains("sl") && !c0.contains("no");
        }
        return false;
    }

    private void parseLabelValueTable(XWPFTable table, Map<String, String> out) {
        for (XWPFTableRow row : table.getRows()) {
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() >= 2) {
                String label = cells.get(0).getText().trim().toLowerCase();
                String val = cells.get(1).getText().trim();
                out.put(label, val);
            }
        }
    }

    private PmpDto createEmptyPmpDto() {
        return new PmpDto(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );
    }
}
