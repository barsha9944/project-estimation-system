package com.projectestimation.backend.projectmetrics.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.projectmetrics.dto.AnalysisMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.CodingMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.DesignMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.OtherActivityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.QualityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SitMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SprintMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SummaryMetricsResponse;

@Component
public class ProjectMetricsExcelParser {

    private static final Logger log = LoggerFactory.getLogger(ProjectMetricsExcelParser.class);
    private final DataFormatter dataFormatter = new DataFormatter();

    public ProjectMetricsResponse parse(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is);
        }
    }

    public ProjectMetricsResponse parse(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            return parse(is);
        }
    }

    public ProjectMetricsResponse parse(InputStream is) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = findMetricsSheet(workbook);
            if (sheet == null) {
                log.warn("Summary Project Metrics sheet not found");
                return new ProjectMetricsResponse();
            }

            ProjectMetricsResponse response = new ProjectMetricsResponse();
            response.setSprints(new ArrayList<>());

            int lastRow = sheet.getLastRowNum();
            for (int r = 3; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                String projectName = getString(row, 0);
                String taskOrRelease = getString(row, 1);

                if (taskOrRelease.equalsIgnoreCase("Total")) {
                    // Cumulative row
                    SummaryMetricsResponse summary = parseSummary(row);
                    summary.setProjectName(projectName);
                    summary.setReleaseNo(taskOrRelease);
                    response.setSummary(summary);

                    response.setAnalysis(parseAnalysis(row));
                    response.setDesign(parseDesign(row));
                    response.setCoding(parseCoding(row));
                    response.setSit(parseSit(row));
                    response.setOtherActivity(parseOtherActivity(row));
                    response.setQuality(parseQuality(row));
                } else {
                    // Sprint row
                    SprintMetricsResponse sprint = new SprintMetricsResponse();
                    sprint.setTaskName(taskOrRelease);
                    sprint.setSprintNumber(extractSprintNumber(taskOrRelease, response.getSprints().size() + 1));

                    SummaryMetricsResponse summary = parseSummary(row);
                    summary.setProjectName(projectName);
                    summary.setReleaseNo(taskOrRelease);
                    sprint.setSummary(summary);

                    sprint.setAnalysis(parseAnalysis(row));
                    sprint.setDesign(parseDesign(row));
                    sprint.setCoding(parseCoding(row));
                    sprint.setSit(parseSit(row));
                    sprint.setOtherActivity(parseOtherActivity(row));
                    sprint.setQuality(parseQuality(row));

                    response.getSprints().add(sprint);
                }
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to parse Project Metrics Excel: {}", e.getMessage(), e);
            throw new IOException("Failed to parse Project Metrics Excel", e);
        }
    }

    private Sheet findMetricsSheet(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String name = workbook.getSheetName(i);
            if (name != null && name.trim().equalsIgnoreCase("Summary Project Metrics")) {
                return workbook.getSheetAt(i);
            }
        }
        return workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
    }

    private SummaryMetricsResponse parseSummary(Row row) {
        SummaryMetricsResponse s = new SummaryMetricsResponse();
        s.setOriginalSize(getDouble(row, 2));
        s.setActualSize(getDouble(row, 3));
        s.setSizeVariance(getDouble(row, 4));
        s.setTotalPlannedEffortWithoutPm(getDouble(row, 5));
        s.setTotalPlannedEffort(getDouble(row, 6));
        s.setTotalActualEffortWithoutPm(getDouble(row, 7));
        s.setTotalActualEffort(getDouble(row, 8));
        s.setEffortVariance(getDouble(row, 9));
        s.setPlannedDuration(getInt(row, 10));
        s.setActualDuration(getInt(row, 11));
        s.setScheduleVariance(getDouble(row, 12));
        s.setActualOverallProductivity(getDouble(row, 13));
        s.setReviewEffectiveness(getDouble(row, 14));
        s.setTestingEffectiveness(getDouble(row, 15));
        return s;
    }

    private AnalysisMetricsResponse parseAnalysis(Row row) {
        AnalysisMetricsResponse a = new AnalysisMetricsResponse();
        a.setPlannedDuration(getInt(row, 16));
        a.setActualDuration(getInt(row, 17));
        a.setScheduleVariance(getDouble(row, 18));
        a.setPlannedEffort(getDouble(row, 19));
        a.setActualEffort(getDouble(row, 20));
        a.setProductivity(getDouble(row, 21));
        a.setEffortVariance(getDouble(row, 22));
        a.setEffortInAnalysis(getDouble(row, 23));
        a.setReviewDefects(getInt(row, 24));
        a.setReviewEffort(getDouble(row, 25));
        a.setDefectDensity(getDouble(row, 26));
        a.setDefectDetectionRate(getDouble(row, 27));
        a.setDefectRate(getDouble(row, 28));
        return a;
    }

    private DesignMetricsResponse parseDesign(Row row) {
        DesignMetricsResponse d = new DesignMetricsResponse();
        d.setPlannedDuration(getInt(row, 29));
        d.setActualDuration(getInt(row, 30));
        d.setScheduleVariance(getDouble(row, 31));
        d.setPlannedEffort(getDouble(row, 32));
        d.setActualEffort(getDouble(row, 33));
        d.setProductivity(getDouble(row, 34));
        d.setEffortVariance(getDouble(row, 35));
        d.setEffortInAnalysis(getDouble(row, 36));
        d.setReviewDefects(getInt(row, 37));
        d.setReviewEffort(getDouble(row, 38));
        d.setDefectDensity(getDouble(row, 39));
        d.setDefectDetectionRate(getDouble(row, 40));
        d.setDefectRate(getDouble(row, 41));
        return d;
    }

    private CodingMetricsResponse parseCoding(Row row) {
        CodingMetricsResponse c = new CodingMetricsResponse();
        c.setPlannedDuration(getInt(row, 42));
        c.setActualDuration(getInt(row, 43));
        c.setScheduleVariance(getDouble(row, 44));
        c.setPlannedEffort(getDouble(row, 45));
        c.setActualEffort(getDouble(row, 46));
        c.setEffortVariance(getDouble(row, 47));
        c.setCodingEffort(getDouble(row, 48));
        c.setCodeReviewDefects(getInt(row, 49));
        c.setCodeReviewEffort(getDouble(row, 50));
        c.setDefectDensity(getDouble(row, 51));
        c.setCodeReviewDetectionRate(getDouble(row, 52));
        c.setUnitTestingDefects(getInt(row, 53));
        c.setUnitTestingEffort(getDouble(row, 54));
        c.setUnitTestingDetectionRate(getDouble(row, 55));
        c.setDefectRate(getDouble(row, 56));
        c.setProductivity(getDouble(row, 57));
        return c;
    }

    private SitMetricsResponse parseSit(Row row) {
        SitMetricsResponse sit = new SitMetricsResponse();
        sit.setPlannedDuration(getInt(row, 58));
        sit.setActualDuration(getInt(row, 59));
        sit.setScheduleVariance(getDouble(row, 60));
        sit.setPlannedEffort(getDouble(row, 61));
        sit.setActualEffort(getDouble(row, 62));
        sit.setEffortVariance(getDouble(row, 63));
        sit.setTotalTestConditions(getInt(row, 64));
        sit.setTestCaseWritingEffort(getDouble(row, 65));
        sit.setTestCaseReviewDefects(getInt(row, 66));
        sit.setTestCaseReviewEffort(getDouble(row, 67));
        sit.setTestExecutionEffort(getDouble(row, 68));
        sit.setTestCaseReviewDetectionRate(getDouble(row, 69));
        sit.setSitDefects(getInt(row, 70));
        sit.setSitEffort(getDouble(row, 71));
        sit.setSitDetectionRate(getDouble(row, 72));
        return sit;
    }

    private OtherActivityMetricsResponse parseOtherActivity(Row row) {
        OtherActivityMetricsResponse o = new OtherActivityMetricsResponse();
        o.setActualTotal(getDouble(row, 73));
        o.setActualProjectManagement(getDouble(row, 74));
        o.setActualSupportGroup(getDouble(row, 75));
        o.setActualOthers(getDouble(row, 76));
        o.setPlannedTotal(getDouble(row, 77));
        o.setPlannedProjectManagement(getDouble(row, 78));
        o.setPlannedSupportGroup(getDouble(row, 79));
        o.setPlannedOthers(getDouble(row, 80));
        return o;
    }

    private QualityMetricsResponse parseQuality(Row row) {
        QualityMetricsResponse q = new QualityMetricsResponse();
        q.setAveragePreDeliveryDefectDensity(getDouble(row, 81));
        q.setUatDefects(getInt(row, 82));
        q.setPostDeliveryDefectDensity(getDouble(row, 83));
        q.setOverallDefectDensity(getDouble(row, 84));
        q.setPlannedUatEffort(getDouble(row, 85));
        q.setActualUatEffort(getDouble(row, 86));
        q.setOverallDefectRate(getDouble(row, 87));
        q.setDefectRemovalEfficiency(getDouble(row, 88));
        return q;
    }

    private Integer extractSprintNumber(String taskName, int fallback) {
        if (taskName == null || taskName.isBlank()) return fallback;
        String digits = taskName.replaceAll("\\D+", "");
        if (!digits.isEmpty()) {
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return dataFormatter.formatCellValue(cell).trim();
    }

    private Double getDouble(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        String str = dataFormatter.formatCellValue(cell).trim();
        if (str.isEmpty()) return null;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getInt(Row row, int col) {
        Double d = getDouble(row, col);
        return d != null ? d.intValue() : null;
    }

    private boolean isRowEmpty(Row row) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = dataFormatter.formatCellValue(cell).trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }
}
