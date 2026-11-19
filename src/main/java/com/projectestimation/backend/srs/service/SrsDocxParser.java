package com.projectestimation.backend.srs.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

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

@Component
public class SrsDocxParser {

    private static final Logger log = LogManager.getLogger(SrsDocxParser.class);

    private static final Pattern SECTION_NUM_PATTERN = Pattern.compile("^(5\\.\\d+)\\s*(.*)$");

    public SrsDto parse(byte[] docxBytes) throws IOException {
        if (docxBytes == null || docxBytes.length == 0) {
            throw new IllegalArgumentException("DOCX byte array cannot be empty");
        }
        try (InputStream is = new java.io.ByteArrayInputStream(docxBytes)) {
            return parse(is);
        }
    }

    public SrsDto parse(Path docxPath) throws IOException {
        try (InputStream is = Files.newInputStream(docxPath);
             XWPFDocument document = new XWPFDocument(is)) {
            return parseDocument(document);
        }
    }

    public SrsDto parse(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            return parseDocument(document);
        }
    }

    public SrsDto parseDocument(XWPFDocument document) {
        DocumentInformationDto docInfo = null;
        IntroductionDto intro = null;
        OverallProductRequirementsDto overall = null;
        ExternalInterfaceRequirementsDto externalInterfaces = null;
        ProposedSystemEnvironmentsDto environments = null;
        List<FunctionalRequirementDto> functionalRequirements = new ArrayList<>();
        NonFunctionalRequirementsDto nfr = null;
        DataRequirementsDto dataReq = null;
        List<TraceabilityMatrixEntryDto> rtmEntries = new ArrayList<>();

        // Maps to hold section text and tables
        Map<String, String> sectionTexts = new HashMap<>();
        Map<String, List<String>> sectionListTables = new HashMap<>();
        Map<String, XWPFTable> sectionTables = new HashMap<>();

        String currentHeading = "";
        String lastHeading1 = "";
        String lastHeading2 = "";

        List<IBodyElement> bodyElements = document.getBodyElements();

        for (int i = 0; i < bodyElements.size(); i++) {
            IBodyElement element = bodyElements.get(i);

            if (element instanceof XWPFParagraph paragraph) {
                String text = paragraph.getText().trim();
                if (text.isEmpty()) {
                    continue;
                }

                // Detect headings
                if (isMainHeading(text)) {
                    lastHeading1 = normalizeHeading(text);
                    currentHeading = lastHeading1;
                } else if (isSubHeading(text)) {
                    lastHeading2 = normalizeHeading(text);
                    currentHeading = lastHeading2;
                } else {
                    // Accumulate text under currentHeading
                    if (!currentHeading.isEmpty()) {
                        String existing = sectionTexts.getOrDefault(currentHeading, "");
                        sectionTexts.put(currentHeading, existing.isEmpty() ? text : existing + "\n" + text);
                    }
                }
            } else if (element instanceof XWPFTable table) {
                // Table parsing depending on context
                if (docInfo == null && isDocumentInfoTable(table)) {
                    docInfo = parseDocumentInfoTable(table);
                } else if (!currentHeading.isEmpty()) {
                    sectionTables.put(currentHeading, table);
                    List<String> listItems = parseListTable(table);
                    if (!listItems.isEmpty()) {
                        sectionListTables.put(currentHeading, listItems);
                    }
                }
            }
        }

        // Section 1: Introduction
        intro = new IntroductionDto(
                sectionTexts.getOrDefault("1.1 Purpose of this document", ""),
                sectionTexts.getOrDefault("1.2 Background of Development", ""),
                sectionTexts.getOrDefault("1.3 Scope of the System", ""),
                sectionListTables.getOrDefault("1.4 Assumptions and Dependencies", Collections.emptyList())
        );

        // Section 2: Overall Product Requirements
        overall = new OverallProductRequirementsDto(
                sectionTexts.getOrDefault("2.1 Product Perspective", ""),
                sectionListTables.getOrDefault("2.2 Product Components", Collections.emptyList())
        );

        // Section 3: External Interfaces
        List<InterfaceRequirementDto> softwareInterfaces = new ArrayList<>();
        XWPFTable swTable = sectionTables.get("3.1 Software Interfaces");
        if (swTable != null) {
            softwareInterfaces = parseSoftwareInterfaces(swTable);
        }

        externalInterfaces = new ExternalInterfaceRequirementsDto(
                softwareInterfaces,
                sectionListTables.getOrDefault("3.2 Hardware Interfaces", Collections.emptyList()),
                sectionListTables.getOrDefault("3.3 Communications Interfaces", Collections.emptyList()),
                sectionTexts.getOrDefault("3.4 User Interfaces", "")
        );

        // Section 4: Proposed System Environments
        environments = parseEnvironments(bodyElements, sectionTexts);

        // Section 8: Requirements Traceability Matrix (parsed early to assist functional requirement IDs if needed)
        for (int i = 0; i < bodyElements.size(); i++) {
            if (bodyElements.get(i) instanceof XWPFParagraph p) {
                String t = p.getText().trim();
                if (t.contains("8.0 Requirements Traceability Matrix")) {
                    for (int j = i + 1; j < bodyElements.size(); j++) {
                        if (bodyElements.get(j) instanceof XWPFTable tbl) {
                            rtmEntries = parseTraceabilityMatrix(tbl);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        // Section 5: Functional Requirements
        functionalRequirements = parseFunctionalRequirements(bodyElements, rtmEntries);

        // Section 6: Non-Functional Requirements
        nfr = new NonFunctionalRequirementsDto(
                sectionTexts.getOrDefault("6.1 Accuracy", ""),
                sectionTexts.getOrDefault("6.2 Audit Trail", ""),
                sectionTexts.getOrDefault("6.3 Availability", ""),
                sectionTexts.getOrDefault("6.4 Capacity Limits", ""),
                sectionTexts.getOrDefault("6.5 Data Retention", ""),
                sectionTexts.getOrDefault("6.6 Performance", ""),
                sectionTexts.getOrDefault("6.7 Portability", ""),
                sectionTexts.getOrDefault("6.8 Recoverability", ""),
                sectionTexts.getOrDefault("6.9 Reliability", ""),
                sectionTexts.getOrDefault("6.10 Security Requirements", ""),
                sectionTexts.getOrDefault("6.11 Other Compliance Requirements", "")
        );

        // Section 7: Data Requirements
        List<DataRequirementItemDto> inputs = Collections.emptyList();
        List<DataRequirementItemDto> outputs = Collections.emptyList();
        XWPFTable inTable = sectionTables.get("7.2 Input to the System");
        if (inTable != null) {
            inputs = parseDataRequirementItems(inTable);
        }
        XWPFTable outTable = sectionTables.get("7.3 Output from the System");
        if (outTable != null) {
            outputs = parseDataRequirementItems(outTable);
        }

        dataReq = new DataRequirementsDto(
                sectionTexts.getOrDefault("7.1 Data Structures and Relationships", ""),
                inputs,
                outputs,
                sectionTexts.getOrDefault("7.4 Inter-functional Data Definitions", ""),
                sectionTexts.getOrDefault("7.5 Component Cross Reference", "")
        );

        return new SrsDto(
                docInfo,
                intro,
                overall,
                externalInterfaces,
                environments,
                functionalRequirements,
                nfr,
                dataReq,
                rtmEntries
        );
    }

    private boolean isMainHeading(String text) {
        return text.matches("^\\d+\\.0\\s+.*")
                || text.equalsIgnoreCase("Document Information")
                || text.equalsIgnoreCase("Document Release History")
                || text.equalsIgnoreCase("Circulation Details")
                || text.equalsIgnoreCase("List of Amendments");
    }

    private boolean isSubHeading(String text) {
        return text.matches("^\\d+\\.\\d+\\s+.*");
    }

    private String normalizeHeading(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private boolean isDocumentInfoTable(XWPFTable table) {
        if (table.getRows().size() < 4) {
            return false;
        }
        String firstCell = getCellText(table, 0, 0);
        return firstCell.equalsIgnoreCase("Document Title");
    }

    private DocumentInformationDto parseDocumentInfoTable(XWPFTable table) {
        String docTitle = getCellText(table, 0, 1);
        String version = getCellText(table, 0, 3);
        String projectName = getCellText(table, 1, 1);
        String date = getCellText(table, 1, 3);
        String clientName = getCellText(table, 2, 1);
        String preparedBy = getCellText(table, 2, 3);
        String reviewedBy = getCellText(table, 3, 1);
        String approvedBy = getCellText(table, 3, 3);

        return new DocumentInformationDto(
                docTitle,
                projectName,
                clientName,
                version,
                date,
                preparedBy,
                reviewedBy,
                approvedBy
        );
    }

    private List<String> parseListTable(XWPFTable table) {
        List<String> items = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        if (rows.size() <= 1) {
            return items;
        }
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            if (row.getTableCells().size() >= 2) {
                String val = row.getCell(1).getText().trim();
                if (!val.isEmpty()) {
                    items.add(val);
                }
            } else if (!row.getTableCells().isEmpty()) {
                String val = row.getCell(0).getText().trim();
                if (!val.isEmpty()) {
                    items.add(val);
                }
            }
        }
        return items;
    }

    private List<InterfaceRequirementDto> parseSoftwareInterfaces(XWPFTable table) {
        List<InterfaceRequirementDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            if (row.getTableCells().size() >= 2) {
                list.add(new InterfaceRequirementDto(
                        row.getCell(0).getText().trim(),
                        row.getCell(1).getText().trim()
                ));
            }
        }
        return list;
    }

    private List<DataRequirementItemDto> parseDataRequirementItems(XWPFTable table) {
        List<DataRequirementItemDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            if (row.getTableCells().size() >= 2) {
                list.add(new DataRequirementItemDto(
                        row.getCell(0).getText().trim(),
                        row.getCell(1).getText().trim()
                ));
            }
        }
        return list;
    }

    private ProposedSystemEnvironmentsDto parseEnvironments(List<IBodyElement> bodyElements, Map<String, String> sectionTexts) {
        String operationalScenario = sectionTexts.getOrDefault("4.1 Operational Scenario", "");
        EnvironmentDto devEnv = null;
        EnvironmentDto testEnv = null;
        EnvironmentDto deployEnv = null;

        String currentEnv = "";
        String currentSubArea = "";

        Map<String, List<EnvironmentItemDto>> swMap = new HashMap<>();
        Map<String, List<String>> hwMap = new HashMap<>();
        Map<String, List<String>> netMap = new HashMap<>();

        for (IBodyElement el : bodyElements) {
            if (el instanceof XWPFParagraph p) {
                String t = p.getText().trim();
                if (t.contains("4.2 Development Environment")) {
                    currentEnv = "DEV";
                } else if (t.contains("4.3 Testing Environment")) {
                    currentEnv = "TEST";
                } else if (t.contains("4.4 Deployment Environment")) {
                    currentEnv = "DEPLOY";
                } else if (t.startsWith("5.0")) {
                    currentEnv = "";
                } else if (!currentEnv.isEmpty()) {
                    if (t.equalsIgnoreCase("Software Needed")) {
                        currentSubArea = "SW";
                    } else if (t.equalsIgnoreCase("Hardware Needed")) {
                        currentSubArea = "HW";
                    } else if (t.equalsIgnoreCase("Network Needed")) {
                        currentSubArea = "NET";
                    }
                }
            } else if (el instanceof XWPFTable tbl && !currentEnv.isEmpty()) {
                if ("SW".equals(currentSubArea)) {
                    List<EnvironmentItemDto> swItems = new ArrayList<>();
                    List<XWPFTableRow> rows = tbl.getRows();
                    for (int r = 1; r < rows.size(); r++) {
                        if (rows.get(r).getTableCells().size() >= 2) {
                            swItems.add(new EnvironmentItemDto(
                                    rows.get(r).getCell(0).getText().trim(),
                                    rows.get(r).getCell(1).getText().trim()
                            ));
                        }
                    }
                    swMap.put(currentEnv, swItems);
                } else if ("HW".equals(currentSubArea)) {
                    hwMap.put(currentEnv, parseListTable(tbl));
                } else if ("NET".equals(currentSubArea)) {
                    netMap.put(currentEnv, parseListTable(tbl));
                }
            }
        }

        devEnv = new EnvironmentDto(
                swMap.getOrDefault("DEV", Collections.emptyList()),
                hwMap.getOrDefault("DEV", Collections.emptyList()),
                netMap.getOrDefault("DEV", Collections.emptyList())
        );
        testEnv = new EnvironmentDto(
                swMap.getOrDefault("TEST", Collections.emptyList()),
                hwMap.getOrDefault("TEST", Collections.emptyList()),
                netMap.getOrDefault("TEST", Collections.emptyList())
        );
        deployEnv = new EnvironmentDto(
                swMap.getOrDefault("DEPLOY", Collections.emptyList()),
                hwMap.getOrDefault("DEPLOY", Collections.emptyList()),
                netMap.getOrDefault("DEPLOY", Collections.emptyList())
        );

        return new ProposedSystemEnvironmentsDto(operationalScenario, devEnv, testEnv, deployEnv);
    }

    private List<FunctionalRequirementDto> parseFunctionalRequirements(
            List<IBodyElement> bodyElements,
            List<TraceabilityMatrixEntryDto> rtmEntries
    ) {
        List<FunctionalRequirementDto> requirements = new ArrayList<>();
        boolean inSection5 = false;
        String currentSectionNum = "";
        String currentReqName = "";
        int reqIndex = 0;

        for (int i = 0; i < bodyElements.size(); i++) {
            IBodyElement el = bodyElements.get(i);

            if (el instanceof XWPFParagraph p) {
                String t = p.getText().trim();
                if (t.contains("5.0 Detailed Functional Requirements")) {
                    inSection5 = true;
                } else if (inSection5 && t.startsWith("6.0")) {
                    inSection5 = false;
                } else if (inSection5 && isSubHeading(t) && t.startsWith("5.")) {
                    Matcher m = SECTION_NUM_PATTERN.matcher(t);
                    if (m.find()) {
                        currentSectionNum = m.group(1).trim();
                        currentReqName = m.group(2).trim();
                    } else {
                        currentSectionNum = t.split(" ")[0];
                        currentReqName = t.substring(currentSectionNum.length()).trim();
                    }
                }
            } else if (el instanceof XWPFTable tbl && inSection5) {
                if (tbl.getRows().size() >= 8) {
                    Map<String, String> rowMap = new HashMap<>();
                    List<BusinessRuleFieldDto> fields = new ArrayList<>();

                    for (XWPFTableRow row : tbl.getRows()) {
                        if (row.getTableCells().size() >= 2) {
                            String key = row.getCell(0).getText().trim();
                            XWPFTableCell valCell = row.getCell(1);

                            if (key.equalsIgnoreCase("Fields")) {
                                if (!valCell.getTables().isEmpty()) {
                                    fields = parseFieldsTable(valCell.getTables().get(0));
                                }
                            } else {
                                rowMap.put(key.toLowerCase(), valCell.getText().trim());
                            }
                        }
                    }

                    // Extract requirement ID
                    String reqId = rowMap.get("requirement id");
                    if (reqId == null || reqId.isBlank()) {
                        if (reqIndex < rtmEntries.size()) {
                            reqId = rtmEntries.get(reqIndex).requirementId();
                        } else {
                            reqId = "FR-" + String.format("%02d", reqIndex + 1);
                        }
                    }

                    String module = rowMap.getOrDefault("module", "");
                    String desc = rowMap.getOrDefault("description", "");
                    String primaryActor = rowMap.getOrDefault("primary actor", "");
                    String secondaryActor = rowMap.getOrDefault("secondary actor", "");
                    List<String> preconditions = parseNumberedLines(rowMap.getOrDefault("preconditions", ""));
                    List<String> basicFlow = parseNumberedLines(rowMap.getOrDefault("basic flow", ""));
                    List<String> businessRules = parseNumberedLines(rowMap.getOrDefault("business rules", ""));
                    List<String> postconditions = parseNumberedLines(rowMap.getOrDefault("postconditions", ""));
                    String uiDesign = rowMap.getOrDefault("ui design", "");

                    requirements.add(new FunctionalRequirementDto(
                            reqId,
                            currentSectionNum,
                            module,
                            currentReqName,
                            desc,
                            primaryActor,
                            secondaryActor,
                            preconditions,
                            basicFlow,
                            businessRules,
                            fields,
                            postconditions,
                            uiDesign
                    ));
                    reqIndex++;
                }
            }
        }
        return requirements;
    }

    private List<BusinessRuleFieldDto> parseFieldsTable(XWPFTable table) {
        List<BusinessRuleFieldDto> fields = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            if (row.getTableCells().size() >= 4) {
                String fieldName = row.getCell(0).getText().trim();
                String type = row.getCell(1).getText().trim();
                String format = row.getCell(2).getText().trim();
                String mandatoryStr = row.getCell(3).getText().trim();
                Boolean mandatory = mandatoryStr.equalsIgnoreCase("true") || mandatoryStr.equalsIgnoreCase("yes");
                fields.add(new BusinessRuleFieldDto(fieldName, type, format, mandatory));
            }
        }
        return fields;
    }

    private List<TraceabilityMatrixEntryDto> parseTraceabilityMatrix(XWPFTable table) {
        List<TraceabilityMatrixEntryDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            if (row.getTableCells().size() >= 6) {
                list.add(new TraceabilityMatrixEntryDto(
                        row.getCell(0).getText().trim(),
                        row.getCell(1).getText().trim(),
                        row.getCell(2).getText().trim(),
                        row.getCell(3).getText().trim(),
                        row.getCell(4).getText().trim(),
                        row.getCell(5).getText().trim()
                ));
            }
        }
        return list;
    }

    private List<String> parseNumberedLines(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String[] lines = text.split("\\r?\\n");
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                // Strip leading numbering like "1. ", "1) ", or "- "
                trimmed = trimmed.replaceFirst("^(\\d+[.)]|[-*•])\\s*", "");
                result.add(trimmed);
            }
        }
        return result;
    }

    private String getCellText(XWPFTable table, int row, int col) {
        if (table.getRows().size() > row && table.getRow(row).getTableCells().size() > col) {
            return table.getRow(row).getCell(col).getText().trim();
        }
        return "";
    }
}
