package com.projectestimation.backend.projectschedule.excel;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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

        Sheet sheet =
                workbook.createSheet("Project Schedule");

        CellStyle headerStyle =
                workbook.createCellStyle();

        Font headerFont =
                workbook.createFont();

        headerFont.setBold(true);

        headerStyle.setFont(headerFont);

        int rowIndex = 0;

        Row header =
                sheet.createRow(rowIndex++);

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

        for (int i = 0; i < columns.length; i++) {

            Cell cell =
                    header.createCell(i);

            cell.setCellValue(columns[i]);

            cell.setCellStyle(headerStyle);

        }

        for (SaveProjectScheduleTaskRequest task : request.getTasks()) {

            Row row =
                    sheet.createRow(rowIndex++);

            int col = 0;

            row.createCell(col++)
                    .setCellValue(task.getSequence());

            row.createCell(col++)
                    .setCellValue(task.getTaskName());

            row.createCell(col++)
                    .setCellValue(task.getDuration());

            row.createCell(col++)
                    .setCellValue(
                            task.getPlannedStartDate() == null
                                    ? ""
                                    : task.getPlannedStartDate().toString()
                    );

            row.createCell(col++)
                    .setCellValue(
                            task.getPlannedEndDate() == null
                                    ? ""
                                    : task.getPlannedEndDate().toString()
                    );

            row.createCell(col++)
                    .setCellValue(
                            task.getActualStartDate() == null
                                    ? ""
                                    : task.getActualStartDate().toString()
                    );

            row.createCell(col++)
                    .setCellValue(
                            task.getActualEndDate() == null
                                    ? ""
                                    : task.getActualEndDate().toString()
                    );

            row.createCell(col++)
                    .setCellValue(task.getPredecessor());

            row.createCell(col++)
                    .setCellValue(task.getStatus());

        }

        for (int i = 0; i < columns.length; i++) {

            sheet.autoSizeColumn(i);

        }

    }
    private void createGanttSheet(
            XSSFWorkbook workbook,
            SaveProjectScheduleRequest request
    ) {

        Sheet sheet = workbook.createSheet("Gantt Chart");

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

        monthRow.createCell(0).setCellValue("Task");

        dayRow.createCell(0).setCellValue("");

        int column = 1;

        LocalDate current = minDate;

        int monthStartColumn = 1;

        while (!current.isAfter(maxDate)) {

            Month currentMonth = current.getMonth();

            int startColumn = column;

            while (!current.isAfter(maxDate)
                    && current.getMonth() == currentMonth) {

                dayRow.createCell(column)
                        .setCellValue(current.getDayOfMonth());

                column++;

                current = current.plusDays(1);

            }

            int endColumn = column - 1;

            Cell monthCell = monthRow.createCell(startColumn);

            monthCell.setCellValue(
                    currentMonth.name() + " " + current.getYear()
            );

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

            row.createCell(0)
                    .setCellValue(task.getTaskName());

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

}