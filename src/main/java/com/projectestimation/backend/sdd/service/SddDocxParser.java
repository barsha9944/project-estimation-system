package com.projectestimation.backend.sdd.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

@Component
public class SddDocxParser {

    private static final Logger log = LogManager.getLogger(SddDocxParser.class);

    private static final Pattern COMPONENT_HEADING_PATTERN =
            Pattern.compile("^(?:3\\.\\d+|[A-Z0-9.-]+)\\s+Specific Description of Component\\s+([A-Za-z0-9.-]+)[:\\s]*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBCOMPONENT_HEADING_PATTERN =
            Pattern.compile("^(3\\.\\d+\\.\\d+)\\s+(.*)$");

    public SddDto parse(byte[] docxBytes) throws IOException {
        if (docxBytes == null || docxBytes.length == 0) {
            throw new IllegalArgumentException("DOCX byte array cannot be empty");
        }
        try (InputStream is = new ByteArrayInputStream(docxBytes)) {
            return parse(is);
        }
    }

    public SddDto parse(Path docxPath) throws IOException {
        try (InputStream is = Files.newInputStream(docxPath)) {
            return parse(is);
        }
    }

    public SddDto parse(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            return parseDocument(document);
        }
    }

    private SddDto parseDocument(XWPFDocument document) {
        DocumentReleaseHistoryDto releaseHistory = null;
        CirculationDetailsDto circulationDetails = null;
        List<AmendmentDto> amendments = new ArrayList<>();

        // Introduction
        StringBuilder purposeOfDocument = new StringBuilder();
        List<ScopeTaskDto> scopeTasks = new ArrayList<>();
        List<DesignAlternativeDto> designAlternatives = new ArrayList<>();
        StringBuilder designAlternativesDecision = new StringBuilder();
        StringBuilder makeBuyOrReuseDecisions = new StringBuilder();

        // System Perspective
        StringBuilder systemContext = new StringBuilder();
        EnvironmentDto devTestDepEnv = null;
        StringBuilder appComponentsOverview = new StringBuilder();
        StringBuilder generalFeatures = new StringBuilder();

        // Environment sub-tables & text
        List<EnvironmentItemDto> devSoftware = new ArrayList<>();
        StringBuilder devHardware = new StringBuilder();
        StringBuilder devNetwork = new StringBuilder();
        List<EnvironmentItemDto> testSoftware = new ArrayList<>();
        StringBuilder testHardware = new StringBuilder();
        StringBuilder testNetwork = new StringBuilder();
        List<EnvironmentItemDto> depSoftware = new ArrayList<>();
        StringBuilder depHardware = new StringBuilder();
        StringBuilder depNetwork = new StringBuilder();

        // 3.0 Application Components
        List<ApplicationComponentDto> applicationComponents = new ArrayList<>();
        ApplicationComponentDto currentComponent = null;
        List<ApplicationComponentDetailsDto> currentDetailsList = new ArrayList<>();

        // For current subcomponent
        String currentSubCompNumber = null;
        String currentSubCompName = null;
        String currentDesc = null;
        String currentPrimaryActor = null;
        String currentSecondaryActor = null;
        String currentPrecondition = null;
        List<String> currentBasicFlow = new ArrayList<>();
        List<String> currentBusinessRules = new ArrayList<>();
        String currentPostCondition = null;
        String currentUiDesign = null;
        List<DesignDetailDto> currentDesignDetails = new ArrayList<>();
        String currentSequenceDiagram = null;
        String activeListType = null; // "flow" or "rules"

        // 4.0 Database Design
        StringBuilder databaseDesignDesc = new StringBuilder();

        // 5.0 Special Considerations
        StringBuilder designRules = new StringBuilder();
        StringBuilder programmingRules = new StringBuilder();
        StringBuilder errorHandling = new StringBuilder();
        StringBuilder specialSecurity = new StringBuilder();
        StringBuilder specialRecovery = new StringBuilder();
        StringBuilder auditTracing = new StringBuilder();
        StringBuilder systemImpl = new StringBuilder();
        StringBuilder dbAdmin = new StringBuilder();

        // 6.0 System Integration Strategy
        StringBuilder integrationStrategy = new StringBuilder();
        StringBuilder integrationSequence = new StringBuilder();

        // 7.0 Traceability Matrix
        List<SddTraceabilityMatrixEntryDto> traceabilityMatrix = new ArrayList<>();

        String currentSection = "";

        List<IBodyElement> elements = document.getBodyElements();
        for (int i = 0; i < elements.size(); i++) {
            IBodyElement elem = elements.get(i);

            if (elem instanceof XWPFParagraph para) {
                String text = para.getText().trim();
                if (text.isEmpty()) continue;

                // Section detection
                String upper = text.toUpperCase();
                if (upper.contains("DOCUMENT RELEASE HISTORY")) {
                    currentSection = "RELEASE_HISTORY";
                    continue;
                } else if (upper.contains("CIRCULATION DETAILS")) {
                    currentSection = "CIRCULATION_DETAILS";
                    continue;
                } else if (upper.contains("LIST OF AMENDMENTS")) {
                    currentSection = "AMENDMENTS";
                    continue;
                } else if (upper.startsWith("1.0 ") || upper.equals("1.0 INTRODUCTION")) {
                    currentSection = "1.0";
                    continue;
                } else if (upper.startsWith("1.1 ") || upper.contains("PURPOSE OF THIS DOCUMENT")) {
                    currentSection = "1.1";
                    continue;
                } else if (upper.startsWith("1.2 ") || upper.contains("SCOPE OF THE PROPOSED SYSTEM")) {
                    currentSection = "1.2";
                    continue;
                } else if (upper.startsWith("1.3 ") || upper.contains("DESIGN ALTERNATIVES CONSIDERED")) {
                    currentSection = "1.3";
                    continue;
                } else if (upper.startsWith("1.4 ") || upper.contains("MAKE, BUY OR REUSE DECISIONS")) {
                    currentSection = "1.4";
                    continue;
                } else if (upper.startsWith("2.0 ") || upper.equals("2.0 THE SYSTEM PERSPECTIVE")) {
                    currentSection = "2.0";
                    continue;
                } else if (upper.startsWith("2.1 ") || upper.contains("THE SYSTEM CONTEXT")) {
                    currentSection = "2.1";
                    continue;
                } else if (upper.startsWith("2.2 ") || upper.contains("DEVELOPMENT, TESTING, AND DEPLOYMENT ENVIRONMENT")) {
                    currentSection = "2.2";
                    continue;
                } else if (upper.contains("2.2.1.1 SOFTWARE NEEDED")) {
                    currentSection = "2.2.1.1";
                    continue;
                } else if (upper.contains("2.2.1.2 HARDWARE NEEDED")) {
                    currentSection = "2.2.1.2";
                    continue;
                } else if (upper.contains("2.2.1.3 NETWORK NEEDED")) {
                    currentSection = "2.2.1.3";
                    continue;
                } else if (upper.contains("2.2.2.1 SOFTWARE NEEDED")) {
                    currentSection = "2.2.2.1";
                    continue;
                } else if (upper.contains("2.2.2.2 HARDWARE NEEDED")) {
                    currentSection = "2.2.2.2";
                    continue;
                } else if (upper.contains("2.2.2.3 NETWORK NEEDED")) {
                    currentSection = "2.2.2.3";
                    continue;
                } else if (upper.contains("2.2.3.1 SOFTWARE NEEDED")) {
                    currentSection = "2.2.3.1";
                    continue;
                } else if (upper.contains("2.2.3.2 HARDWARE NEEDED")) {
                    currentSection = "2.2.3.2";
                    continue;
                } else if (upper.contains("2.2.3.3 NETWORK NEEDED")) {
                    currentSection = "2.2.3.3";
                    continue;
                } else if (upper.startsWith("2.3 ") || upper.contains("THE APPLICATION COMPONENTS")) {
                    currentSection = "2.3";
                    continue;
                } else if (upper.startsWith("2.4 ") || upper.contains("GENERAL FEATURES OF THE PROPOSED APPLICATION")) {
                    currentSection = "2.4";
                    continue;
                } else if (upper.startsWith("3.0 ") || upper.contains("SPECIFIC DESCRIPTION OF THE APPLICATION COMPONENTS")) {
                    currentSection = "3.0";
                    continue;
                } else if (upper.startsWith("4.0 ") || upper.contains("PROPOSED DATABASE DESIGN")) {
                    // Flush any pending subcomponent
                    if (currentSubCompNumber != null) {
                        currentDetailsList.add(new ApplicationComponentDetailsDto(
                                currentSubCompNumber, currentSubCompName, currentDesc,
                                currentPrimaryActor, currentSecondaryActor, currentPrecondition,
                                currentBasicFlow, currentBusinessRules, currentPostCondition,
                                currentUiDesign, currentDesignDetails, currentSequenceDiagram
                        ));
                        currentSubCompNumber = null;
                    }
                    if (currentComponent != null) {
                        applicationComponents.add(new ApplicationComponentDto(
                                currentComponent.componentNumber(),
                                currentComponent.componentName(),
                                currentComponent.description(),
                                new ArrayList<>(currentDetailsList)
                        ));
                        currentComponent = null;
                        currentDetailsList.clear();
                    }
                    currentSection = "4.0";
                    continue;
                } else if (upper.startsWith("5.0 ") || upper.contains("SPECIAL CONSIDERATIONS")) {
                    currentSection = "5.0";
                    continue;
                } else if (upper.startsWith("5.1 ") || upper.contains("DESIGN RULES/CRITERIA")) {
                    currentSection = "5.1";
                    continue;
                } else if (upper.startsWith("5.2 ") || upper.contains("PROGRAMMING RULES")) {
                    currentSection = "5.2";
                    continue;
                } else if (upper.startsWith("5.3 ") || upper.contains("ERROR HANDLING PROCEDURES")) {
                    currentSection = "5.3";
                    continue;
                } else if (upper.startsWith("5.4 ") || upper.contains("SPECIAL SECURITY PROVISIONS")) {
                    currentSection = "5.4";
                    continue;
                } else if (upper.startsWith("5.5 ") || upper.contains("SPECIAL RECOVERY PROCEDURE")) {
                    currentSection = "5.5";
                    continue;
                } else if (upper.startsWith("5.6 ") || upper.contains("AUDIT TRACING FACILITY")) {
                    currentSection = "5.6";
                    continue;
                } else if (upper.startsWith("5.7 ") || upper.contains("SYSTEM IMPLEMENTATION PROCEDURES")) {
                    currentSection = "5.7";
                    continue;
                } else if (upper.startsWith("5.8 ") || upper.contains("DATABASE ADMINISTRATION PROCEDURE")) {
                    currentSection = "5.8";
                    continue;
                } else if (upper.startsWith("6.0 ") || upper.contains("SYSTEM INTEGRATION STRATEGY")) {
                    currentSection = "6.0";
                    continue;
                } else if (upper.startsWith("6.1 ") || upper.contains("DESIRED STRATEGY FOR INTEGRATION")) {
                    currentSection = "6.1";
                    continue;
                } else if (upper.startsWith("6.2 ") || upper.contains("DESIRED SEQUENCE & CRITERIA")) {
                    currentSection = "6.2";
                    continue;
                } else if (upper.startsWith("7.0 ") || upper.contains("REQUIREMENTS TRACEABILITY MATRIX")) {
                    currentSection = "7.0";
                    continue;
                }

                // Check for 3.0 component heading or subcomponent heading
                if (currentSection.startsWith("3.") || currentSection.equals("3.0")) {
                    Matcher compMatcher = COMPONENT_HEADING_PATTERN.matcher(text);
                    if (compMatcher.matches()) {
                        // Flush previous subcomponent
                        if (currentSubCompNumber != null) {
                            currentDetailsList.add(new ApplicationComponentDetailsDto(
                                    currentSubCompNumber, currentSubCompName, currentDesc,
                                    currentPrimaryActor, currentSecondaryActor, currentPrecondition,
                                    currentBasicFlow, currentBusinessRules, currentPostCondition,
                                    currentUiDesign, currentDesignDetails, currentSequenceDiagram
                            ));
                            currentSubCompNumber = null;
                        }
                        // Flush previous component
                        if (currentComponent != null) {
                            applicationComponents.add(new ApplicationComponentDto(
                                    currentComponent.componentNumber(),
                                    currentComponent.componentName(),
                                    currentComponent.description(),
                                    new ArrayList<>(currentDetailsList)
                            ));
                            currentDetailsList.clear();
                        }
                        String num = compMatcher.group(1).trim();
                        String name = compMatcher.group(2).trim();
                        currentComponent = new ApplicationComponentDto(num, name, "", new ArrayList<>());
                        currentSection = "3.COMPONENT";
                        continue;
                    }

                    Matcher subMatcher = SUBCOMPONENT_HEADING_PATTERN.matcher(text);
                    if (subMatcher.matches()) {
                        // Flush previous subcomponent
                        if (currentSubCompNumber != null) {
                            currentDetailsList.add(new ApplicationComponentDetailsDto(
                                    currentSubCompNumber, currentSubCompName, currentDesc,
                                    currentPrimaryActor, currentSecondaryActor, currentPrecondition,
                                    currentBasicFlow, currentBusinessRules, currentPostCondition,
                                    currentUiDesign, currentDesignDetails, currentSequenceDiagram
                            ));
                        }
                        currentSubCompNumber = subMatcher.group(1).trim();
                        currentSubCompName = subMatcher.group(2).trim();
                        currentDesc = null;
                        currentPrimaryActor = null;
                        currentSecondaryActor = null;
                        currentPrecondition = null;
                        currentBasicFlow = new ArrayList<>();
                        currentBusinessRules = new ArrayList<>();
                        currentPostCondition = null;
                        currentUiDesign = null;
                        currentDesignDetails = new ArrayList<>();
                        currentSequenceDiagram = null;
                        activeListType = null;
                        currentSection = "3.SUBCOMPONENT";
                        continue;
                    }

                    // Inside subcomponent content lines
                    if (currentSection.equals("3.SUBCOMPONENT")) {
                        if (text.startsWith("Description:")) {
                            currentDesc = cleanSafePrefix(text, "Description:");
                            activeListType = null;
                        } else if (text.startsWith("Primary Actor:")) {
                            currentPrimaryActor = cleanSafePrefix(text, "Primary Actor:");
                            activeListType = null;
                        } else if (text.startsWith("Secondary Actor:")) {
                            currentSecondaryActor = cleanSafePrefix(text, "Secondary Actor:");
                            activeListType = null;
                        } else if (text.startsWith("Precondition:")) {
                            currentPrecondition = cleanSafePrefix(text, "Precondition:");
                            activeListType = null;
                        } else if (text.equalsIgnoreCase("Basic flow:") || text.startsWith("Basic flow:")) {
                            activeListType = "flow";
                        } else if (text.equalsIgnoreCase("Business Rules:") || text.startsWith("Business Rules:")) {
                            activeListType = "rules";
                        } else if (text.startsWith("Post condition:")) {
                            currentPostCondition = cleanSafePrefix(text, "Post condition:");
                            activeListType = null;
                        } else if (text.startsWith("UI Design:")) {
                            activeListType = null;
                        } else if (text.startsWith("• ")) {
                            String item = text.substring(2).trim();
                            if ("flow".equals(activeListType)) {
                                currentBasicFlow.add(item);
                            } else if ("rules".equals(activeListType)) {
                                currentBusinessRules.add(item);
                            }
                        } else if (text.matches("^\\d+\\.\\s+.*")) {
                            String item = text.replaceFirst("^\\d+\\.\\s+", "").trim();
                            if ("flow".equals(activeListType)) {
                                currentBasicFlow.add(item);
                            } else if ("rules".equals(activeListType)) {
                                currentBusinessRules.add(item);
                            }
                        }
                        continue;
                    }
                }

                // Section body text accumulation
                switch (currentSection) {
                    case "1.1" -> appendLine(purposeOfDocument, text);
                    case "1.3" -> appendLine(designAlternativesDecision, text);
                    case "1.4" -> appendLine(makeBuyOrReuseDecisions, text);
                    case "2.1" -> appendLine(systemContext, text);
                    case "2.2.1.2" -> appendLine(devHardware, text);
                    case "2.2.1.3" -> appendLine(devNetwork, text);
                    case "2.2.2.2" -> appendLine(testHardware, text);
                    case "2.2.2.3" -> appendLine(testNetwork, text);
                    case "2.2.3.2" -> appendLine(depHardware, text);
                    case "2.2.3.3" -> appendLine(depNetwork, text);
                    case "2.3" -> appendLine(appComponentsOverview, text);
                    case "2.4" -> appendLine(generalFeatures, text);
                    case "4.0" -> appendLine(databaseDesignDesc, text);
                    case "5.1" -> appendLine(designRules, text);
                    case "5.2" -> appendLine(programmingRules, text);
                    case "5.3" -> appendLine(errorHandling, text);
                    case "5.4" -> appendLine(specialSecurity, text);
                    case "5.5" -> appendLine(specialRecovery, text);
                    case "5.6" -> appendLine(auditTracing, text);
                    case "5.7" -> appendLine(systemImpl, text);
                    case "5.8" -> appendLine(dbAdmin, text);
                    case "6.1" -> appendLine(integrationStrategy, text);
                    case "6.2" -> appendLine(integrationSequence, text);
                }

            } else if (elem instanceof XWPFTable table) {
                switch (currentSection) {
                    case "RELEASE_HISTORY" -> releaseHistory = parseReleaseHistoryTable(table);
                    case "CIRCULATION_DETAILS" -> circulationDetails = parseCirculationDetailsTable(table);
                    case "AMENDMENTS" -> amendments = parseAmendmentsTable(table);
                    case "1.2" -> scopeTasks = parseScopeTasksTable(table);
                    case "1.3" -> designAlternatives = parseDesignAlternativesTable(table);
                    case "2.2.1.1" -> devSoftware = parseEnvironmentItemsTable(table);
                    case "2.2.1.2" -> appendLine(devHardware, extractTableText(table));
                    case "2.2.1.3" -> appendLine(devNetwork, extractTableText(table));
                    case "2.2.2.1" -> testSoftware = parseEnvironmentItemsTable(table);
                    case "2.2.2.2" -> appendLine(testHardware, extractTableText(table));
                    case "2.2.2.3" -> appendLine(testNetwork, extractTableText(table));
                    case "2.2.3.1" -> depSoftware = parseEnvironmentItemsTable(table);
                    case "2.2.3.2" -> appendLine(depHardware, extractTableText(table));
                    case "2.2.3.3" -> appendLine(depNetwork, extractTableText(table));
                    case "7.0" -> traceabilityMatrix = parseTraceabilityMatrixTable(table);
                    default -> {
                        if (currentSection.equals("3.SUBCOMPONENT")) {
                            // Check table headers: Design Details vs UI Design
                            if (isDesignDetailsTable(table)) {
                                currentDesignDetails = parseDesignDetailsTable(table);
                            } else {
                                // Likely UI Design table
                                String ui = extractTableText(table);
                                if (!ui.isBlank() && !ui.equalsIgnoreCase("Not specified")) {
                                    currentUiDesign = ui;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Final flush of last subcomponent and component
        if (currentSubCompNumber != null) {
            currentDetailsList.add(new ApplicationComponentDetailsDto(
                    currentSubCompNumber, currentSubCompName, currentDesc,
                    currentPrimaryActor, currentSecondaryActor, currentPrecondition,
                    currentBasicFlow, currentBusinessRules, currentPostCondition,
                    currentUiDesign, currentDesignDetails, currentSequenceDiagram
            ));
        }
        if (currentComponent != null) {
            applicationComponents.add(new ApplicationComponentDto(
                    currentComponent.componentNumber(),
                    currentComponent.componentName(),
                    currentComponent.description(),
                    new ArrayList<>(currentDetailsList)
            ));
        }

        // Build EnvironmentDto
        EnvironmentDetailsDto devEnv = new EnvironmentDetailsDto(devSoftware, devHardware.toString().trim(), devNetwork.toString().trim());
        EnvironmentDetailsDto testEnv = new EnvironmentDetailsDto(testSoftware, testHardware.toString().trim(), testNetwork.toString().trim());
        EnvironmentDetailsDto depEnv = new EnvironmentDetailsDto(depSoftware, depHardware.toString().trim(), depNetwork.toString().trim());
        devTestDepEnv = new EnvironmentDto(devEnv, testEnv, depEnv);

        // Build IntroductionSddDto
        IntroductionSddDto intro = new IntroductionSddDto(
                purposeOfDocument.toString().trim(),
                scopeTasks,
                designAlternatives,
                designAlternativesDecision.toString().trim(),
                makeBuyOrReuseDecisions.toString().trim()
        );

        // Build SystemPerspectiveDto
        SystemPerspectiveDto sysPerspective = new SystemPerspectiveDto(
                systemContext.toString().trim(),
                devTestDepEnv,
                appComponentsOverview.toString().trim(),
                generalFeatures.toString().trim()
        );

        // Build SpecialConsiderationsDto
        SpecialConsiderationsDto specialCons = new SpecialConsiderationsDto(
                designRules.toString().trim(),
                programmingRules.toString().trim(),
                errorHandling.toString().trim(),
                specialSecurity.toString().trim(),
                specialRecovery.toString().trim(),
                auditTracing.toString().trim(),
                systemImpl.toString().trim(),
                dbAdmin.toString().trim()
        );

        // Build SystemIntegrationStrategyDto
        SystemIntegrationStrategyDto integrationStrat = new SystemIntegrationStrategyDto(
                integrationStrategy.toString().trim(),
                integrationSequence.toString().trim(),
                ""
        );

        // Build DatabaseDesignDto
        DatabaseDesignDto dbDesign = new DatabaseDesignDto(databaseDesignDesc.toString().trim());

        return new SddDto(
                releaseHistory != null ? releaseHistory : new DocumentReleaseHistoryDto(Collections.emptyList()),
                circulationDetails != null ? circulationDetails : new CirculationDetailsDto("", Collections.emptyList()),
                amendments,
                intro,
                sysPerspective,
                applicationComponents,
                "", // classDiagram
                dbDesign,
                specialCons,
                integrationStrat,
                traceabilityMatrix
        );
    }

    private String cleanSafePrefix(String text, String prefix) {
        String res = text.substring(prefix.length()).trim();
        return res.equalsIgnoreCase("Not specified") ? "" : res;
    }

    private void appendLine(StringBuilder sb, String text) {
        if (!sb.isEmpty()) sb.append("\n");
        sb.append(text);
    }

    private DocumentReleaseHistoryDto parseReleaseHistoryTable(XWPFTable table) {
        List<DocumentReleaseHistoryEntryDto> entries = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 6) {
                String slNoStr = cells.get(0).getText().trim();
                Integer slNo = null;
                try { if (!slNoStr.isEmpty()) slNo = Integer.parseInt(slNoStr); } catch (NumberFormatException ignored) {}
                String version = cells.get(1).getText().trim();
                String date = cells.get(2).getText().trim();
                String prepBy = cells.get(3).getText().trim();
                String approvedBy = cells.get(4).getText().trim();
                String reasons = cells.get(5).getText().trim();
                if (!version.isEmpty() || !date.isEmpty() || slNo != null) {
                    entries.add(new DocumentReleaseHistoryEntryDto(slNo, version, date, prepBy, approvedBy, reasons));
                }
            }
        }
        return new DocumentReleaseHistoryDto(entries);
    }

    private CirculationDetailsDto parseCirculationDetailsTable(XWPFTable table) {
        List<CirculationDetailsEntryDto> entries = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 3) {
                String copyNoStr = cells.get(0).getText().trim();
                Integer copyNo = null;
                try { if (!copyNoStr.isEmpty()) copyNo = Integer.parseInt(copyNoStr); } catch (NumberFormatException ignored) {}
                String designation = cells.get(1).getText().trim();
                String location = cells.get(2).getText().trim();
                if (copyNo != null || !designation.isEmpty()) {
                    entries.add(new CirculationDetailsEntryDto(copyNo, designation, location));
                }
            }
        }
        return new CirculationDetailsDto("", entries);
    }

    private List<AmendmentDto> parseAmendmentsTable(XWPFTable table) {
        List<AmendmentDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 5) {
                String slNoStr = cells.get(0).getText().trim();
                Integer slNo = null;
                try { if (!slNoStr.isEmpty()) slNo = Integer.parseInt(slNoStr); } catch (NumberFormatException ignored) {}
                String secNo = cells.get(1).getText().trim();
                String desc = cells.get(2).getText().trim();
                String approvedBy = cells.get(3).getText().trim();
                String crNo = cells.get(4).getText().trim();
                if (slNo != null || !desc.isEmpty()) {
                    list.add(new AmendmentDto(slNo, secNo, desc, approvedBy, crNo));
                }
            }
        }
        return list;
    }

    private List<ScopeTaskDto> parseScopeTasksTable(XWPFTable table) {
        List<ScopeTaskDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 3) {
                String taskNoStr = cells.get(0).getText().trim();
                Integer taskNo = null;
                try { if (!taskNoStr.isEmpty()) taskNo = Integer.parseInt(taskNoStr); } catch (NumberFormatException ignored) {}
                String taskName = cells.get(1).getText().trim();
                String taskDetails = cells.get(2).getText().trim();
                if (taskNo != null || !taskName.isEmpty()) {
                    list.add(new ScopeTaskDto(taskNo, taskName, taskDetails));
                }
            }
        }
        return list;
    }

    private List<DesignAlternativeDto> parseDesignAlternativesTable(XWPFTable table) {
        List<DesignAlternativeDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 2) {
                String alt = cells.get(0).getText().trim();
                String desc = cells.get(1).getText().trim();
                if (!alt.isEmpty() || !desc.isEmpty()) {
                    list.add(new DesignAlternativeDto(alt, desc));
                }
            }
        }
        return list;
    }

    private List<EnvironmentItemDto> parseEnvironmentItemsTable(XWPFTable table) {
        List<EnvironmentItemDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 2) {
                String tech = cells.get(0).getText().trim();
                String prod = cells.get(1).getText().trim();
                if (!tech.isEmpty() || !prod.isEmpty()) {
                    list.add(new EnvironmentItemDto(tech, prod));
                }
            }
        }
        return list;
    }

    private boolean isDesignDetailsTable(XWPFTable table) {
        if (table.getRows().isEmpty()) return false;
        List<XWPFTableCell> headerCells = table.getRow(0).getTableCells();
        if (headerCells.size() >= 4) {
            String first = headerCells.get(0).getText().trim().toLowerCase();
            String second = headerCells.get(1).getText().trim().toLowerCase();
            return first.contains("sl") || second.contains("request") || second.contains("jsp");
        }
        return false;
    }

    private List<DesignDetailDto> parseDesignDetailsTable(XWPFTable table) {
        List<DesignDetailDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 6) {
                String slNoStr = cells.get(0).getText().trim();
                Integer slNo = null;
                try { if (!slNoStr.isEmpty()) slNo = Integer.parseInt(slNoStr); } catch (NumberFormatException ignored) {}
                String reqJsp = cells.get(1).getText().trim();
                String resJsp = cells.get(2).getText().trim();
                String controller = cells.get(3).getText().trim();
                String dao = cells.get(4).getText().trim();
                String entityName = cells.get(5).getText().trim();
                list.add(new DesignDetailDto(
                        slNo,
                        cleanSafe(reqJsp),
                        cleanSafe(resJsp),
                        cleanSafe(controller),
                        cleanSafe(dao),
                        cleanSafe(entityName)
                ));
            }
        }
        return list;
    }

    private String cleanSafe(String val) {
        return (val == null || val.equalsIgnoreCase("Not specified")) ? "" : val;
    }

    private List<SddTraceabilityMatrixEntryDto> parseTraceabilityMatrixTable(XWPFTable table) {
        List<SddTraceabilityMatrixEntryDto> list = new ArrayList<>();
        List<XWPFTableRow> rows = table.getRows();
        for (int r = 1; r < rows.size(); r++) {
            List<XWPFTableCell> cells = rows.get(r).getTableCells();
            if (cells.size() >= 3) {
                String reqId = cells.get(0).getText().trim();
                String reqDesc = cells.get(1).getText().trim();
                String comp = cells.get(2).getText().trim();
                if (!reqId.isEmpty() || !comp.isEmpty()) {
                    list.add(new SddTraceabilityMatrixEntryDto(cleanSafe(reqId), cleanSafe(reqDesc), cleanSafe(comp)));
                }
            }
        }
        return list;
    }

    private String extractTableText(XWPFTable table) {
        StringBuilder sb = new StringBuilder();
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = cell.getText().trim();
                if (!text.isEmpty()) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append(text);
                }
            }
        }
        return sb.toString();
    }
}
