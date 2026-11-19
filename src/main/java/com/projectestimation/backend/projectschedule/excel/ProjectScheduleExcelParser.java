package com.projectestimation.backend.projectschedule.excel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleTaskRequest;
import com.projectestimation.backend.projectschedule.dto.SaveTaskBreakdownRequest;

@Component
public class ProjectScheduleExcelParser {

    public List<SaveProjectScheduleTaskRequest> parseTasks(byte[] excelBytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(excelBytes);
             Workbook workbook = new XSSFWorkbook(is)) {
            return parseTasksFromWorkbook(workbook);
        }
    }

    public List<SaveProjectScheduleTaskRequest> parseTasks(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {
            return parseTasksFromWorkbook(workbook);
        }
    }

    private List<SaveProjectScheduleTaskRequest> parseTasksFromWorkbook(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Project Schedule");
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        if (sheet == null) {
            return new ArrayList<>();
        }

        Map<String, SaveProjectScheduleTaskRequest> taskMap = new LinkedHashMap<>();

        int lastRowNum = sheet.getLastRowNum();
        for (int r = 1; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            Integer seq = getNumericValue(row.getCell(0));
            String taskName = getStringValue(row.getCell(1));
            String breakdownName = getStringValue(row.getCell(2));
            LocalDate plannedStart = getDateValue(row.getCell(3));
            LocalDate plannedEnd = getDateValue(row.getCell(4));
            Integer workingDays = getNumericValue(row.getCell(5));
            LocalDate actualStart = getDateValue(row.getCell(6));
            LocalDate actualEnd = getDateValue(row.getCell(7));
            String predecessor = getStringValue(row.getCell(9));
            String status = getStringValue(row.getCell(10));

            if (taskName == null || taskName.isBlank()) continue;

            String canonicalTaskName = taskName.trim().replaceAll("\\s+", " ");
            String taskKey = canonicalTaskName.toLowerCase();
            SaveProjectScheduleTaskRequest taskRequest = taskMap.get(taskKey);

            if (taskRequest == null) {
                taskRequest = new SaveProjectScheduleTaskRequest();
                taskRequest.setSequence(seq != null ? seq : (taskMap.size() + 1));
                taskRequest.setTaskName(canonicalTaskName);
                taskRequest.setPlannedStartDate(plannedStart);
                taskRequest.setPlannedEndDate(plannedEnd);
                taskRequest.setDuration(workingDays != null ? workingDays : 1);
                taskRequest.setActualStartDate(actualStart != null ? actualStart : plannedStart);
                taskRequest.setActualEndDate(actualEnd != null ? actualEnd : plannedEnd);
                taskRequest.setPredecessor(predecessor);
                taskRequest.setStatus(status != null && !status.isBlank() ? status : "NOT_STARTED");
                taskRequest.setTaskBreakdowns(new ArrayList<>());
                taskMap.put(taskKey, taskRequest);
            } else if (seq != null && (taskRequest.getSequence() == null || seq < taskRequest.getSequence())) {
                taskRequest.setSequence(seq);
            }

            if (breakdownName != null && !breakdownName.isBlank()) {
                String cleanBreakdownName = breakdownName.trim().replaceAll("\\s+", " ");
                boolean exists = taskRequest.getTaskBreakdowns().stream()
                        .anyMatch(b -> b.getActivityName() != null && b.getActivityName().trim().equalsIgnoreCase(cleanBreakdownName));
                if (!exists) {
                    SaveTaskBreakdownRequest breakdown = new SaveTaskBreakdownRequest();
                    breakdown.setActivityName(cleanBreakdownName);
                    breakdown.setPlannedStartDate(plannedStart);
                    breakdown.setPlannedEndDate(plannedEnd);
                    breakdown.setDuration(workingDays != null ? workingDays : 1);
                    breakdown.setActualStartDate(actualStart != null ? actualStart : plannedStart);
                    breakdown.setActualEndDate(actualEnd != null ? actualEnd : plannedEnd);
                    breakdown.setPredecessor(predecessor);
                    breakdown.setStatus(status != null && !status.isBlank() ? status : "NOT_STARTED");
                    breakdown.setProgress(0);
                    breakdown.setSequence(taskRequest.getTaskBreakdowns().size() + 1);

                    taskRequest.getTaskBreakdowns().add(breakdown);
                }
            }
        }

        return new ArrayList<>(taskMap.values());
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Integer getNumericValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return (int) Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private LocalDate getDateValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            if (date != null) {
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return LocalDate.parse(cell.getStringCellValue().trim());
            } catch (Exception ignored) {}
        }
        return null;
    }
}
