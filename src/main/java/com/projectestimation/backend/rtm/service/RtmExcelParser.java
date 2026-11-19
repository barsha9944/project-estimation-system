package com.projectestimation.backend.rtm.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.rtm.dto.RtmResponseDto;
import com.projectestimation.backend.rtm.dto.RtmRowDto;

@Component
public class RtmExcelParser {

    public RtmResponseDto parse(byte[] excelBytes, Long opportunityId, String oppName, String clientName) throws IOException {
        try (InputStream is = new ByteArrayInputStream(excelBytes);
             Workbook workbook = WorkbookFactory.create(is)) {
            return parseWorkbook(workbook, opportunityId, oppName, clientName);
        }
    }

    public RtmResponseDto parse(Path filePath, Long opportunityId, String oppName, String clientName) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(is)) {
            return parseWorkbook(workbook, opportunityId, oppName, clientName);
        }
    }

    private RtmResponseDto parseWorkbook(Workbook workbook, Long opportunityId, String oppName, String clientName) {
        Sheet sheet = getDevEnhnSheet(workbook);
        List<RtmRowDto> rows = new ArrayList<>();

        int lastRowNum = sheet.getLastRowNum();
        for (int r = 11; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String c0 = getCellString(row.getCell(0));
            String c1 = getCellString(row.getCell(1));

            // Stop if footer row reached
            if (c1.startsWith("Maintained By") || c0.startsWith("Maintained By")) {
                break;
            }

            if (c0.isBlank() && c1.isBlank()) {
                continue;
            }

            String reqId = "";
            String reqName = c1;
            if (c1.contains(":")) {
                int idx = c1.indexOf(':');
                reqId = c1.substring(0, idx).trim();
                reqName = c1.substring(idx + 1).trim();
            } else if (c1.matches("^(?:FR|NFR|REQ)[-_\\d]+.*")) {
                String[] parts = c1.split("\\s+", 2);
                reqId = parts[0].trim();
                reqName = parts.length > 1 ? parts[1].trim() : "";
            }

            String srsSub = getCellString(row.getCell(2));
            String sddSub = getCellString(row.getCell(3));
            String srcRef = getCellString(row.getCell(4));
            String unitTest = getCellString(row.getCell(5));
            String sysTest = getCellString(row.getCell(6));
            String accTest = getCellString(row.getCell(7));
            String status = getCellString(row.getCell(8));

            RtmRowDto rowDto = RtmRowDto.builder()
                    .serialNo(c0)
                    .requirementId(reqId)
                    .requirementName(reqName)
                    .requirementDescription(reqName)
                    .srsSubsection(srsSub)
                    .sddSubsection(sddSub)
                    .sourceCodeReference(!srcRef.isBlank() ? srcRef : "N.A.")
                    .unitTestCaseReference(!unitTest.isBlank() ? unitTest : "N.A.")
                    .systemTestCaseReference(!sysTest.isBlank() ? sysTest : (!unitTest.isBlank() ? unitTest : "N.A."))
                    .acceptanceTestCaseReference(!accTest.isBlank() ? accTest : "N.A.")
                    .status(!status.isBlank() ? status : "Done")
                    .build();

            rows.add(rowDto);
        }

        return new RtmResponseDto(opportunityId, oppName, clientName, rows);
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

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    yield String.valueOf((long) val);
                } else {
                    yield String.valueOf(val);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
