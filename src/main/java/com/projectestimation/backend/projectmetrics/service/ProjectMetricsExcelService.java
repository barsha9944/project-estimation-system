package com.projectestimation.backend.projectmetrics.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

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
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.projectmetrics.model.ProjectMetrics;
import com.projectestimation.backend.projectmetrics.model.ProjectMetricsSprint;
import com.projectestimation.backend.projectmetrics.repository.ProjectMetricsRepository;

@Service
public class ProjectMetricsExcelService {

    private final ProjectMetricsRepository projectMetricsRepository;

    private static final int COLUMN_COUNT = 89;

    public ProjectMetricsExcelService(
            ProjectMetricsRepository projectMetricsRepository) {

        this.projectMetricsRepository = projectMetricsRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateExcel(Long opportunityId) throws IOException {

        ProjectMetrics metrics =
                projectMetricsRepository
                        .findByOpportunityIdWithSprints(opportunityId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Project metrics not found for opportunity "
                                                + opportunityId));

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            Sheet sheet =
                    workbook.createSheet(
                            "Summary Project Metrics");

            createColumnWidths(sheet);

            CellStyle identityStyle =
                    createStyle(workbook, "99CCFF", true, 11);

            CellStyle summaryTitleStyle =
                    createStyle(workbook, "FFFF99", true, 11);

            CellStyle subprocessTitleStyle =
                    createStyle(workbook, "FFCC00", true, 11);

            CellStyle otherTitleStyle =
                    createStyle(workbook, "D0CECE", true, 11);

            CellStyle summaryStyle =
                    createStyle(workbook, "FF8080", true, 11);

            CellStyle analysisStyle =
                    createStyle(workbook, "FFCC99", true, 11);

            CellStyle designStyle =
                    createStyle(workbook, "DEEBF7", true, 11);

            CellStyle codingStyle =
                    createStyle(workbook, "CCFFCC", true, 11);

            CellStyle sitStyle =
                    createStyle(workbook, "CC99FF", true, 11);

            CellStyle otherActualStyle =
                    createStyle(workbook, "548235", true, 11);

            CellStyle otherPlannedStyle =
                    createStyle(workbook, "FFF2CC", true, 11);

            CellStyle phaseHeaderStyle =
                    createStyle(workbook, "99CCFF", true, 11);

            CellStyle formulaHeaderStyle =
                    createStyle(workbook, "FFFF99", true, 11);

            CellStyle dataStyle =
                    createStyle(workbook, "FFFFFF", false, 11);

            createHeaders(
                    sheet,
                    identityStyle,
                    summaryTitleStyle,
                    subprocessTitleStyle,
                    otherTitleStyle,
                    summaryStyle,
                    analysisStyle,
                    designStyle,
                    codingStyle,
                    sitStyle,
                    otherActualStyle,
                    otherPlannedStyle,
                    phaseHeaderStyle,
                    formulaHeaderStyle
            );

            createDataRows(
                    sheet,
                    metrics,
                    dataStyle
            );

         // Freeze only the 3 header rows.
         // Data rows (Sprint 1, Sprint 2, Total) must scroll normally.
//         sheet.createFreezePane(0, 3, 0, 3);

         sheet.setDisplayGridlines(false);

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    private void createColumnWidths(Sheet sheet) {

        int[] widths = {
                250, 90, 90, 90, 90, 90, 90, 90, 90, 90,
                90, 90, 90, 90, 123, 128, 90, 90, 90, 90,
                90, 108, 90, 90, 90, 90, 90, 156, 90, 90,
                90, 90, 139, 90, 123, 90, 90, 90, 90, 90,
                167, 90, 90, 90, 90, 90, 90, 90, 90, 90,
                90, 90, 159, 90, 90, 149, 147, 131, 90, 90,
                90, 90, 90, 90, 90, 90, 90, 90, 90, 162,
                90, 90, 148, 90, 125, 90, 90, 90, 131, 90,
                90, 90, 90, 90, 90, 90, 90, 90, 90
        };

        for (int i = 0; i < widths.length; i++) {

            int poiWidth =
                    (int) Math.round(
                            widths[i] * 256.0 / 7.0
                    );

            poiWidth =
                    Math.min(
                            poiWidth,
                            255 * 256
                    );

            sheet.setColumnWidth(i, poiWidth);
        }
    }

    private void createHeaders(
            Sheet sheet,
            CellStyle identityStyle,
            CellStyle summaryTitleStyle,
            CellStyle subprocessTitleStyle,
            CellStyle otherTitleStyle,
            CellStyle summaryStyle,
            CellStyle analysisStyle,
            CellStyle designStyle,
            CellStyle codingStyle,
            CellStyle sitStyle,
            CellStyle otherActualStyle,
            CellStyle otherPlannedStyle,
            CellStyle phaseHeaderStyle,
            CellStyle formulaHeaderStyle) {

        Row row1 = sheet.createRow(0);
        Row row2 = sheet.createRow(1);
        Row row3 = sheet.createRow(2);

        row1.setHeightInPoints(25);
        row2.setHeightInPoints(25);
        row3.setHeightInPoints(168);

        /*
         * =====================================================
         * ROW 1
         * =====================================================
         */

        createCell(
                row1,
                0,
                "Project Name",
                identityStyle
        );

        createCell(
                row1,
                1,
                "Release No",
                identityStyle
        );

        createMergedCell(
                sheet,
                row1,
                2,
                15,
                "Summary Project Data & Metrics Values",
                summaryTitleStyle
        );

        createMergedCell(
                sheet,
                row1,
                16,
                72,
                "Sub-process Data & Metrics",
                subprocessTitleStyle
        );

        createMergedCell(
                sheet,
                row1,
                73,
                80,
                "Other Activity",
                otherTitleStyle
        );

        createCell(
                row1,
                81,
                "Average Pre-delivery Defect Density (No. of Defects/UCP)",
                summaryTitleStyle
        );

        createCell(
                row1,
                82,
                "UAT Defects",
                identityStyle
        );

        createCell(
                row1,
                83,
                "Post Delivery Defect Density (No. of Defects/UCP)",
                summaryTitleStyle
        );

        createCell(
                row1,
                84,
                "Overall Defect Density (No. of Defects/UCP)",
                summaryTitleStyle
        );

        createCell(
                row1,
                85,
                "Planned UAT Effort (person-hour)",
                identityStyle
        );

        createCell(
                row1,
                86,
                "Effort spent during UAT (Person-hours)",
                identityStyle
        );

        createCell(
                row1,
                87,
                "Overall Defect Rate (defect/person-hour)",
                summaryTitleStyle
        );

        createCell(
                row1,
                88,
                "Defect removal Efficiency (%)",
                summaryTitleStyle
        );

        merge(
                sheet,
                0, 2,
                0, 0
        );

        merge(
                sheet,
                0, 2,
                1, 1
        );

        merge(
                sheet,
                0, 2,
                81, 81
        );

        merge(
                sheet,
                0, 2,
                82, 82
        );

        merge(
                sheet,
                0, 2,
                83, 83
        );

        merge(
                sheet,
                0, 2,
                84, 84
        );

        merge(
                sheet,
                0, 2,
                85, 85
        );

        merge(
                sheet,
                0, 2,
                86, 86
        );

        merge(
                sheet,
                0, 2,
                87, 87
        );

        merge(
                sheet,
                0, 2,
                88, 88
        );

        /*
         * =====================================================
         * ROW 2
         * =====================================================
         */

        String[] summaryHeaders = {
                "Original Size (Use Case Point)",
                "Actual Size (Use Case Point)",
                "Size Variance (%)",
                "Total Planned Effort Without PM Effort (Person Hours)",
                "Total Planned Effort (Person Hours)",
                "Total Actual Effort Without PM Effort (Person Hours)",
                "Total Actual Effort (Person Hours)",
                "Effort Variance (%)",
                "Planned Duration (Calendar Days)",
                "Actual Duration (Calendar Days)",
                "Schedule Variance (%)",
                "Actual Overall Productivity (UCP / Person hours)",
                "Review Effectiveness\n(Review Defects as % of Review and Test Defects)",
                "Testing Effectiveness\n(Testing Defects as % of Review and Test Defects)"
        };

        for (int i = 0; i < summaryHeaders.length; i++) {

            createCell(
                    row2,
                    2 + i,
                    summaryHeaders[i],
                    summaryStyle
            );

            merge(
                    sheet,
                    1,
                    2,
                    2 + i,
                    2 + i
            );
        }

        /*
         * Analysis
         */

        createMergedCell(
                sheet,
                row2,
                16,
                28,
                "Analysis",
                analysisStyle
        );

        /*
         * Design
         */

        createMergedCell(
                sheet,
                row2,
                29,
                41,
                "Design",
                designStyle
        );

        /*
         * Coding
         */

        createMergedCell(
                sheet,
                row2,
                42,
                57,
                "Coding & Unit Testing",
                codingStyle
        );

        /*
         * SIT
         */

        createMergedCell(
                sheet,
                row2,
                58,
                72,
                "System & Integration Testing (SIT)",
                sitStyle
        );

        /*
         * Other Activity
         */

        createMergedCell(
                sheet,
                row2,
                73,
                76,
                "Other Activity - Actual Effort",
                otherActualStyle
        );

        createMergedCell(
                sheet,
                row2,
                77,
                80,
                "Other Activity - Planned Effort",
                otherPlannedStyle
        );

        String[] analysisHeaders = {
                "Planned Duration (Calendar Days)",
                "Actual Duration (Calendar Days)",
                "Schedule Variance (%)",
                "Planned Effort including Review(Person Hours)",
                "Actual Total Effort (Person Hours)",
                "Analysis_Productivity UCP/Effort",
                "Effort Varaince (%)",
                "Effort In Analysis (and Rework) person-hour",
                "Analysis Review Defects (No.)",
                "Analysis Review Effort (Person Hours)",
                "Defect Density - Analysis",
                "Defect Detection Rate - Analysis (No. of Defects in Review of Analysis /Review Person-Hrs)",
                "Defect Rate - Analysis (No of defects/ Effort in Analysis)"
        };

        String[] designHeaders = {
                "Planned Duration (Calendar Days)",
                "Actual Duration (Calendar Days)",
                "Schedule Variance (%)",
                "Planned Effort including Review(Person Hours)",
                "Actual Total Effort (Person Hours)",
                "Design Productivity UCP/Effort",
                "Effort Varaince (%)",
                "Effort in Design (and Rework) person-hour",
                "Design Review Defects (No.)",
                "Design Review Effort (Person Hours)",
                "Defect Density - Design",
                "Defect Detection Rate - Design (No. of Defects found in Design Review / Person-Hrs)",
                "Defect Rate - Design (No of defects/ Effort in Design)"
        };

        String[] codingHeaders = {
                "Planned Duration (Calendar Days)",
                "Actual Duration (Calendar Days)",
                "Schedule Variance (%)",
                "Planned Effort incl Review & Testing (Person Hours)",
                "Actual Total Effort (Person Hours)",
                "Effort Varaince (%)",
                "Effort in Coding (and Rework) person-hour",
                "Code Review Defects (No.)",
                "Code Review Effort (Person Hours)",
                "Defect Density - Coding",
                "Defect Detection Rate - Code Review (No. of Defects found in Code Review / Person-Hrs)",
                "Unit Testing Defects (No.)",
                "Unit Testing Effort (Person Hours)",
                "Defect Detection Rate - Unit Testing (No. of Defects found in Unit Testing / Person-Hrs)",
                "Defect Rate -Coding (No. of Defects found in Unit Testing+Code Review /Person-Hrs Coding)",
                "Coding Productivity"
        };

        String[] sitHeaders = {
                "Planned Duration (Calendar Days)",
                "Actual Duration (Calendar Days)",
                "Schedule Variance (%)",
                "Planned Effort incl. Review & Testing(Person Hours)",
                "Actual Total Effort (Person Hours)",
                "Effort Varaince (%)",
                "Total Number of Test Condition (No)",
                "Effort in Writing Test Case incl Test Planning (person-hour)",
                "SIT Test Case Review Defects (No.)",
                "SIT Test Case Review Effort (Person Hours)",
                "Effort in Test Execution (person-hour)",
                "Defect Detection Rate - SIT Test Case Review (No. of Defects found in Test Case Review / Person-Hrs)",
                "SIT Defects (No.)",
                "SIT Effort (Person Hours)",
                "Defect Detection Rate - SIT (No. of Testing Defects found in SIT / Person-Hrs)"
        };

        String[] otherHeaders = {
                "Total Of Other Activity (person-hour)",
                "Project Management, Monitoring & Control (person-hour)",
                "Support Group Activity (person-hour)",
                "Others (person-hour)",
                "Total Of Other Activity (person-hour)",
                "Project Management, Monitoring & Control (person-hour)",
                "Support Group Activity (person-hour)",
                "Others (person-hour)"
        };

        writeHeaderArray(
                row3,
                16,
                analysisHeaders,
                phaseHeaderStyle,
                formulaHeaderStyle,
                new boolean[] {
                        false, false, true, false, false, true, true,
                        false, false, false, true, true, true
                }
        );

        writeHeaderArray(
                row3,
                29,
                designHeaders,
                phaseHeaderStyle,
                formulaHeaderStyle,
                new boolean[] {
                        false, false, true, false, true, true, true,
                        false, false, false, true, true, true
                }
        );

        writeHeaderArray(
                row3,
                42,
                codingHeaders,
                phaseHeaderStyle,
                formulaHeaderStyle,
                new boolean[] {
                        false, false, true, false, false, true, false,
                        false, false, true, true, false, false, true, true, true
                }
        );

        writeHeaderArray(
                row3,
                58,
                sitHeaders,
                phaseHeaderStyle,
                formulaHeaderStyle,
                new boolean[] {
                        false, false, true, false, false, true, false,
                        false, false, false, false, true, false, false, true
                }
        );

        writeHeaderArray(
                row3,
                73,
                otherHeaders,
                phaseHeaderStyle,
                formulaHeaderStyle,
                new boolean[] {
                        true, false, false, false,
                        true, false, false, false
                }
        );
    }

    private void createDataRows(
        Sheet sheet,
        ProjectMetrics metrics,
        CellStyle dataStyle) {

    List<ProjectMetricsSprint> sprints =
            metrics.getSprints();

    int rowIndex = 3;

    // =====================================================
    // SPRINT ROWS
    // =====================================================

    if (sprints != null) {

        for (ProjectMetricsSprint sprint : sprints) {

            Row row = sheet.createRow(rowIndex++);

            row.setHeightInPoints(24);

            int column = 0;

            // =====================================================
            // SUMMARY
            // =====================================================

            setString(
                    row,
                    column++,
                    metrics.getProjectName(),
                    dataStyle
            );

            setString(
                    row,
                    column++,
                    sprint.getTaskName(),
                    dataStyle
            );

            setNumber(row, column++, metrics.getOriginalSize(), dataStyle);
            setNumber(row, column++, metrics.getActualSize(), dataStyle);
            setNumber(row, column++, metrics.getSizeVariance(), dataStyle);

            setNumber(
                    row,
                    column++,
                    metrics.getTotalPlannedEffortWithoutPm(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getTotalPlannedEffort(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getTotalActualEffortWithoutPm(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getTotalActualEffort(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getEffortVariance(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getPlannedDuration(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getActualDuration(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getScheduleVariance(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getActualOverallProductivity(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getReviewEffectiveness(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getTestingEffectiveness(),
                    dataStyle
            );

            // =====================================================
            // ANALYSIS
            // =====================================================

            setNumber(row, column++, sprint.getAnalysisPlannedDuration(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisActualDuration(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisScheduleVariance(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisPlannedEffort(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisActualEffort(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisProductivity(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisEffortVariance(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisEffortInAnalysis(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisReviewDefects(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisReviewEffort(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisDefectDensity(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisDefectDetectionRate(), dataStyle);
            setNumber(row, column++, sprint.getAnalysisDefectRate(), dataStyle);

            // =====================================================
            // DESIGN
            // =====================================================

            setNumber(row, column++, sprint.getDesignPlannedDuration(), dataStyle);
            setNumber(row, column++, sprint.getDesignActualDuration(), dataStyle);
            setNumber(row, column++, sprint.getDesignScheduleVariance(), dataStyle);
            setNumber(row, column++, sprint.getDesignPlannedEffort(), dataStyle);
            setNumber(row, column++, sprint.getDesignActualEffort(), dataStyle);
            setNumber(row, column++, sprint.getDesignProductivity(), dataStyle);
            setNumber(row, column++, sprint.getDesignEffortVariance(), dataStyle);
            setNumber(row, column++, sprint.getDesignEffortInAnalysis(), dataStyle);
            setNumber(row, column++, sprint.getDesignReviewDefects(), dataStyle);
            setNumber(row, column++, sprint.getDesignReviewEffort(), dataStyle);
            setNumber(row, column++, sprint.getDesignDefectDensity(), dataStyle);
            setNumber(row, column++, sprint.getDesignDefectDetectionRate(), dataStyle);
            setNumber(row, column++, sprint.getDesignDefectRate(), dataStyle);

            // =====================================================
            // CODING
            // =====================================================

            setNumber(row, column++, sprint.getCodingPlannedDuration(), dataStyle);
            setNumber(row, column++, sprint.getCodingActualDuration(), dataStyle);
            setNumber(row, column++, sprint.getCodingScheduleVariance(), dataStyle);
            setNumber(row, column++, sprint.getCodingPlannedEffort(), dataStyle);
            setNumber(row, column++, sprint.getCodingActualEffort(), dataStyle);
            setNumber(row, column++, sprint.getCodingEffortVariance(), dataStyle);
            setNumber(row, column++, sprint.getCodingEffort(), dataStyle);
            setNumber(row, column++, sprint.getCodeReviewDefects(), dataStyle);
            setNumber(row, column++, sprint.getCodeReviewEffort(), dataStyle);
            setNumber(row, column++, sprint.getCodingDefectDensity(), dataStyle);
            setNumber(row, column++, sprint.getCodeReviewDetectionRate(), dataStyle);
            setNumber(row, column++, sprint.getUnitTestingDefects(), dataStyle);
            setNumber(row, column++, sprint.getUnitTestingEffort(), dataStyle);
            setNumber(row, column++, sprint.getUnitTestingDetectionRate(), dataStyle);
            setNumber(row, column++, sprint.getCodingDefectRate(), dataStyle);
            setNumber(row, column++, sprint.getCodingProductivity(), dataStyle);

            // =====================================================
            // SIT
            // =====================================================

            setNumber(row, column++, sprint.getSitPlannedDuration(), dataStyle);
            setNumber(row, column++, sprint.getSitActualDuration(), dataStyle);
            setNumber(row, column++, sprint.getSitScheduleVariance(), dataStyle);
            setNumber(row, column++, sprint.getSitPlannedEffort(), dataStyle);
            setNumber(row, column++, sprint.getSitActualEffort(), dataStyle);
            setNumber(row, column++, sprint.getSitEffortVariance(), dataStyle);
            setNumber(row, column++, sprint.getTotalTestConditions(), dataStyle);
            setNumber(row, column++, sprint.getTestCaseWritingEffort(), dataStyle);
            setNumber(row, column++, sprint.getTestCaseReviewDefects(), dataStyle);
            setNumber(row, column++, sprint.getTestCaseReviewEffort(), dataStyle);
            setNumber(row, column++, sprint.getTestExecutionEffort(), dataStyle);
            setNumber(row, column++, sprint.getTestCaseReviewDetectionRate(), dataStyle);
            setNumber(row, column++, sprint.getSitDefects(), dataStyle);
            setNumber(row, column++, sprint.getSitEffort(), dataStyle);
            setNumber(row, column++, sprint.getSitDetectionRate(), dataStyle);

            // =====================================================
            // OTHER ACTIVITY
            // =====================================================

            setNumber(row, column++, sprint.getOtherActualTotal(), dataStyle);
            setNumber(row, column++, sprint.getOtherActualProjectManagement(), dataStyle);
            setNumber(row, column++, sprint.getOtherActualSupportGroup(), dataStyle);
            setNumber(row, column++, sprint.getOtherActualOthers(), dataStyle);

            setNumber(row, column++, sprint.getOtherPlannedTotal(), dataStyle);
            setNumber(row, column++, sprint.getOtherPlannedProjectManagement(), dataStyle);
            setNumber(row, column++, sprint.getOtherPlannedSupportGroup(), dataStyle);
            setNumber(row, column++, sprint.getOtherPlannedOthers(), dataStyle);

            // =====================================================
            // QUALITY / UAT
            // =====================================================

            setNumber(
                    row,
                    column++,
                    metrics.getAveragePreDeliveryDefectDensity(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getUatDefects(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getPostDeliveryDefectDensity(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getOverallDefectDensity(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getPlannedUatEffort(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getActualUatEffort(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getOverallDefectRate(),
                    dataStyle
            );

            setNumber(
                    row,
                    column++,
                    metrics.getDefectRemovalEfficiency(),
                    dataStyle
            );
        }
    }

    // =====================================================
    // TOTAL / CUMULATIVE ROW
    // =====================================================

    createTotalRow(
            sheet,
            metrics,
            dataStyle,
            rowIndex
    );
}
    
    private void createTotalRow(
            Sheet sheet,
            ProjectMetrics metrics,
            CellStyle dataStyle,
            int rowIndex) {

        Row row = sheet.createRow(rowIndex);

        row.setHeightInPoints(28);

        int column = 0;

        // =====================================================
        // SUMMARY
        // =====================================================

        setString(
                row,
                column++,
                metrics.getProjectName(),
                dataStyle
        );

        setString(
                row,
                column++,
                "Total",
                dataStyle
        );

        setNumber(row, column++, metrics.getOriginalSize(), dataStyle);
        setNumber(row, column++, metrics.getActualSize(), dataStyle);
        setNumber(row, column++, metrics.getSizeVariance(), dataStyle);

        setNumber(
                row,
                column++,
                metrics.getTotalPlannedEffortWithoutPm(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getTotalPlannedEffort(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getTotalActualEffortWithoutPm(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getTotalActualEffort(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getEffortVariance(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getPlannedDuration(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getActualDuration(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getScheduleVariance(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getActualOverallProductivity(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getReviewEffectiveness(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getTestingEffectiveness(),
                dataStyle
        );

        // =====================================================
        // CUMULATIVE ANALYSIS
        // =====================================================

        setNumber(row, column++, metrics.getAnalysisPlannedDuration(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisActualDuration(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisScheduleVariance(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisPlannedEffort(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisActualEffort(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisProductivity(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisEffortVariance(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisEffortInAnalysis(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisReviewDefects(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisReviewEffort(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisDefectDensity(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisDefectDetectionRate(), dataStyle);
        setNumber(row, column++, metrics.getAnalysisDefectRate(), dataStyle);

        // =====================================================
        // CUMULATIVE DESIGN
        // =====================================================

        setNumber(row, column++, metrics.getDesignPlannedDuration(), dataStyle);
        setNumber(row, column++, metrics.getDesignActualDuration(), dataStyle);
        setNumber(row, column++, metrics.getDesignScheduleVariance(), dataStyle);
        setNumber(row, column++, metrics.getDesignPlannedEffort(), dataStyle);
        setNumber(row, column++, metrics.getDesignActualEffort(), dataStyle);
        setNumber(row, column++, metrics.getDesignProductivity(), dataStyle);
        setNumber(row, column++, metrics.getDesignEffortVariance(), dataStyle);
        setNumber(row, column++, metrics.getDesignEffortInAnalysis(), dataStyle);
        setNumber(row, column++, metrics.getDesignReviewDefects(), dataStyle);
        setNumber(row, column++, metrics.getDesignReviewEffort(), dataStyle);
        setNumber(row, column++, metrics.getDesignDefectDensity(), dataStyle);
        setNumber(row, column++, metrics.getDesignDefectDetectionRate(), dataStyle);
        setNumber(row, column++, metrics.getDesignDefectRate(), dataStyle);

        // =====================================================
        // CUMULATIVE CODING
        // =====================================================

        setNumber(row, column++, metrics.getCodingPlannedDuration(), dataStyle);
        setNumber(row, column++, metrics.getCodingActualDuration(), dataStyle);
        setNumber(row, column++, metrics.getCodingScheduleVariance(), dataStyle);
        setNumber(row, column++, metrics.getCodingPlannedEffort(), dataStyle);
        setNumber(row, column++, metrics.getCodingActualEffort(), dataStyle);
        setNumber(row, column++, metrics.getCodingEffortVariance(), dataStyle);
        setNumber(row, column++, metrics.getCodingEffort(), dataStyle);
        setNumber(row, column++, metrics.getCodeReviewDefects(), dataStyle);
        setNumber(row, column++, metrics.getCodeReviewEffort(), dataStyle);
        setNumber(row, column++, metrics.getCodingDefectDensity(), dataStyle);
        setNumber(row, column++, metrics.getCodeReviewDetectionRate(), dataStyle);
        setNumber(row, column++, metrics.getUnitTestingDefects(), dataStyle);
        setNumber(row, column++, metrics.getUnitTestingEffort(), dataStyle);
        setNumber(row, column++, metrics.getUnitTestingDetectionRate(), dataStyle);
        setNumber(row, column++, metrics.getCodingDefectRate(), dataStyle);
        setNumber(row, column++, metrics.getCodingProductivity(), dataStyle);

        // =====================================================
        // CUMULATIVE SIT
        // =====================================================

        setNumber(row, column++, metrics.getSitPlannedDuration(), dataStyle);
        setNumber(row, column++, metrics.getSitActualDuration(), dataStyle);
        setNumber(row, column++, metrics.getSitScheduleVariance(), dataStyle);
        setNumber(row, column++, metrics.getSitPlannedEffort(), dataStyle);
        setNumber(row, column++, metrics.getSitActualEffort(), dataStyle);
        setNumber(row, column++, metrics.getSitEffortVariance(), dataStyle);
        setNumber(row, column++, metrics.getTotalTestConditions(), dataStyle);
        setNumber(row, column++, metrics.getTestCaseWritingEffort(), dataStyle);
        setNumber(row, column++, metrics.getTestCaseReviewDefects(), dataStyle);
        setNumber(row, column++, metrics.getTestCaseReviewEffort(), dataStyle);
        setNumber(row, column++, metrics.getTestExecutionEffort(), dataStyle);
        setNumber(row, column++, metrics.getTestCaseReviewDetectionRate(), dataStyle);
        setNumber(row, column++, metrics.getSitDefects(), dataStyle);
        setNumber(row, column++, metrics.getSitEffort(), dataStyle);
        setNumber(row, column++, metrics.getSitDetectionRate(), dataStyle);

        // =====================================================
        // CUMULATIVE OTHER ACTIVITY
        // =====================================================

        setNumber(row, column++, metrics.getOtherActualTotal(), dataStyle);
        setNumber(row, column++, metrics.getOtherActualProjectManagement(), dataStyle);
        setNumber(row, column++, metrics.getOtherActualSupportGroup(), dataStyle);
        setNumber(row, column++, metrics.getOtherActualOthers(), dataStyle);

        setNumber(row, column++, metrics.getOtherPlannedTotal(), dataStyle);
        setNumber(row, column++, metrics.getOtherPlannedProjectManagement(), dataStyle);
        setNumber(row, column++, metrics.getOtherPlannedSupportGroup(), dataStyle);
        setNumber(row, column++, metrics.getOtherPlannedOthers(), dataStyle);

        // =====================================================
        // QUALITY / UAT
        // =====================================================

        setNumber(
                row,
                column++,
                metrics.getAveragePreDeliveryDefectDensity(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getUatDefects(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getPostDeliveryDefectDensity(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getOverallDefectDensity(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getPlannedUatEffort(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getActualUatEffort(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getOverallDefectRate(),
                dataStyle
        );

        setNumber(
                row,
                column++,
                metrics.getDefectRemovalEfficiency(),
                dataStyle
        );
    }

    private CellStyle createTotalStyle(
            XSSFWorkbook workbook) {

        CellStyle style =
                createStyle(
                        workbook,
                        "FFFF99",
                        true,
                        11
                );

        return style;
    }
    
    
    private void writeHeaderArray(
            Row row,
            int startColumn,
            String[] headers,
            CellStyle phaseStyle,
            CellStyle formulaStyle,
            boolean[] formulaColumns) {

        for (int i = 0; i < headers.length; i++) {

            createCell(
                    row,
                    startColumn + i,
                    headers[i],
                    formulaColumns[i]
                            ? formulaStyle
                            : phaseStyle
            );
        }
    }

    private CellStyle createStyle(
            XSSFWorkbook workbook,
            String hexColor,
            boolean bold,
            int fontSize) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFillForegroundColor(
                new XSSFColor(
                        java.awt.Color.decode("#" + hexColor),
                        null
                )
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

        Font font =
                workbook.createFont();

        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) fontSize);
        font.setBold(bold);
        font.setColor(IndexedColors.BLACK.getIndex());

        style.setFont(font);

        return style;
    }

    private short hexToIndexedColor(
            XSSFWorkbook workbook,
            String hex) {

        /*
         * XSSF supports RGB colors through XSSFColor,
         * but using indexed colors here would lose the
         * exact UI colors. Therefore the actual
         * implementation below uses XSSFColor.
         */

        return IndexedColors.WHITE.getIndex();
    }

    private void createCell(
            Row row,
            int column,
            String value,
            CellStyle style) {

        Cell cell =
                row.createCell(column);

        cell.setCellValue(
                value == null ? "" : value
        );

        cell.setCellStyle(style);
    }

    private void createMergedCell(
            Sheet sheet,
            Row row,
            int startColumn,
            int endColumn,
            String value,
            CellStyle style) {

        createCell(
                row,
                startColumn,
                value,
                style
        );

        for (int column = startColumn + 1;
             column <= endColumn;
             column++) {

            createCell(
                    row,
                    column,
                    "",
                    style
            );
        }

        merge(
                sheet,
                row.getRowNum(),
                row.getRowNum(),
                startColumn,
                endColumn
        );
    }

    private void merge(
            Sheet sheet,
            int firstRow,
            int lastRow,
            int firstColumn,
            int lastColumn) {

    	CellRangeAddress region =
    	        new CellRangeAddress(
                        firstRow,
                        lastRow,
                        firstColumn,
                        lastColumn
                );
        
        sheet.addMergedRegion(region);

        applyBorderToMergedRegion(sheet, region);
    }

    private void setString(
            Row row,
            int column,
            String value,
            CellStyle style) {

        Cell cell =
                row.createCell(column);

        cell.setCellStyle(style);

        cell.setCellValue(
                value == null ? "" : value
        );
    }

    private void setNumber(
            Row row,
            int column,
            Number value,
            CellStyle style) {

        Cell cell =
                row.createCell(column);

        cell.setCellStyle(style);

        if (value == null) {
            cell.setCellValue("");
        } else {
            cell.setCellValue(
                    value.doubleValue()
            );
        }
    }
    
    private void applyBorderToMergedRegion(
            Sheet sheet,
            CellRangeAddress region) {

        RegionUtil.setBorderTop(
                BorderStyle.THIN,
                region,
                sheet
        );

        RegionUtil.setBorderBottom(
                BorderStyle.THIN,
                region,
                sheet
        );

        RegionUtil.setBorderLeft(
                BorderStyle.THIN,
                region,
                sheet
        );

        RegionUtil.setBorderRight(
                BorderStyle.THIN,
                region,
                sheet
        );
    }
}