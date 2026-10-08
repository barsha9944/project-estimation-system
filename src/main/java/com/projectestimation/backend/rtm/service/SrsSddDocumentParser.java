package com.projectestimation.backend.rtm.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.rtm.dto.RtmRowDto;
import com.projectestimation.backend.sdd.dto.ApplicationComponentDetailsDto;
import com.projectestimation.backend.sdd.dto.ApplicationComponentDto;
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.dto.SddTraceabilityMatrixEntryDto;
import com.projectestimation.backend.srs.dto.FunctionalRequirementDto;
import com.projectestimation.backend.srs.dto.SrsDto;

/**
 * Parses and extracts Requirements Traceability Matrix entries dynamically
 * from opportunity-specific SRS and SDD documents or DTOs.
 * 
 * Does NOT contain any hardcoded document paths or static file dependencies.
 */
@Component
public class SrsSddDocumentParser {

    private static final Logger log = LoggerFactory.getLogger(SrsSddDocumentParser.class);

    /**
     * Extracts RTM rows dynamically from opportunity-specific SRS and SDD DTOs.
     *
     * @param srsDto the opportunity's SRS DTO
     * @param sddDto the opportunity's SDD DTO
     * @return list of RtmRowDto containing requirement and design details
     */
    public List<RtmRowDto> parseRequirements(SrsDto srsDto, SddDto sddDto) {
        if (srsDto == null) {
            throw new IllegalArgumentException("SRS document data cannot be null");
        }

        List<FunctionalRequirementDto> functionalRequirements = srsDto.functionalRequirements();
        if (functionalRequirements == null || functionalRequirements.isEmpty()) {
            log.warn("No functional requirements present in the provided SRS");
            return new ArrayList<>();
        }

        // 1. Build SDD lookup maps (by requirementId and by requirement number)
        Map<String, String> sddSubsectionByReqId = new HashMap<>();
        Map<String, String> sddDescriptionByReqId = new HashMap<>();

        if (sddDto != null) {
            // Check SDD Traceability Matrix if present
            if (sddDto.requirementsTraceabilityMatrix() != null) {
                for (SddTraceabilityMatrixEntryDto entry : sddDto.requirementsTraceabilityMatrix()) {
                    if (entry == null || entry.requirementId() == null) {
                        continue;
                    }
                    String reqId = entry.requirementId().trim().toUpperCase();
                    String designComp = entry.designComponent() != null ? entry.designComponent().trim() : "";
                    String desc = entry.requirementDescription() != null ? entry.requirementDescription().trim() : "";

                    if (!designComp.isBlank()) {
                        sddSubsectionByReqId.put(reqId, normalizeSddSubsection(designComp));
                    }
                    if (!desc.isBlank()) {
                        sddDescriptionByReqId.put(reqId, desc);
                    }
                }
            }

            // Check SDD Application Components for module/component descriptions
            if (sddDto.applicationComponents() != null) {
                for (ApplicationComponentDto comp : sddDto.applicationComponents()) {
                    if (comp == null) {
                        continue;
                    }
                    String compNum = comp.componentNumber() != null ? comp.componentNumber().trim() : "";
                    String compDesc = comp.description() != null ? comp.description().trim() : "";
                    if (!compNum.isBlank() && !compDesc.isBlank()) {
                        sddDescriptionByReqId.putIfAbsent(compNum, compDesc);
                    }

                    if (comp.details() != null) {
                        for (ApplicationComponentDetailsDto detail : comp.details()) {
                            if (detail == null) {
                                continue;
                            }
                            String subNum = detail.subComponentNumber() != null ? detail.subComponentNumber().trim() : "";
                            String subDesc = detail.description() != null ? detail.description().trim() : "";
                            if (!subNum.isBlank() && !subDesc.isBlank()) {
                                sddDescriptionByReqId.putIfAbsent(subNum, subDesc);
                            }
                        }
                    }
                }
            }
        }

        // 2. Map each functional requirement into an RtmRowDto
        List<RtmRowDto> rows = new ArrayList<>();
        for (int i = 0; i < functionalRequirements.size(); i++) {
            FunctionalRequirementDto req = functionalRequirements.get(i);
            if (req == null) {
                continue;
            }

            String reqId = req.requirementId() != null && !req.requirementId().isBlank()
                    ? req.requirementId().trim()
                    : String.format("FR-%03d", i + 1);

            int reqNum = extractRequirementNumber(reqId);
            if (reqNum <= 0) {
                reqNum = i + 1;
            }

            String serialNo = reqNum + ".0";
            String srsSubsection = "5." + reqNum;

            // SDD subsection: lookup from SDD or fallback to canonical SDD 3.x mapping (FR-001 -> SDD 3.1, etc.)
            String sddSubsection = sddSubsectionByReqId.get(reqId.toUpperCase());
            if (sddSubsection == null || sddSubsection.isBlank()) {
                sddSubsection = "3." + reqNum;
            }

            // Requirement descriptions
            String reqName = req.requirementName() != null ? req.requirementName().trim() : "";
            String reqDesc = req.description() != null && !req.description().isBlank()
                    ? req.description().trim()
                    : reqName;

            // SDD requirement description
            String sddDesc = sddDescriptionByReqId.get(reqId.toUpperCase());
            if (sddDesc == null || sddDesc.isBlank()) {
                sddDesc = sddDescriptionByReqId.get(sddSubsection);
            }
            if (sddDesc == null || sddDesc.isBlank()) {
                sddDesc = reqDesc;
            }

            RtmRowDto row = RtmRowDto.builder()
                    .serialNo(serialNo)
                    .requirementId(reqId)
                    .requirementName(reqName)
                    .requirementDescription(sddDesc)
                    .srsSubsection(srsSubsection)
                    .sddSubsection(sddSubsection)
                    .sourceCodeReference("N.A.")
                    .unitTestCaseReference("N.A.")
                    .systemTestCaseReference("N.A.")
                    .acceptanceTestCaseReference("N.A.")
                    .status("Done")
                    .build();

            rows.add(row);
        }

        log.info("Extracted {} RTM requirements from opportunity SRS and SDD DTOs", rows.size());
        return rows;
    }

    /**
     * Parses requirements dynamically from opportunity-specific DOCX streams if provided.
     *
     * @param srsStream InputStream for the opportunity's SRS docx
     * @param sddStream InputStream for the opportunity's SDD docx (optional)
     * @return list of RtmRowDto
     */
    public List<RtmRowDto> parseRequirementsFromStreams(InputStream srsStream, InputStream sddStream) {
        if (srsStream == null) {
            throw new IllegalArgumentException("SRS input stream cannot be null");
        }

        try (XWPFDocument srsDoc = new XWPFDocument(srsStream)) {
            List<RtmRowDto> rows = new ArrayList<>();
            Map<String, String> srsSubsections = new HashMap<>();
            Map<String, String> sddSubsections = new HashMap<>();

            // 1. Extract SRS subsections (e.g. 5.1 -> 5.1 User Authentication)
            for (XWPFParagraph p : srsDoc.getParagraphs()) {
                String text = p.getText().trim();
                if (text.startsWith("5.") && text.length() > 3) {
                    String[] parts = text.split("\\s+", 2);
                    if (parts.length > 0 && parts[0].matches("5\\.\\d+")) {
                        srsSubsections.put(parts[0], text);
                    }
                }
            }

            // Extract Traceability Table or Requirements Table from SRS
            for (XWPFTable table : srsDoc.getTables()) {
                if (table.getRows().size() > 1) {
                    XWPFTableRow header = table.getRow(0);
                    if (header != null && header.getCell(0) != null &&
                            header.getCell(0).getText().contains("Requirement ID")) {

                        for (int r = 1; r < table.getRows().size(); r++) {
                            XWPFTableRow row = table.getRow(r);
                            List<XWPFTableCell> cells = row.getTableCells();
                            if (cells.size() >= 2) {
                                String reqId = cells.get(0).getText().trim();
                                String desc = cells.get(1).getText().trim();
                                String reqName = cells.size() >= 5 ? cells.get(4).getText().trim() : desc;

                                int reqNum = extractRequirementNumber(reqId);
                                if (reqNum <= 0) {
                                    reqNum = r;
                                }

                                String srsSub = "5." + reqNum;
                                String sddSub = "3." + reqNum;

                                RtmRowDto dto = RtmRowDto.builder()
                                        .serialNo(reqNum + ".0")
                                        .requirementId(reqId)
                                        .requirementName(reqName)
                                        .requirementDescription(desc)
                                        .srsSubsection(srsSub)
                                        .sddSubsection(sddSub)
                                        .sourceCodeReference("N.A.")
                                        .unitTestCaseReference("N.A.")
                                        .systemTestCaseReference("N.A.")
                                        .acceptanceTestCaseReference("N.A.")
                                        .status("Done")
                                        .build();

                                rows.add(dto);
                            }
                        }
                        break;
                    }
                }
            }

            // 2. Extract SDD subsections if SDD stream provided
            if (sddStream != null) {
                try (XWPFDocument sddDoc = new XWPFDocument(sddStream)) {
                    for (XWPFParagraph p : sddDoc.getParagraphs()) {
                        String text = p.getText().trim();
                        if (text.startsWith("3.") && text.length() > 3) {
                            String[] parts = text.split("\\s+", 2);
                            if (parts.length > 0 && parts[0].matches("3\\.\\d+")) {
                                sddSubsections.put(parts[0], text);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not read SDD stream paragraphs: {}", e.getMessage());
                }
            }

            return rows;
        } catch (Exception e) {
            log.error("Failed to parse requirements from streams: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to parse requirements from document stream: " + e.getMessage(), e);
        }
    }

    private String normalizeSddSubsection(String text) {
        if (text == null) {
            return "";
        }
        text = text.trim();
        if (text.matches("^3\\.\\d+.*")) {
            String[] parts = text.split("\\s+", 2);
            return parts[0];
        }
        return text;
    }

    public int extractRequirementNumber(String reqId) {
        if (reqId == null) {
            return -1;
        }
        String digits = reqId.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
