package com.projectestimation.backend.review.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.review.dto.ReviewFindingDto;
import com.projectestimation.backend.review.model.DocumentReviewType;

@Component
public class ReviewDataSheetGenerator {

    private static final String FIXED_REVIEWER_NAME = "Manas Chattopadhay";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter META_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yy");

    public byte[] generateReviewDataSheet(
            DocumentReviewType docType,
            String opportunityName,
            String projectManager,
            String teamLead,
            LocalDateTime originalGeneratedAt,
            LocalDateTime cycle1Date,
            LocalDateTime cycle2Date,
            String documentFileName,
            String docVersion,
            List<ReviewFindingDto> findings
    ) throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String oppName = (opportunityName != null && !opportunityName.isBlank())
                    ? opportunityName.trim()
                    : "Project";

            String docCat = (docType == DocumentReviewType.SDD)
                    ? "SDD of " + oppName
                    : "SRS of " + oppName;

            String typeTitle = (docType == DocumentReviewType.SDD) ? "SDD" : "SRS";
            String version = (docVersion != null && !docVersion.isBlank()) ? docVersion : "1.0";
            String pmName = (projectManager != null && !projectManager.isBlank()) ? projectManager : FIXED_REVIEWER_NAME;
            String preparedBy = (teamLead != null && !teamLead.isBlank()) ? teamLead : "Team Lead";

            String cycle1DateStr = (cycle1Date != null)
                    ? cycle1Date.format(DATE_FORMATTER)
                    : (originalGeneratedAt != null ? originalGeneratedAt.format(DATE_FORMATTER) : LocalDateTime.now().format(DATE_FORMATTER));

            String cycle2DateStr = (cycle2Date != null)
                    ? cycle2Date.format(DATE_FORMATTER)
                    : cycle1DateStr;

            String metaCycle1DateStr = (cycle1Date != null)
                    ? cycle1Date.format(META_DATE_FORMATTER)
                    : (originalGeneratedAt != null ? originalGeneratedAt.format(META_DATE_FORMATTER) : LocalDateTime.now().format(META_DATE_FORMATTER));

            String metaCycle2DateStr = (cycle2Date != null)
                    ? cycle2Date.format(META_DATE_FORMATTER)
                    : metaCycle1DateStr;

            String sheetName = oppName.replaceAll("[^a-zA-Z0-9_]", "_") + "_" + typeTitle + "_Review";
            if (sheetName.length() > 31) {
                sheetName = sheetName.substring(0, 31);
            }

            Sheet sheet = workbook.createSheet(sheetName);

            addBeasLogoToSheet(workbook, sheet);
            // Fonts
            Font boldTitleFont = workbook.createFont();
            boldTitleFont.setFontName("Arial");
            boldTitleFont.setFontHeightInPoints((short) 14);
            boldTitleFont.setBold(true);

            Font boldMetaFont = workbook.createFont();
            boldMetaFont.setFontName("Arial");
            boldMetaFont.setFontHeightInPoints((short) 10);
            boldMetaFont.setBold(true);

            Font normalFont = workbook.createFont();
            normalFont.setFontName("Arial");
            normalFont.setFontHeightInPoints((short) 10);

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setBold(true);

            // Cell Styles
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(boldTitleFont);

            CellStyle metaLabelStyle = workbook.createCellStyle();
            metaLabelStyle.setFont(boldMetaFont);

            CellStyle metaValStyle = workbook.createCellStyle();
            metaValStyle.setFont(normalFont);

            CellStyle tableHeaderStyle = workbook.createCellStyle();
            tableHeaderStyle.setFont(headerFont);
            tableHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            tableHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            tableHeaderStyle.setBorderTop(BorderStyle.THIN);
            tableHeaderStyle.setBorderBottom(BorderStyle.THIN);
            tableHeaderStyle.setBorderLeft(BorderStyle.THIN);
            tableHeaderStyle.setBorderRight(BorderStyle.THIN);
            tableHeaderStyle.setWrapText(true);

            CellStyle tableDataStyle = workbook.createCellStyle();
            tableDataStyle.setFont(normalFont);
            tableDataStyle.setBorderTop(BorderStyle.THIN);
            tableDataStyle.setBorderBottom(BorderStyle.THIN);
            tableDataStyle.setBorderLeft(BorderStyle.THIN);
            tableDataStyle.setBorderRight(BorderStyle.THIN);
            tableDataStyle.setVerticalAlignment(VerticalAlignment.TOP);
            tableDataStyle.setWrapText(true);

            CellStyle tableCenterDataStyle = workbook.createCellStyle();
            tableCenterDataStyle.setFont(normalFont);
            tableCenterDataStyle.setBorderTop(BorderStyle.THIN);
            tableCenterDataStyle.setBorderBottom(BorderStyle.THIN);
            tableCenterDataStyle.setBorderLeft(BorderStyle.THIN);
            tableCenterDataStyle.setBorderRight(BorderStyle.THIN);
            tableCenterDataStyle.setAlignment(HorizontalAlignment.CENTER);
            tableCenterDataStyle.setVerticalAlignment(VerticalAlignment.TOP);

            // Row 0: Title
            Row titleRow = sheet.createRow(3);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue(typeTitle + " Review Sheet");
            titleCell.setCellStyle(titleStyle);

            // Metadata rows (Rows 3..9)
            addMetaRow(sheet, 6, "Project Name:", oppName, metaLabelStyle, metaValStyle);
            addMetaRow(sheet, 7, "Project Manager Name:", pmName, metaLabelStyle, metaValStyle);
            addMetaRow(sheet, 8, "Reviewer:", FIXED_REVIEWER_NAME, metaLabelStyle, metaValStyle);
            addMetaRow(sheet, 9, "Prepared by:", preparedBy, metaLabelStyle, metaValStyle);
            addMetaRow(sheet, 10, "Review Date:", metaCycle1DateStr, metaLabelStyle, metaValStyle);
            addMetaRow(sheet, 11, "Approver:", FIXED_REVIEWER_NAME, metaLabelStyle, metaValStyle);
            addMetaRow(sheet, 12, "Approval Date:", metaCycle2DateStr, metaLabelStyle, metaValStyle);

            // Findings Table Header (Row 12)
            int tableStartRow = 15;
            Row headerRow = sheet.createRow(tableStartRow);
            headerRow.setHeightInPoints(24);
            String[] headers = {
                    "ID",
                    "Doc. Cat. (SRS/SDD/PMP)",
                    "Doc. Name",
                    "Doc Version",
                    "Ref. Section",
                    "Defects",
                    "Recommendation",
                    "Severity",
                    "Status as on " + cycle1DateStr,
                    "Status as on " + cycle2DateStr
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            // Populate Gemini findings
            if (findings != null && !findings.isEmpty()) {
                for (int i = 0; i < findings.size(); i++) {
                    ReviewFindingDto finding = findings.get(i);
                    Row row = sheet.createRow(tableStartRow + 1 + i);
                    row.setHeightInPoints(36);

                    // Col 0: ID
                    Cell c0 = row.createCell(0);
                    c0.setCellValue(finding.id() != null ? finding.id() : (i + 1));
                    c0.setCellStyle(tableCenterDataStyle);

                    // Col 1: Doc. Cat.
                    Cell c1 = row.createCell(1);
                    c1.setCellValue(docCat);
                    c1.setCellStyle(tableDataStyle);

                    // Col 2: Doc. Name
                    Cell c2 = row.createCell(2);
                    c2.setCellValue(documentFileName);
                    c2.setCellStyle(tableDataStyle);

                    // Col 3: Doc Version
                    Cell c3 = row.createCell(3);
                    c3.setCellValue(version);
                    c3.setCellStyle(tableCenterDataStyle);

                    // Col 4: Ref. Section
                    Cell c4 = row.createCell(4);
                    c4.setCellValue(finding.referenceSection() != null ? finding.referenceSection() : "");
                    c4.setCellStyle(tableCenterDataStyle);

                    // Col 5: Defects
                    Cell c5 = row.createCell(5);
                    c5.setCellValue(finding.defect() != null ? finding.defect() : "");
                    c5.setCellStyle(tableDataStyle);

                    // Col 6: Recommendation
                    Cell c6 = row.createCell(6);
                    c6.setCellValue(finding.recommendation() != null ? finding.recommendation() : "");
                    c6.setCellStyle(tableDataStyle);

                    // Col 7: Severity
                    Cell c7 = row.createCell(7);
                    c7.setCellValue(finding.severity() != null ? finding.severity() : "Major");
                    c7.setCellStyle(tableCenterDataStyle);

                    // Col 8: Status at Review 1
                    Cell c8 = row.createCell(8);
                    c8.setCellValue(finding.statusAtReview1() != null ? finding.statusAtReview1() : "Open");
                    c8.setCellStyle(tableCenterDataStyle);

                    // Col 9: Status at Review 2
                    Cell c9 = row.createCell(9);
                    c9.setCellValue(finding.statusAtReview2() != null ? finding.statusAtReview2() : "");
                    c9.setCellStyle(tableCenterDataStyle);
                }
            }

            // Set column widths
            sheet.setColumnWidth(0, 1800);  // ID
            sheet.setColumnWidth(1, 7500);  // Doc Cat
            sheet.setColumnWidth(2, 6000);  // Doc Name
            sheet.setColumnWidth(3, 3000);  // Doc Version
            sheet.setColumnWidth(4, 3200);  // Ref. Section
            sheet.setColumnWidth(5, 12000); // Defects
            sheet.setColumnWidth(6, 12000); // Recommendation
            sheet.setColumnWidth(7, 3200);  // Severity
            sheet.setColumnWidth(8, 4800);  // Status Rev 1
            sheet.setColumnWidth(9, 4800);  // Status Rev 2

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void addMetaRow(
            Sheet sheet,
            int rowIdx,
            String label,
            String value,
            CellStyle labelStyle,
            CellStyle valStyle
    ) {
        Row row = sheet.createRow(rowIdx);
        Cell labelCell = row.createCell(1);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valCell = row.createCell(2);
        valCell.setCellValue(value != null ? value : "");
        valCell.setCellStyle(valStyle);
    }
    
    private void addBeasLogoToSheet(
            XSSFWorkbook workbook,
            Sheet sheet) throws IOException {

        try (InputStream logoStream = getClass()
                .getClassLoader()
                .getResourceAsStream("psr/beas-logo.png")) {

            if (logoStream == null) {
                throw new IOException(
                        "BEAS logo not found: src/main/resources/psr/beas-logo.png");
            }

            byte[] logoBytes = logoStream.readAllBytes();

            int pictureIndex = workbook.addPicture(
                    logoBytes,
                    Workbook.PICTURE_TYPE_PNG);

            CreationHelper helper = workbook.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = helper.createClientAnchor();

            anchor.setCol1(0);
            anchor.setRow1(0);
            anchor.setCol2(3);
            anchor.setRow2(3);

            drawing.createPicture(anchor, pictureIndex);
        }
    }
}
