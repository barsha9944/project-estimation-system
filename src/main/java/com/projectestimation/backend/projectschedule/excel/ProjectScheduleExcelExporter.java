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
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDPt;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.common.exception.ProjectScheduleFailedException;
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

            throw new ProjectScheduleFailedException(
                    "Failed to generate project schedule excel.",
                    ex
            );

        }

    }

private void createProjectScheduleSheet(
        XSSFWorkbook workbook,
        SaveProjectScheduleRequest request) {

    Sheet sheet = workbook.createSheet("Project Schedule");

    CellStyle headerStyle = createTableHeaderStyle(workbook);
    CellStyle cellStyle = createTableCellStyle(workbook);
    CellStyle dateStyle = createDateCellStyle(workbook);

    String[] columns = {
            "Seq",
            "Task",
            "Task Breakdown",
            "Planned Start",
            "Planned End",
            "Working Days",
            "Actual Start",
            "Actual End",
            "Actual Working Days",
            "Predecessor",
            "Status"
    };

    int rowIndex = 0;

    Row header = sheet.createRow(rowIndex++);

    for (int i = 0; i < columns.length; i++) {

        Cell cell = header.createCell(i);

        cell.setCellValue(columns[i]);

        cell.setCellStyle(headerStyle);
    }

    if (request.getTasks() == null || request.getTasks().isEmpty()) {
        return;
    }

    for (SaveProjectScheduleTaskRequest task : request.getTasks()) {

        /*
         * If the task has breakdowns, export one Excel row
         * for every breakdown.
         */
        if (task.getTaskBreakdowns() != null
                && !task.getTaskBreakdowns().isEmpty()) {

            for (var breakdown : task.getTaskBreakdowns()) {

                Row row = sheet.createRow(rowIndex++);

                int col = 0;

                // Sequence
                setNumericCell(
                        row.createCell(col++),
                        task.getSequence(),
                        cellStyle
                );

                // Task
                setStringCell(
                        row.createCell(col++),
                        task.getTaskName(),
                        cellStyle
                );

                // Breakdown
                setStringCell(
                        row.createCell(col++),
                        breakdown.getActivityName(),
                        cellStyle
                );

                // Planned Start
                setDateCell(
                        row.createCell(col++),
                        breakdown.getPlannedStartDate(),
                        dateStyle
                );

                // Planned End
                setDateCell(
                        row.createCell(col++),
                        breakdown.getPlannedEndDate(),
                        dateStyle
                );

                // Working Days
                setNumericCell(
                        row.createCell(col++),
                        breakdown.getDuration(),
                        cellStyle
                );

                // Actual Start
                setDateCell(
                        row.createCell(col++),
                        breakdown.getActualStartDate(),
                        dateStyle
                );

                // Actual End
                setDateCell(
                        row.createCell(col++),
                        breakdown.getActualEndDate(),
                        dateStyle
                );

                // Actual Working Days
                setNumericCell(
                        row.createCell(col++),
                        calculateDays(
                                breakdown.getActualStartDate(),
                                breakdown.getActualEndDate()
                        ),
                        cellStyle
                );

                // Predecessor
                setStringCell(
                        row.createCell(col++),
                        task.getPredecessor(),
                        cellStyle
                );

                // Status
                setStringCell(
                        row.createCell(col++),
                        task.getStatus(),
                        cellStyle
                );
            }

        } else {

            /*
             * Fallback for tasks without breakdowns.
             */
            Row row = sheet.createRow(rowIndex++);

            int col = 0;

            // Sequence
            setNumericCell(
                    row.createCell(col++),
                    task.getSequence(),
                    cellStyle
            );

            // Task
            setStringCell(
                    row.createCell(col++),
                    task.getTaskName(),
                    cellStyle
            );

            // Breakdown
            setStringCell(
                    row.createCell(col++),
                    "",
                    cellStyle
            );

            // Planned Start
            setDateCell(
                    row.createCell(col++),
                    task.getPlannedStartDate(),
                    dateStyle
            );

            // Planned End
            setDateCell(
                    row.createCell(col++),
                    task.getPlannedEndDate(),
                    dateStyle
            );

            // Working Days
            setNumericCell(
                    row.createCell(col++),
                    task.getDuration(),
                    cellStyle
            );

            // Actual Start
            setDateCell(
                    row.createCell(col++),
                    task.getActualStartDate(),
                    dateStyle
            );

            // Actual End
            setDateCell(
                    row.createCell(col++),
                    task.getActualEndDate(),
                    dateStyle
            );

            // Actual Working Days
            setNumericCell(
                    row.createCell(col++),
                    calculateDays(
                            task.getActualStartDate(),
                            task.getActualEndDate()
                    ),
                    cellStyle
            );

            // Predecessor
            setStringCell(
                    row.createCell(col++),
                    task.getPredecessor(),
                    cellStyle
            );

            // Status
            setStringCell(
                    row.createCell(col++),
                    task.getStatus(),
                    cellStyle
            );
        }
    }

    /*
     * Format the sheet.
     */
    for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);

        // Prevent extremely wide columns
        if (sheet.getColumnWidth(i) > 15000) {
            sheet.setColumnWidth(i, 15000);
        }
    }

    sheet.createFreezePane(0, 1);

    if (rowIndex > 1) {

        sheet.setAutoFilter(
                new CellRangeAddress(
                        0,
                        rowIndex - 1,
                        0,
                        columns.length - 1
                )
        );
    }
}
   private void createHiddenGanttDataSheet(
        XSSFWorkbook workbook,
        SaveProjectScheduleRequest request) {

    Sheet sheet = workbook.createSheet("Gantt Data");

    workbook.setSheetHidden(
            workbook.getSheetIndex(sheet),
            true
    );

    Row header = sheet.createRow(0);

    header.createCell(0).setCellValue("Task");
    header.createCell(1).setCellValue("Offset");
    header.createCell(2).setCellValue("Duration");
    header.createCell(3).setCellValue("Sequence");
    header.createCell(4).setCellValue("Breakdown");

    if (request.getTasks() == null
            || request.getTasks().isEmpty()) {
        return;
    }

    int rowIndex = 1;

    for (SaveProjectScheduleTaskRequest task : request.getTasks()) {

        /*
         * Export breakdowns as individual Gantt rows.
         */
        if (task.getTaskBreakdowns() != null
                && !task.getTaskBreakdowns().isEmpty()) {

            for (var breakdown : task.getTaskBreakdowns()) {

                LocalDate start =
                        breakdown.getPlannedStartDate();

                LocalDate end =
                        breakdown.getPlannedEndDate();

                if (start == null || end == null) {
                    continue;
                }

                Row row = sheet.createRow(rowIndex++);

                String label =
                        task.getTaskName()
                        + " - "
                        + breakdown.getActivityName();

                row.createCell(0)
                        .setCellValue(label);

                row.createCell(1)
                        .setCellValue(
                                DateUtil.getExcelDate(start)
                        );

                row.createCell(2)
                        .setCellValue(
                                DateUtil.getExcelDate(end)
                                - DateUtil.getExcelDate(start)
                                + 1
                        );

                row.createCell(3)
                        .setCellValue(task.getSequence());

                row.createCell(4)
                        .setCellValue(
                                breakdown.getActivityName()
                        );
            }

        } else {

            /*
             * Fallback for tasks without breakdowns.
             */
            LocalDate start =
                    task.getPlannedStartDate();

            LocalDate end =
                    task.getPlannedEndDate();

            if (start == null || end == null) {
                continue;
            }

            Row row = sheet.createRow(rowIndex++);

            row.createCell(0)
                    .setCellValue(task.getTaskName());

            row.createCell(1)
                    .setCellValue(
                            DateUtil.getExcelDate(start)
                    );

            row.createCell(2)
                    .setCellValue(
                            DateUtil.getExcelDate(end)
                            - DateUtil.getExcelDate(start)
                            + 1
                    );

            row.createCell(3)
                    .setCellValue(task.getSequence());

            row.createCell(4)
                    .setCellValue("");
        }
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
    
    int lastRow = dataSheet.getLastRowNum();

    if (lastRow < 1) {
        return;
    }

    double minDate =
            dataSheet.getRow(1)
                    .getCell(1)
                    .getNumericCellValue();

    valueAxis.setMinimum(minDate);

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

    chartData.setVaryColors(true);

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
    
    CTBarSer durationBar =
            barChart.getSerArray(1);

    for (int i = 0; i < lastRow; i++) {

        String taskName =
                dataSheet.getRow(i + 1)
                         .getCell(0)
                         .getStringCellValue();

        byte[] color =
                getTaskColor(taskName);

        CTDPt point =
                durationBar.addNewDPt();

        point.addNewIdx().setVal(i);

        CTShapeProperties spPr =
                point.addNewSpPr();

        CTSolidColorFillProperties fill =
                spPr.addNewSolidFill();

        CTSRgbColor rgb =
                fill.addNewSrgbClr();

        rgb.setVal(color);
    }
}
    
   
   private byte[] getTaskColor(String taskName) {

	    taskName = taskName.toLowerCase();

	    if (taskName.contains("requirement"))
	        return new byte[]{33, (byte)150, (byte)243}; // Blue

	    if (taskName.contains("architecture")
	            || taskName.contains("design"))
	        return new byte[]{(byte)156, 39, (byte)176}; // Purple

	    if (taskName.contains("development")
	            || taskName.contains("generation")
	            || taskName.contains("logic"))
	        return new byte[]{76, (byte)175, 80}; // Green

	    if (taskName.contains("integration"))
	        return new byte[]{(byte)255, (byte)152, 0}; // Orange

	    if (taskName.contains("testing"))
	        return new byte[]{(byte)244, 67, 54}; // Red

	    if (taskName.contains("deployment"))
	        return new byte[]{96, 125, (byte)139}; // Grey

	    return new byte[]{0, (byte)188, (byte)212}; // Cyan
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
    
    private void setStringCell(
            Cell cell,
            String value,
            CellStyle style) {

        cell.setCellValue(
                value == null ? "" : value
        );

        cell.setCellStyle(style);
    }
    
    private void setNumericCell(
            Cell cell,
            Number value,
            CellStyle style) {

        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }

        cell.setCellStyle(style);
    }
    
    private void setDateCell(
            Cell cell,
            LocalDate value,
            CellStyle style) {

        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }

        cell.setCellStyle(style);
    }
    
    private int calculateDays(
            LocalDate start,
            LocalDate end) {

        if (start == null || end == null) {
            return 0;
        }

        return (int) (
                end.toEpochDay()
                - start.toEpochDay()
                + 1
        );
    }
}
