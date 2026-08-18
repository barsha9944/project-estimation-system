package com.projectestimation.backend.projectmetrics.excel;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.common.exception.ProjectScheduleFailedException;
import com.projectestimation.backend.projectmetrics.dto.AnalysisMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.CodingMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.DesignMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.OtherActivityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.QualityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SitMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SprintMetricsResponse;

@Component
public class ProjectMetricsExcelExporter {

    public byte[] export(ProjectMetricsResponse metrics) {

        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {

            Sheet sheet =
                    workbook.createSheet("Summary Project Metrics");

            createSummarySection(sheet, workbook, metrics);

            createAnalysisSection(sheet, workbook, metrics);

            createDesignSection(sheet, workbook, metrics);

            createCodingSection(sheet, workbook, metrics);

            createSitSection(sheet, workbook, metrics);

            createOtherActivitySection(sheet, workbook, metrics);

            createQualitySection(sheet, workbook, metrics);

            formatSheet(sheet);

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (Exception ex) {

            throw new ProjectScheduleFailedException(
                    "Failed to generate project metrics excel.",
                    ex
            );
        }
    }

    // =========================================================
    // SUMMARY
    // =========================================================

    private void createSummarySection(
            Sheet sheet,
            XSSFWorkbook workbook,
            ProjectMetricsResponse metrics) {

        int row = sheet.getLastRowNum() + 2;

        createSectionTitle(
                sheet,
                workbook,
                row,
                0,
                16,
                "Summary Project Data & Metrics Values",
                RED
        );

        row++;

        String[] headers = {
                "Project Name",
                "Release No",
                "Original Size\n(Use Case Point)",
                "Actual Size\n(Use Case Point)",
                "Size Variance (%)",
                "Total Planned Effort\nWithout PM Effort\n(Person Hours)",
                "Total Planned Effort\n(Person Hours)",
                "Total Actual Effort\nWithout PM Effort\n(Person Hours)",
                "Total Actual Effort\n(Person Hours)",
                "Effort Variance (%)",
                "Planned Duration\n(Calendar Days)",
                "Actual Duration\n(Calendar Days)",
                "Schedule Variance (%)",
                "Actual Overall\nProductivity",
                "Review Effectiveness",
                "Testing Effectiveness"
        };

        CellStyle[] styles = createHeaderStyles(
                workbook,
                headers.length,
                RED
        );

        createHeaderRow(
                sheet,
                workbook,
                row,
                headers,
                styles
        );

        row++;

        if (metrics.getSprints() == null) {
            return;
        }

        for (SprintMetricsResponse sprint : metrics.getSprints()) {

            Row dataRow = sheet.createRow(row++);

            setString(
                    dataRow.createCell(0),
                    metrics.getSummary().getProjectName()
            );

            setString(
                    dataRow.createCell(1),
                    metrics.getSummary().getReleaseNo()
            );

            setDouble(
                    dataRow.createCell(2),
                    metrics.getSummary().getOriginalSize()
            );

            setDouble(
                    dataRow.createCell(3),
                    metrics.getSummary().getActualSize()
            );

            setDouble(
                    dataRow.createCell(4),
                    metrics.getSummary().getSizeVariance()
            );

            setDouble(
                    dataRow.createCell(5),
                    metrics.getSummary()
                            .getTotalPlannedEffortWithoutPm()
            );

            setDouble(
                    dataRow.createCell(6),
                    metrics.getSummary()
                            .getTotalPlannedEffort()
            );

            setDouble(
                    dataRow.createCell(7),
                    metrics.getSummary()
                            .getTotalActualEffortWithoutPm()
            );

            setDouble(
                    dataRow.createCell(8),
                    metrics.getSummary()
                            .getTotalActualEffort()
            );

            setDouble(
                    dataRow.createCell(9),
                    metrics.getSummary()
                            .getEffortVariance()
            );

            setInteger(
                    dataRow.createCell(10),
                    metrics.getSummary()
                            .getPlannedDuration()
            );

            setInteger(
                    dataRow.createCell(11),
                    metrics.getSummary()
                            .getActualDuration()
            );

            setDouble(
                    dataRow.createCell(12),
                    metrics.getSummary()
                            .getScheduleVariance()
            );

            setDouble(
                    dataRow.createCell(13),
                    metrics.getSummary()
                            .getActualOverallProductivity()
            );

            setDouble(
                    dataRow.createCell(14),
                    metrics.getSummary()
                            .getReviewEffectiveness()
            );

            setDouble(
                    dataRow.createCell(15),
                    metrics.getSummary()
                            .getTestingEffectiveness()
            );

            applyBodyStyle(dataRow, 16);
        }
    }

    // =========================================================
    // ANALYSIS
    // =========================================================

    private void createAnalysisSection(
            Sheet sheet,
            XSSFWorkbook workbook,
            ProjectMetricsResponse metrics) {

        int row = nextSectionRow(sheet);

        String[] headers = {
                "Task Name",
                "Planned Duration\n(Calendar Days)",
                "Actual Duration\n(Calendar Days)",
                "Schedule Variance (%)",
                "Planned Effort including\nReview(Person Hours)",
                "Actual Total Effort\n(Person Hours)",
                "Analysis Productivity\nUCP/Effort",
                "Effort Variance (%)",
                "Effort in Analysis\n(and Rework)\nperson-hour",
                "Analysis Review Defects\n(No.)",
                "Analysis Review Effort\n(Person Hours)",
                "Defect Density - Analysis",
                "Defect Detection Rate - Analysis",
                "Defect Rate - Analysis"
        };

        row = createSectionWithHeaders(
                sheet,
                workbook,
                row,
                "Analysis",
                headers,
                BLUE
        );

        for (SprintMetricsResponse sprint : metrics.getSprints()) {

            AnalysisMetricsResponse data =
                    sprint.getAnalysis();

            Row excelRow = sheet.createRow(row++);

            setString(
                    excelRow.createCell(0),
                    getTaskName(sprint)
            );

            setInteger(excelRow.createCell(1), data.getPlannedDuration());
            setInteger(excelRow.createCell(2), data.getActualDuration());
            setDouble(excelRow.createCell(3), data.getScheduleVariance());
            setDouble(excelRow.createCell(4), data.getPlannedEffort());
            setDouble(excelRow.createCell(5), data.getActualEffort());
            setDouble(excelRow.createCell(6), data.getProductivity());
            setDouble(excelRow.createCell(7), data.getEffortVariance());
            setDouble(excelRow.createCell(8), data.getEffortInAnalysis());
            setInteger(excelRow.createCell(9), data.getReviewDefects());
            setDouble(excelRow.createCell(10), data.getReviewEffort());
            setDouble(excelRow.createCell(11), data.getDefectDensity());
            setDouble(excelRow.createCell(12), data.getDefectDetectionRate());
            setDouble(excelRow.createCell(13), data.getDefectRate());

            applyBodyStyle(excelRow, headers.length);
        }
    }

    // =========================================================
    // DESIGN
    // =========================================================

    private void createDesignSection(
            Sheet sheet,
            XSSFWorkbook workbook,
            ProjectMetricsResponse metrics) {

        int row = nextSectionRow(sheet);

        String[] headers = {
                "Task Name",
                "Planned Duration\n(Calendar Days)",
                "Actual Duration\n(Calendar Days)",
                "Schedule Variance (%)",
                "Planned Effort including\nReview(Person Hours)",
                "Actual Total Effort\n(Person Hours)",
                "Design Productivity\nUCP/Effort",
                "Effort Variance (%)",
                "Effort in Design\n(and Rework)\nperson-hour",
                "Design Review Defects\n(No.)",
                "Design Review Effort\n(Person Hours)",
                "Defect Density - Design",
                "Defect Detection Rate - Design",
                "Defect Rate - Design"
        };

        row = createSectionWithHeaders(
                sheet,
                workbook,
                row,
                "Design",
                headers,
                BLUE
        );

        for (SprintMetricsResponse sprint : metrics.getSprints()) {

            DesignMetricsResponse data =
                    sprint.getDesign();

            Row excelRow = sheet.createRow(row++);

            setString(
                    excelRow.createCell(0),
                    getTaskName(sprint)
            );

            setInteger(excelRow.createCell(1), data.getPlannedDuration());
            setInteger(excelRow.createCell(2), data.getActualDuration());
            setDouble(excelRow.createCell(3), data.getScheduleVariance());
            setDouble(excelRow.createCell(4), data.getPlannedEffort());
            setDouble(excelRow.createCell(5), data.getActualEffort());
            setDouble(excelRow.createCell(6), data.getProductivity());
            setDouble(excelRow.createCell(7), data.getEffortVariance());
            setDouble(excelRow.createCell(8), data.getEffortInAnalysis());
            setInteger(excelRow.createCell(9), data.getReviewDefects());
            setDouble(excelRow.createCell(10), data.getReviewEffort());
            setDouble(excelRow.createCell(11), data.getDefectDensity());
            setDouble(excelRow.createCell(12), data.getDefectDetectionRate());
            setDouble(excelRow.createCell(13), data.getDefectRate());

            applyBodyStyle(excelRow, headers.length);
        }
    }

    // =========================================================
    // CODING
    // =========================================================

    private void createCodingSection(
            Sheet sheet,
            XSSFWorkbook workbook,
            ProjectMetricsResponse metrics) {

        int row = nextSectionRow(sheet);

        String[] headers = {
                "Task Name",
                "Planned Duration\n(Calendar Days)",
                "Actual Duration\n(Calendar Days)",
                "Schedule Variance (%)",
                "Planned Effort incl Review & Testing\n(Person Hours)",
                "Actual Total Effort\n(Person Hours)",
                "Effort Variance (%)",
                "Effort in Coding\n(and Rework)\nperson-hour",
                "Code Review Defects\n(No.)",
                "Code Review Effort\n(Person Hours)",
                "Defect Density - Coding",
                "Defect Detection Rate - Code Review",
                "Unit Testing Defects\n(No.)",
                "Unit Testing Effort\n(Person Hours)",
                "Unit Testing Detection Rate",
                "Defect Rate - Coding",
                "Coding Productivity"
        };

        row = createSectionWithHeaders(
                sheet,
                workbook,
                row,
                "Coding & Unit Testing",
                headers,
                GREEN
        );

        for (SprintMetricsResponse sprint : metrics.getSprints()) {

            CodingMetricsResponse data =
                    sprint.getCoding();

            Row excelRow = sheet.createRow(row++);

            setString(excelRow.createCell(0), getTaskName(sprint));

            setInteger(excelRow.createCell(1), data.getPlannedDuration());
            setInteger(excelRow.createCell(2), data.getActualDuration());
            setDouble(excelRow.createCell(3), data.getScheduleVariance());
            setDouble(excelRow.createCell(4), data.getPlannedEffort());
            setDouble(excelRow.createCell(5), data.getActualEffort());
            setDouble(excelRow.createCell(6), data.getEffortVariance());
            setDouble(excelRow.createCell(7), data.getCodingEffort());
            setInteger(excelRow.createCell(8), data.getCodeReviewDefects());
            setDouble(excelRow.createCell(9), data.getCodeReviewEffort());
            setDouble(excelRow.createCell(10), data.getDefectDensity());
            setDouble(excelRow.createCell(11), data.getCodeReviewDetectionRate());
            setInteger(excelRow.createCell(12), data.getUnitTestingDefects());
            setDouble(excelRow.createCell(13), data.getUnitTestingEffort());
            setDouble(excelRow.createCell(14), data.getUnitTestingDetectionRate());
            setDouble(excelRow.createCell(15), data.getDefectRate());
            setDouble(excelRow.createCell(16), data.getProductivity());

            applyBodyStyle(excelRow, headers.length);
        }
    }

    // =========================================================
    // SIT
    // =========================================================

    private void createSitSection(
            Sheet sheet,
            XSSFWorkbook workbook,
            ProjectMetricsResponse metrics) {

        int row = nextSectionRow(sheet);

        String[] headers = {
                "Task Name",
                "Planned Duration\n(Calendar Days)",
                "Actual Duration\n(Calendar Days)",
                "Schedule Variance (%)",
                "Planned Effort incl. Review & Testing\n(Person Hours)",
                "Actual Total Effort\n(Person Hours)",
                "Effort Variance (%)",
                "Total Number of Test Condition\n(No)",
                "Effort in Writing Test Case incl Test Planning\n(person-hour)",
                "SIT Test Case Review Defects\n(No.)",
                "SIT Test Case Review Effort\n(Person Hours)",
                "Effort in Test Execution\n(person-hour)",
                "Test Case Review Detection Rate",
                "SIT Defects\n(No.)",
                "SIT Effort\n(Person Hours)",
                "Defect Detection Rate - SIT"
        };

        row = createSectionWithHeaders(
                sheet,
                workbook,
                row,
                "System & Integration Testing (SIT)",
                headers,
                PURPLE
        );

        for (SprintMetricsResponse sprint : metrics.getSprints()) {

            SitMetricsResponse data =
                    sprint.getSit();

            Row excelRow = sheet.createRow(row++);

            setString(excelRow.createCell(0), getTaskName(sprint));

            setInteger(excelRow.createCell(1), data.getPlannedDuration());
            setInteger(excelRow.createCell(2), data.getActualDuration());
            setDouble(excelRow.createCell(3), data.getScheduleVariance());
            setDouble(excelRow.createCell(4), data.getPlannedEffort());
            setDouble(excelRow.createCell(5), data.getActualEffort());
            setDouble(excelRow.createCell(6), data.getEffortVariance());
            setInteger(excelRow.createCell(7), data.getTotalTestConditions());
            setDouble(excelRow.createCell(8), data.getTestCaseWritingEffort());
            setInteger(excelRow.createCell(9), data.getTestCaseReviewDefects());
            setDouble(excelRow.createCell(10), data.getTestCaseReviewEffort());
            setDouble(excelRow.createCell(11), data.getTestExecutionEffort());
            setDouble(excelRow.createCell(12), data.getTestCaseReviewDetectionRate());
            setInteger(excelRow.createCell(13), data.getSitDefects());
            setDouble(excelRow.createCell(14), data.getSitEffort());
            setDouble(excelRow.createCell(15), data.getSitDetectionRate());

            applyBodyStyle(excelRow, headers.length);
        }
    }

    // =========================================================
    // OTHER ACTIVITY
    // =========================================================

    private void createOtherActivitySection(
            Sheet sheet,
            XSSFWorkbook workbook,
            ProjectMetricsResponse metrics) {

        int row = nextSectionRow(sheet);

        String[] headers = {
                "Task Name",
                "Actual Total Other Activity\n(Person Hour)",
                "Actual Project Management,\nMonitoring & Control\n(Person Hour)",
                "Actual Support Group Activity\n(Person Hour)",
                "Actual Others\n(Person Hour)",
                "Planned Total Other Activity\n(Person Hour)",
                "Planned Project Management,\nMonitoring & Control\n(Person Hour)",
                "Planned Support Group Activity\n(Person Hour)",
                "Planned Others\n(Person Hour)"
        };

        row = createSectionWithHeaders(
                sheet,
                workbook,
                row,
                "Other Activity",
                headers,
                GREY
        );

        for (SprintMetricsResponse sprint : metrics.getSprints()) {

            OtherActivityMetricsResponse data =
                    sprint.getOtherActivity();

            Row excelRow = sheet.createRow(row++);

            setString(excelRow.createCell(0), getTaskName(sprint));

            setDouble(excelRow.createCell(1), data.getActualTotal());
            setDouble(excelRow.createCell(2), data.getActualProjectManagement());
            setDouble(excelRow.createCell(3), data.getActualSupportGroup());
            setDouble(excelRow.createCell(4), data.getActualOthers());

            setDouble(excelRow.createCell(5), data.getPlannedTotal());
            setDouble(excelRow.createCell(6), data.getPlannedProjectManagement());
            setDouble(excelRow.createCell(7), data.getPlannedSupportGroup());
            setDouble(excelRow.createCell(8), data.getPlannedOthers());

            applyBodyStyle(excelRow, headers.length);
        }
    }

    // =========================================================
    // QUALITY
    // =========================================================

    private void createQualitySection(
            Sheet sheet,
            XSSFWorkbook workbook,
            ProjectMetricsResponse metrics) {

        int row = nextSectionRow(sheet);

        String[] headers = {
                "Task Name",
                "Average Pre-delivery Defect Density\n(No. of Defects/UCP)",
                "UAT Defects",
                "Post Delivery Defect Density\n(No. of Defects/UCP)",
                "Overall Defect Density\n(No. of Defects/UCP)",
                "Planned UAT Effort\n(person-hour)",
                "Effort spent during UAT\n(Person-hours)",
                "Overall Defect Rate\n(defect/person-hour)",
                "Defect removal Efficiency (%)"
        };

        row = createSectionWithHeaders(
                sheet,
                workbook,
                row,
                "Quality / UAT",
                headers,
                YELLOW
        );

        QualityMetricsResponse data =
                metrics.getQuality();

        for (SprintMetricsResponse sprint : metrics.getSprints()) {

            Row excelRow = sheet.createRow(row++);

            setString(excelRow.createCell(0), getTaskName(sprint));

            setDouble(
                    excelRow.createCell(1),
                    data.getAveragePreDeliveryDefectDensity()
            );

            setInteger(
                    excelRow.createCell(2),
                    data.getUatDefects()
            );

            setDouble(
                    excelRow.createCell(3),
                    data.getPostDeliveryDefectDensity()
            );

            setDouble(
                    excelRow.createCell(4),
                    data.getOverallDefectDensity()
            );

            setDouble(
                    excelRow.createCell(5),
                    data.getPlannedUatEffort()
            );

            setDouble(
                    excelRow.createCell(6),
                    data.getActualUatEffort()
            );

            setDouble(
                    excelRow.createCell(7),
                    data.getOverallDefectRate()
            );

            setDouble(
                    excelRow.createCell(8),
                    data.getDefectRemovalEfficiency()
            );

            applyBodyStyle(excelRow, headers.length);
        }
    }

    // =========================================================
    // COMMON EXCEL METHODS
    // =========================================================

    private static final byte[] BLUE =
            new byte[]{(byte) 157, (byte) 195, (byte) 230};

    private static final byte[] RED =
            new byte[]{(byte) 255, (byte) 124, (byte) 128};

    private static final byte[] YELLOW =
            new byte[]{(byte) 255, (byte) 255, (byte) 153};

    private static final byte[] GREEN =
            new byte[]{(byte) 198, (byte) 239, (byte) 206};

    private static final byte[] PURPLE =
            new byte[]{(byte) 198, (byte) 160, (byte) 246};

    private static final byte[] GREY =
            new byte[]{(byte) 217, (byte) 217, (byte) 217};

    private void createSectionTitle(
            Sheet sheet,
            XSSFWorkbook workbook,
            int rowIndex,
            int startColumn,
            int endColumn,
            String title,
            byte[] color) {

        Row row = sheet.createRow(rowIndex);

        Cell cell = row.createCell(startColumn);
        cell.setCellValue(title);

        CellStyle style =
                createColoredStyle(workbook, color, true);

        cell.setCellStyle(style);

        for (int i = startColumn + 1; i <= endColumn; i++) {
            row.createCell(i).setCellStyle(style);
        }

        sheet.addMergedRegion(
                new CellRangeAddress(
                        rowIndex,
                        rowIndex,
                        startColumn,
                        endColumn
                )
        );

        row.setHeightInPoints(24);
    }

    private int createSectionWithHeaders(
            Sheet sheet,
            XSSFWorkbook workbook,
            int row,
            String title,
            String[] headers,
            byte[] color) {

        createSectionTitle(
                sheet,
                workbook,
                row,
                0,
                headers.length - 1,
                title,
                color
        );

        row++;

        CellStyle headerStyle =
                createColoredStyle(
                        workbook,
                        color,
                        true
                );

        Row headerRow = sheet.createRow(row);

        for (int i = 0; i < headers.length; i++) {

            Cell cell = headerRow.createCell(i);

            cell.setCellValue(headers[i]);

            cell.setCellStyle(headerStyle);
        }

        headerRow.setHeightInPoints(70);

        return row + 1;
    }

    private CellStyle createColoredStyle(
            XSSFWorkbook workbook,
            byte[] color,
            boolean bold) {

        CellStyle style = workbook.createCellStyle();

        style.setFillForegroundColor(
                new XSSFColor(color, null)
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setWrapText(true);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(bold);

        style.setFont(font);

        return style;
    }

    private void createHeaderRow(
            Sheet sheet,
            XSSFWorkbook workbook,
            int rowIndex,
            String[] headers,
            CellStyle[] styles) {

        Row row = sheet.createRow(rowIndex);

        for (int i = 0; i < headers.length; i++) {

            Cell cell = row.createCell(i);

            cell.setCellValue(headers[i]);

            cell.setCellStyle(styles[i]);
        }

        row.setHeightInPoints(80);
    }

    private CellStyle[] createHeaderStyles(
            XSSFWorkbook workbook,
            int count,
            byte[] color) {

        CellStyle[] styles = new CellStyle[count];

        for (int i = 0; i < count; i++) {
            styles[i] =
                    createColoredStyle(
                            workbook,
                            color,
                            true
                    );
        }

        return styles;
    }

    private void applyBodyStyle(
            Row row,
            int columnCount) {

        for (int i = 0; i < columnCount; i++) {

            Cell cell = row.getCell(i);

            if (cell == null) {
                cell = row.createCell(i);
            }

            CellStyle style =
                    row.getSheet()
                            .getWorkbook()
                            .createCellStyle();

            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);

            style.setVerticalAlignment(
                    VerticalAlignment.CENTER
            );

            cell.setCellStyle(style);
        }
    }

    private void setString(Cell cell, String value) {
        cell.setCellValue(
                value == null ? "" : value
        );
    }

    private void setDouble(Cell cell, Double value) {

        if (value == null) {
            cell.setCellValue("");
        } else {
            cell.setCellValue(value);
        }
    }

    private void setInteger(Cell cell, Integer value) {

        if (value == null) {
            cell.setCellValue("");
        } else {
            cell.setCellValue(value);
        }
    }

    private String getTaskName(
            SprintMetricsResponse sprint) {

        return sprint.getTaskName() == null
                ? "Sprint " + sprint.getSprintNumber()
                : sprint.getTaskName();
    }

    private int nextSectionRow(Sheet sheet) {

        return sheet.getLastRowNum() + 3;
    }

    private void formatSheet(Sheet sheet) {

        int maxColumns = 17;

        for (int i = 0; i < maxColumns; i++) {

            sheet.setColumnWidth(
                    i,
                    5000
            );
        }

        sheet.setColumnWidth(0, 6500);

        sheet.createFreezePane(1, 2);
    }
}