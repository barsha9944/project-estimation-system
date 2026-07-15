package com.projectestimation.backend.projectschedule.excel;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.util.Objects;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleTaskRequest;

@Component
public class ProjectScheduleExcelExporter {

    public byte[] export(SaveProjectScheduleRequest request) {

        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {

            createProjectScheduleSheet(workbook, request);

            createGanttSheet(workbook, request);

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (Exception ex) {

            throw new RuntimeException(
                    "Failed to generate project schedule excel.",
                    ex
            );

        }

    }

    private void createProjectScheduleSheet(
        XSSFWorkbook workbook,
        SaveProjectScheduleRequest request
) {

    Sheet sheet = workbook.createSheet("Project Schedule");

    CellStyle headerStyle = createTableHeaderStyle(workbook);

    CellStyle cellStyle = createTableCellStyle(workbook);

    CellStyle dateStyle = createDateCellStyle(workbook);

    int rowIndex = 0;

    Row header = sheet.createRow(rowIndex++);

    String[] columns = {

            "Seq",

            "Task",

            "Duration",

            "Planned Start",

            "Planned End",

            "Actual Start",

            "Actual End",

            "Predecessor",

            "Status"

    };

    // Header
    for (int i = 0; i < columns.length; i++) {

        Cell cell = header.createCell(i);

        cell.setCellValue(columns[i]);

        cell.setCellStyle(headerStyle);

    }

    // Data
    for (SaveProjectScheduleTaskRequest task : request.getTasks()) {

        Row row = sheet.createRow(rowIndex++);

        int col = 0;

        Cell cell;

        // Sequence
        cell = row.createCell(col++);
        cell.setCellValue(task.getSequence());
        cell.setCellStyle(cellStyle);

        // Task
        cell = row.createCell(col++);
        cell.setCellValue(task.getTaskName());
        cell.setCellStyle(cellStyle);

        // Duration
        cell = row.createCell(col++);
        cell.setCellValue(task.getDuration());
        cell.setCellStyle(cellStyle);

        // Planned Start
        cell = row.createCell(col++);
        if (task.getPlannedStartDate() != null) {
            cell.setCellValue(task.getPlannedStartDate());
        }
        cell.setCellStyle(dateStyle);

        // Planned End
        cell = row.createCell(col++);
        if (task.getPlannedEndDate() != null) {
            cell.setCellValue(task.getPlannedEndDate());
        }
        cell.setCellStyle(dateStyle);

        // Actual Start
        cell = row.createCell(col++);
        if (task.getActualStartDate() != null) {
            cell.setCellValue(task.getActualStartDate());
        }
        cell.setCellStyle(dateStyle);

        // Actual End
        cell = row.createCell(col++);
        if (task.getActualEndDate() != null) {
            cell.setCellValue(task.getActualEndDate());
        }
        cell.setCellStyle(dateStyle);

        // Predecessor
        cell = row.createCell(col++);
        cell.setCellValue(task.getPredecessor() == null ? "" : task.getPredecessor());
        cell.setCellStyle(cellStyle);

        // Status
        cell = row.createCell(col++);
        cell.setCellValue(task.getStatus() == null ? "" : task.getStatus());
        cell.setCellStyle(cellStyle);

    }

    // Auto-size columns
    for (int i = 0; i < columns.length; i++) {

        sheet.autoSizeColumn(i);

    }

    // Freeze Header Row
    sheet.createFreezePane(0, 1);

    // Add Filters
    sheet.setAutoFilter(
            new CellRangeAddress(
                    0,
                    rowIndex - 1,
                    0,
                    columns.length - 1
            )
    );

}
    private void createGanttSheet(
            XSSFWorkbook workbook,
            SaveProjectScheduleRequest request
    ) {

        Sheet sheet = workbook.createSheet("Gantt Chart");
        
        CellStyle monthHeaderStyle = createMonthHeaderStyle(workbook);

        CellStyle dayHeaderStyle = createDayHeaderStyle(workbook);

        CellStyle taskStyle = createTaskStyle(workbook);

        if (request.getTasks() == null || request.getTasks().isEmpty()) {
            return;
        }

        LocalDate minDate = request.getTasks()
                .stream()
                .map(SaveProjectScheduleTaskRequest::getPlannedStartDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate maxDate = request.getTasks()
                .stream()
                .map(SaveProjectScheduleTaskRequest::getPlannedEndDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(minDate);

        Row monthRow = sheet.createRow(0);

        Row dayRow = sheet.createRow(1);

        Cell taskHeader = monthRow.createCell(0);

        taskHeader.setCellValue("Task");

        taskHeader.setCellStyle(monthHeaderStyle);

        dayRow.createCell(0).setCellValue("");

        int column = 1;

        LocalDate current = minDate;

        int monthStartColumn = 1;

        while (!current.isAfter(maxDate)) {

            Month currentMonth = current.getMonth();

            int startColumn = column;

            while (!current.isAfter(maxDate)
                    && current.getMonth() == currentMonth) {

            	Cell dayCell = dayRow.createCell(column);

            	dayCell.setCellValue(current.getDayOfMonth());

            	dayCell.setCellStyle(dayHeaderStyle);

                column++;

                current = current.plusDays(1);

            }

            int endColumn = column - 1;

            Cell monthCell = monthRow.createCell(startColumn);

            monthCell.setCellValue(
                    currentMonth.name() + " " + current.getYear()
            );

            monthCell.setCellStyle(monthHeaderStyle);

            sheet.addMergedRegion(
                    new CellRangeAddress(
                            0,
                            0,
                            startColumn,
                            endColumn
                    )
            );

        }

        int rowNumber = 2;

        CellStyle plannedStyle =
                workbook.createCellStyle();

        plannedStyle.setFillForegroundColor(
                IndexedColors.LIGHT_BLUE.getIndex()
        );

        plannedStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        CellStyle completedStyle =
                workbook.createCellStyle();

        completedStyle.setFillForegroundColor(
                IndexedColors.BRIGHT_GREEN.getIndex()
        );

        completedStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        CellStyle progressStyle =
                workbook.createCellStyle();

        progressStyle.setFillForegroundColor(
                IndexedColors.ORANGE.getIndex()
        );

        progressStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        for (SaveProjectScheduleTaskRequest task : request.getTasks()) {

            Row row =
                    sheet.createRow(rowNumber++);

            Cell taskCell = row.createCell(0);

            taskCell.setCellValue(task.getTaskName());

            taskCell.setCellStyle(taskStyle);

            if (task.getPlannedStartDate() == null
                    || task.getPlannedEndDate() == null) {

                continue;

            }

            LocalDate day = minDate;

            int excelColumn = 1;

            while (!day.isAfter(maxDate)) {

                if (!day.isBefore(task.getPlannedStartDate())
                        && !day.isAfter(task.getPlannedEndDate())) {

                    Cell cell =
                            row.createCell(excelColumn);

                    CellStyle style =
                            plannedStyle;

                    if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {

                        style = completedStyle;

                    } else if ("IN_PROGRESS".equalsIgnoreCase(task.getStatus())) {

                        style = progressStyle;

                    }

                    cell.setCellStyle(style);

                }

                excelColumn++;

                day = day.plusDays(1);

            }

        }

        sheet.autoSizeColumn(0);

    }
    
    private CellStyle createMonthHeaderStyle(XSSFWorkbook workbook) {

        CellStyle style = workbook.createCellStyle();

        style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        style.setFont(font);

        return style;
    }
    
    
    private CellStyle createDayHeaderStyle(XSSFWorkbook workbook) {

        CellStyle style = workbook.createCellStyle();

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);

        style.setFont(font);

        return style;
    }
    
    private CellStyle createTaskStyle(XSSFWorkbook workbook) {

        CellStyle style = workbook.createCellStyle();

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    private CellStyle createTableHeaderStyle(XSSFWorkbook workbook) {

        CellStyle style = workbook.createCellStyle();

        style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        style.setFont(font);

        return style;
    }
    
    private CellStyle createTableCellStyle(XSSFWorkbook workbook) {

        CellStyle style = workbook.createCellStyle();

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }
    
    private CellStyle createDateCellStyle(XSSFWorkbook workbook) {

        CellStyle style = createTableCellStyle(workbook);

        style.setDataFormat(
                workbook.createDataFormat()
                        .getFormat("dd-MMM-yyyy")
        );

        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }
}