package com.projectestimation.backend.rtm.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.rtm.dto.RtmRowDto;

@Service
public class RtmExcelService {

    private static final Logger log = LoggerFactory.getLogger(RtmExcelService.class);

    private static final String CLASSPATH_TEMPLATE = "rtm/Requirement Traceability Matrix.xls";
    private static final String FILE_TEMPLATE = "C:/Users/BeasDeveloper/Downloads/Requirement Traceability Matrix.xls";

    public byte[] generateExcel(Opportunity opportunity, List<RtmRowDto> rows) throws IOException {
        try (InputStream is = getTemplateInputStream();
             Workbook workbook = WorkbookFactory.create(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet devEnhnSheet = getDevEnhnSheet(workbook);

            // 1. Update Project Header info in Dev_Enhn
            updateProjectInfo(devEnhnSheet, opportunity);

            // 2. Populate Dev_Enhn data rows
            populateDevEnhnSheet(devEnhnSheet, rows);

            // 3. Ensure other 3 sheets remain blank (clear legacy data from CR if present)
            clearOtherSheetsData(workbook);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private Sheet getDevEnhnSheet(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet s = workbook.getSheetAt(i);
            if (s.getSheetName().equalsIgnoreCase("Dev_Enhn")
                    || s.getSheetName().equalsIgnoreCase("dev_enhn")) {
                return s;
            }
        }
        return workbook.getSheetAt(0);
    }

    private void updateProjectInfo(Sheet sheet, Opportunity opportunity) {
        if (opportunity == null) {
            return;
        }

        // Row 4: Project Code
        Row row4 = sheet.getRow(4);
        if (row4 != null && row4.getCell(0) != null) {
            String code = opportunity.getId() != null ? "EXT/DEV/OPP/" + opportunity.getId() : "EXT/DEV/LS/ET";
            row4.getCell(0).setCellValue("  Project Code:     " + code);
        }

        // Row 5: Project Name
        Row row5 = sheet.getRow(5);
        if (row5 != null && row5.getCell(0) != null) {
            String name = opportunity.getOpportunityName() != null && !opportunity.getOpportunityName().isBlank()
                    ? opportunity.getOpportunityName()
                    : "Netzr Procurement";
            row5.getCell(0).setCellValue("  Project Name:     " + name);
        }

        // Row 6: Customer Name
        Row row6 = sheet.getRow(6);
        if (row6 != null && row6.getCell(0) != null) {
            String customer = opportunity.getClientName() != null && !opportunity.getClientName().isBlank()
                    ? opportunity.getClientName()
                    : "Client";
            row6.getCell(0).setCellValue("  Customer Name: " + customer);
        }
    }

    private void populateDevEnhnSheet(Sheet sheet, List<RtmRowDto> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        // Save styles from row 11 (the first data row of sample template)
        Row templateRow = sheet.getRow(11);
        CellStyle[] styles = new CellStyle[9];
        float rowHeight = templateRow != null ? templateRow.getHeightInPoints() : 25.0f;

        if (templateRow != null) {
            for (int c = 0; c < 9; c++) {
                Cell cell = templateRow.getCell(c);
                if (cell != null) {
                    styles[c] = cell.getCellStyle();
                }
            }
        }

        // Remove old merged regions in data area (rows 11 onwards)
        List<Integer> regionsToRemove = new ArrayList<>();
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            if (range.getFirstRow() >= 11) {
                regionsToRemove.add(i);
            }
        }
        for (int i = regionsToRemove.size() - 1; i >= 0; i--) {
            sheet.removeMergedRegion(regionsToRemove.get(i));
        }

        // Remove existing sample rows from 11 onwards
        int lastRowNum = sheet.getLastRowNum();
        for (int r = 11; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                sheet.removeRow(row);
            }
        }

        // Write rows for FR-001 through FR-012
        int startRow = 11;
        for (int i = 0; i < rows.size(); i++) {
            RtmRowDto dto = rows.get(i);
            Row row = sheet.createRow(startRow + i);
            row.setHeightInPoints(Math.max(rowHeight, 30.0f));

            // Col 0: Serial No.
            setCell(row, 0, dto.getSerialNo() != null ? dto.getSerialNo() : (i + 1) + ".0", styles[0]);

            // Col 1: Requirements (FR ID + Name)
            String reqText = formatRequirementText(dto);
            setCell(row, 1, reqText, styles[1]);

            // Col 2: Sub-section Reference in SRS
            setCell(row, 2, dto.getSrsSubsection(), styles[2]);

            // Col 3: Sub-section Reference in SDD
            setCell(row, 3, dto.getSddSubsection(), styles[3]);

            // Col 4: Source Code Reference
            setCell(row, 4, dto.getSourceCodeReference() != null ? dto.getSourceCodeReference() : "N.A.", styles[4]);

            // Col 5: Reference of Unit Test Cases
            setCell(row, 5, dto.getUnitTestCaseReference() != null ? dto.getUnitTestCaseReference() : "N.A.", styles[5]);

            // Col 6: Reference of System Test Cases (must match unit test cases exactly)
            setCell(row, 6, dto.getSystemTestCaseReference() != null ? dto.getSystemTestCaseReference() : dto.getUnitTestCaseReference(), styles[6]);

            // Col 7: Reference of Acceptance Test Cases (N.A.)
            setCell(row, 7, dto.getAcceptanceTestCaseReference() != null ? dto.getAcceptanceTestCaseReference() : "N.A.", styles[7]);

            // Col 8: Status (Done)
            setCell(row, 8, dto.getStatus() != null ? dto.getStatus() : "Done", styles[8]);
        }

        // Add footer row after data
        int footerRowIndex = startRow + rows.size() + 1;
        Row footerRow = sheet.createRow(footerRowIndex);
        Cell footerCell1 = footerRow.createCell(1);
        footerCell1.setCellValue("Maintained By: System");
        Cell footerCell4 = footerRow.createCell(4);
        footerCell4.setCellValue("Last Updated :" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }

    private String formatRequirementText(RtmRowDto dto) {
        String id = dto.getRequirementId() != null ? dto.getRequirementId() : "";
        String name = dto.getRequirementName() != null ? dto.getRequirementName() : "";
        if (!id.isBlank() && !name.isBlank()) {
            return id + ": " + name;
        } else if (!name.isBlank()) {
            return name;
        } else if (!id.isBlank()) {
            return id;
        }
        return "";
    }

    private void clearOtherSheetsData(Workbook workbook) {
        // Sheet 1: CR, Sheet 2: Query, Sheet 3: Database Support
        for (int i = 1; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            // Keep header rows (0-9) and remove any existing rows from 10 onwards
            List<Integer> regionsToRemove = new ArrayList<>();
            for (int m = 0; m < sheet.getNumMergedRegions(); m++) {
                CellRangeAddress range = sheet.getMergedRegion(m);
                if (range.getFirstRow() >= 10) {
                    regionsToRemove.add(m);
                }
            }
            for (int m = regionsToRemove.size() - 1; m >= 0; m--) {
                sheet.removeMergedRegion(regionsToRemove.get(m));
            }

            int lastRow = sheet.getLastRowNum();
            for (int r = 10; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row != null) {
                    sheet.removeRow(row);
                }
            }
        }
    }

    private void setCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private InputStream getTemplateInputStream() throws IOException {
        try {
            ClassPathResource res = new ClassPathResource(CLASSPATH_TEMPLATE);
            if (res.exists()) {
                return res.getInputStream();
            }
        } catch (Exception ignored) {
        }

        File f = new File(FILE_TEMPLATE);
        if (f.exists()) {
            return new FileInputStream(f);
        }

        throw new IOException("RTM sample template Excel not found in classpath or downloads");
    }
}
