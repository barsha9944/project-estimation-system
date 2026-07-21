package com.projectestimation.backend.projectschedule.excel;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Objects;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisOrientation;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.BarGrouping;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTBarSer;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleTaskRequest;
import com.projectestimation.backend.projectschedule.util.ExcelChartDateAxisFixer;

@Component
public class ProjectScheduleExcelExporter {

    public byte[] export(SaveProjectScheduleRequest request) {

        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {

        	createProjectScheduleSheet(workbook, request);

        	createHiddenGanttDataSheet(workbook, request);

        	createGanttChartSheet(workbook);

        	workbook.write(outputStream);

        	// Get workbook bytes
        	byte[] workbookBytes = outputStream.toByteArray();

        	// Fix chart XML
        	return ExcelChartDateAxisFixer.fixDateAxis(workbookBytes);

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
   private void createHiddenGanttDataSheet(
        XSSFWorkbook workbook,
        SaveProjectScheduleRequest request) {

    Sheet sheet = workbook.createSheet("Gantt Data");

    // Hide the sheet
    workbook.setSheetHidden(workbook.getSheetIndex(sheet), true);

    Row header = sheet.createRow(0);

    header.createCell(0).setCellValue("Task");
    header.createCell(1).setCellValue("Offset");
    header.createCell(2).setCellValue("Duration");

    if (request.getTasks() == null || request.getTasks().isEmpty()) {
        return;
    }

    LocalDate projectStart = request.getTasks()
            .stream()
            .map(SaveProjectScheduleTaskRequest::getPlannedStartDate)
            .filter(Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());

    int rowIndex = 1;

    for (SaveProjectScheduleTaskRequest task : request.getTasks()) {

        if (task.getPlannedStartDate() == null ||
                task.getPlannedEndDate() == null) {
            continue;
        }

        Row row = sheet.createRow(rowIndex++);

        row.createCell(0).setCellValue(task.getTaskName());

        double excelStartDate =
                DateUtil.getExcelDate(task.getPlannedStartDate());

        double excelEndDate =
                DateUtil.getExcelDate(task.getPlannedEndDate());

        row.createCell(1).setCellValue(excelStartDate);

        row.createCell(2).setCellValue(
                excelEndDate - excelStartDate + 1);
    }

    sheet.autoSizeColumn(0);
}
   
   private void createGanttChartSheet(XSSFWorkbook workbook) {

    XSSFSheet chartSheet = workbook.createSheet("Gantt Chart");

    XSSFSheet dataSheet =
            workbook.getSheet("Gantt Data");

    XSSFDrawing drawing =
            chartSheet.createDrawingPatriarch();

    XSSFClientAnchor anchor =
            drawing.createAnchor(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    18,
                    18);

    XSSFChart chart =
            drawing.createChart(anchor);

    chart.setTitleText("Project Schedule");
    chart.setTitleOverlay(false);

    chart.deleteLegend();

    XDDFCategoryAxis categoryAxis =
            chart.createCategoryAxis(AxisPosition.LEFT);
    
    categoryAxis.setOrientation(AxisOrientation.MAX_MIN);

    XDDFValueAxis valueAxis =
            chart.createValueAxis(AxisPosition.BOTTOM);

    valueAxis.setCrosses(AxisCrosses.MIN);

    // Try to display Excel serial numbers as dates
    valueAxis.setNumberFormat("dd-MMM");
    
    double minDate =
            DateUtil.getExcelDate(
                    dataSheet.getRow(1)
                            .getCell(1)
                            .getLocalDateTimeCellValue()
                            .toLocalDate());

    valueAxis.setMinimum(minDate);

    int lastRow =
            dataSheet.getLastRowNum();

    XDDFDataSource<String> tasks =
            XDDFDataSourcesFactory.fromStringCellRange(
                    dataSheet,
                    new CellRangeAddress(
                            1,
                            lastRow,
                            0,
                            0));

    XDDFNumericalDataSource<Double> offsets =
            XDDFDataSourcesFactory.fromNumericCellRange(
                    dataSheet,
                    new CellRangeAddress(
                            1,
                            lastRow,
                            1,
                            1));

    XDDFNumericalDataSource<Double> durations =
            XDDFDataSourcesFactory.fromNumericCellRange(
                    dataSheet,
                    new CellRangeAddress(
                            1,
                            lastRow,
                            2,
                            2));

    XDDFBarChartData chartData =
            (XDDFBarChartData) chart.createData(
                    ChartTypes.BAR,
                    categoryAxis,
                    valueAxis);

    chartData.setBarDirection(
            BarDirection.BAR);
    chartData.setBarGrouping(BarGrouping.STACKED);
    
    chartData.setGapWidth(30);

    chartData.setVaryColors(false);

    XDDFChartData.Series offsetSeries =
            chartData.addSeries(tasks, offsets);

    offsetSeries.setTitle("Offset", null);

    XDDFChartData.Series durationSeries =
            chartData.addSeries(tasks, durations);

    durationSeries.setTitle("Duration", null);

    chart.plot(chartData);
    
    CTChart ctChart = chart.getCTChart();

    CTPlotArea plotArea = ctChart.getPlotArea();

    CTValAx valAx = plotArea.getValAxArray(0);

    if (!valAx.isSetNumFmt()) {
        valAx.addNewNumFmt();
    }

    valAx.getNumFmt().setFormatCode("dd-MMM");
    valAx.getNumFmt().setSourceLinked(false);

    CTBarChart barChart = plotArea.getBarChartArray(0);

    CTBarSer offsetBar =
            barChart.getSerArray(0);

    CTShapeProperties shape =
            offsetBar.isSetSpPr()
            ? offsetBar.getSpPr()
            : offsetBar.addNewSpPr();

    shape.addNewNoFill();
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