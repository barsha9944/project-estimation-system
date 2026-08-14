package com.projectestimation.backend.estimation.service;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.common.exception.BadRequestException;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.dto.ActorCalculationRequest;
import com.projectestimation.backend.estimation.dto.ActorCalculationResponse;
import com.projectestimation.backend.estimation.dto.ActorDto;
import com.projectestimation.backend.estimation.dto.EnvironmentalFactorCalculationRequest;
import com.projectestimation.backend.estimation.dto.EnvironmentalFactorCalculationResponse;
import com.projectestimation.backend.estimation.dto.EnvironmentalFactorDto;
import com.projectestimation.backend.estimation.dto.EstimationActorResponse;
import com.projectestimation.backend.estimation.dto.EstimationEnvironmentalFactorResponse;
import com.projectestimation.backend.estimation.dto.EstimationResponse;
import com.projectestimation.backend.estimation.dto.EstimationTechnicalFactorResponse;
import com.projectestimation.backend.estimation.dto.EstimationUseCaseResponse;
import com.projectestimation.backend.estimation.dto.FinalCalculationRequest;
import com.projectestimation.backend.estimation.dto.FinalCalculationResponse;
import com.projectestimation.backend.estimation.dto.SaveEstimationRequest;
import com.projectestimation.backend.estimation.dto.TechnicalFactorCalculationRequest;
import com.projectestimation.backend.estimation.dto.TechnicalFactorCalculationResponse;
import com.projectestimation.backend.estimation.dto.TechnicalFactorDto;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationRequest;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationResponse;
import com.projectestimation.backend.estimation.dto.UseCaseDto;
import com.projectestimation.backend.estimation.model.EstimationActor;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.model.EstimationEnvironmentalFactor;
import com.projectestimation.backend.estimation.model.EstimationTechnicalFactor;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.estimation.repository.EstimationActorRepository;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.estimation.repository.EstimationEnvironmentalFactorRepository;
import com.projectestimation.backend.estimation.repository.EstimationTechnicalFactorRepository;
import com.projectestimation.backend.estimation.repository.EstimationUseCaseRepository;
import com.projectestimation.backend.opportunity.dto.DownloadEstimateRequest;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.proposal.repository.ProposalRepository;

@Service
@Transactional
public class CalculationService {

	private final OpportunityRepository opportunityRepository;
	private final EstimationAnalysisRepository estimationAnalysisRepository;
	private final EstimationActorRepository estimationActorRepository;
	private final EstimationUseCaseRepository estimationUseCaseRepository;
	private final EstimationTechnicalFactorRepository estimationTechnicalFactorRepository;
	private final EstimationEnvironmentalFactorRepository estimationEnvironmentalFactorRepository;
	private final ProposalRepository proposalRepository;
	private final ProjectScheduleRepository projectScheduleRepository;
	
	public CalculationService(OpportunityRepository opportunityRepository,
			EstimationAnalysisRepository estimationAnalysisRepository,
			EstimationActorRepository estimationActorRepository,
			EstimationUseCaseRepository estimationUseCaseRepository,
			EstimationTechnicalFactorRepository estimationTechnicalFactorRepository,
			EstimationEnvironmentalFactorRepository estimationEnvironmentalFactorRepository,
			ProposalRepository proposalRepository,
			ProjectScheduleRepository projectScheduleRepository) {

		this.opportunityRepository = opportunityRepository;
		this.estimationAnalysisRepository = estimationAnalysisRepository;
		this.estimationActorRepository = estimationActorRepository;
		this.estimationUseCaseRepository = estimationUseCaseRepository;
		this.estimationTechnicalFactorRepository = estimationTechnicalFactorRepository;
		this.estimationEnvironmentalFactorRepository = estimationEnvironmentalFactorRepository;
		this.proposalRepository = proposalRepository;
		this.projectScheduleRepository = projectScheduleRepository;
	}

	public ActorCalculationResponse calculate(ActorCalculationRequest request) {

		EstimationAnalysis analysis = estimationAnalysisRepository.findByOpportunityId(request.getOpportunityId())
				.orElseGet(() -> {

					Opportunity opportunity = opportunityRepository.findById(request.getOpportunityId())
							.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

					EstimationAnalysis newAnalysis = new EstimationAnalysis();

					newAnalysis.setOpportunity(opportunity);

					return estimationAnalysisRepository.save(newAnalysis);
				});

		// Delete previously saved actors
		estimationActorRepository.deleteByEstimationAnalysisId(analysis.getId());

		int simple = 0;
		int average = 0;
		int complex = 0;

		for (ActorDto dto : request.getActors()) {

			EstimationActor actor = new EstimationActor();

			actor.setEstimationAnalysis(analysis);
			actor.setActorName(dto.getActorName());
			actor.setActorType(dto.getActorType());

			estimationActorRepository.save(actor);

			if (dto.getActorType() == null) {
				continue;
			}

			switch (dto.getActorType().trim().toUpperCase()) {

			case "SIMPLE":
				simple++;
				break;

			case "AVERAGE":
				average++;
				break;

			case "COMPLEX":
				complex++;
				break;
			}
		}

		int aw = (simple * 1) + (average * 2) + (complex * 3);

		analysis.setActorWeight(aw);

		estimationAnalysisRepository.save(analysis);

		return new ActorCalculationResponse(simple, average, complex, aw);
	}

	public UseCaseCalculationResponse calculate(UseCaseCalculationRequest request) {

		EstimationAnalysis analysis = estimationAnalysisRepository.findByOpportunityId(request.getOpportunityId())
				.orElseGet(() -> {

					Opportunity opportunity = opportunityRepository.findById(request.getOpportunityId())
							.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

					EstimationAnalysis newAnalysis = new EstimationAnalysis();

					newAnalysis.setOpportunity(opportunity);

					return estimationAnalysisRepository.save(newAnalysis);
				});

		// Delete previous use cases
		estimationUseCaseRepository.deleteByEstimationAnalysisId(analysis.getId());

		int simple = 0;
		int average = 0;
		int complex = 0;

		for (UseCaseDto dto : request.getUseCases()) {

			EstimationUseCase useCase = new EstimationUseCase();

			useCase.setEstimationAnalysis(analysis);
			useCase.setUseCaseName(dto.getUseCaseName());
			useCase.setComplexity(dto.getComplexity());

			estimationUseCaseRepository.save(useCase);

			if (dto.getComplexity() == null) {
				continue;
			}

			switch (dto.getComplexity().trim().toUpperCase()) {

			case "SIMPLE":
				simple++;
				break;

			case "AVERAGE":
				average++;
				break;

			case "COMPLEX":
				complex++;
				break;
			}
		}

		int uucp = (simple * 5) + (average * 10) + (complex * 15);

		analysis.setUucp(uucp);

		estimationAnalysisRepository.save(analysis);

		return new UseCaseCalculationResponse(simple, average, complex, uucp);
	}

	public TechnicalFactorCalculationResponse calculate(TechnicalFactorCalculationRequest request) {

		EstimationAnalysis analysis = estimationAnalysisRepository.findByOpportunityId(request.getOpportunityId())
				.orElseThrow(() -> new ResourceNotFoundException("Estimation Analysis not found"));

		estimationTechnicalFactorRepository.deleteByEstimationAnalysisId(analysis.getId());

		double total = 0;

		for (TechnicalFactorDto dto : request.getTechnicalFactors()) {

			EstimationTechnicalFactor factor = new EstimationTechnicalFactor();

			factor.setEstimationAnalysis(analysis);

			factor.setFactorName(dto.getFactorName());

			factor.setMultiplier(dto.getMultiplier());

			factor.setMagnitude(dto.getMagnitude());
			
			factor.setDescription(dto.getDescription());

			estimationTechnicalFactorRepository.save(factor);

			total += dto.getMultiplier() * dto.getMagnitude();
		}

		double tcf = 0.6 + (total / 100.0);

		analysis.setTcf(tcf);

		estimationAnalysisRepository.save(analysis);

		return new TechnicalFactorCalculationResponse(tcf);
	}

	public EnvironmentalFactorCalculationResponse calculate(EnvironmentalFactorCalculationRequest request) {

		EstimationAnalysis analysis = estimationAnalysisRepository.findByOpportunityId(request.getOpportunityId())
				.orElseThrow(() -> new ResourceNotFoundException("Estimation Analysis not found"));

		estimationEnvironmentalFactorRepository.deleteByEstimationAnalysisId(analysis.getId());

		double weightedSum = 0;

		for (EnvironmentalFactorDto dto : request.getEnvironmentalFactors()) {

			EstimationEnvironmentalFactor factor = new EstimationEnvironmentalFactor();

			factor.setEstimationAnalysis(analysis);
			factor.setFactorName(dto.getFactorName());
			factor.setMultiplier(dto.getMultiplier());
			factor.setMagnitude(dto.getMagnitude());
			factor.setDescription(dto.getDescription());

			estimationEnvironmentalFactorRepository.save(factor);

			weightedSum += dto.getMultiplier() * dto.getMagnitude();
		}

		double ef = 1.4 + (-0.03 * weightedSum);

		analysis.setEf(ef);

		estimationAnalysisRepository.save(analysis);

		return new EnvironmentalFactorCalculationResponse(ef);
	}

	public FinalCalculationResponse calculateFinal(FinalCalculationRequest request) {

		EstimationAnalysis analysis = estimationAnalysisRepository.findByOpportunityId(request.getOpportunityId())
				.orElseThrow(() -> new ResourceNotFoundException("Estimation Analysis not found"));

		if (analysis.getUucp() == null || analysis.getTcf() == null || analysis.getEf() == null) {

			throw new BadRequestException("UUCP, TCF and EF must be calculated before final calculation.");
		}

		double ucp = analysis.getUucp() * analysis.getTcf() * analysis.getEf();

		double hoursOfEffort = ucp / request.getBenchmarkProductivityRatio();

		analysis.setUcp(ucp);

		analysis.setBenchmarkProductivityRatio(request.getBenchmarkProductivityRatio());

		analysis.setHoursOfEffort(hoursOfEffort);

		estimationAnalysisRepository.save(analysis);

		return new FinalCalculationResponse(ucp, hoursOfEffort);
	}
	
	
	
	public byte[] downloadEstimate(
	        DownloadEstimateRequest request
	) throws IOException {

	    EstimationAnalysis analysis =
	            estimationAnalysisRepository
	                    .findByOpportunityId(request.getOpportunityId())
	                    .orElseThrow(() ->
	                            new ResourceNotFoundException("Estimation Analysis not found"));

	    List<EstimationActor> actors =
	            estimationActorRepository
	                    .findByEstimationAnalysisId(analysis.getId());

	    List<EstimationUseCase> useCases =
	            estimationUseCaseRepository
	                    .findByEstimationAnalysisId(analysis.getId());

	    List<EstimationTechnicalFactor> technicalFactors =
	            estimationTechnicalFactorRepository
	                    .findByEstimationAnalysisId(analysis.getId());

	    List<EstimationEnvironmentalFactor> environmentalFactors =
	            estimationEnvironmentalFactorRepository
	                    .findByEstimationAnalysisId(analysis.getId());

	    try (
	            XSSFWorkbook workbook = new XSSFWorkbook();
	            ByteArrayOutputStream out = new ByteArrayOutputStream()
	    ) {

	        createActorSheet(
	                workbook,
	                actors,
	                analysis
	        );

	        createUseCaseSheet(
	                workbook,
	                useCases,
	                analysis
	        );

	        createTechnicalFactorSheet(
	                workbook,
	                technicalFactors,
	                analysis
	        );

	        createEnvironmentalFactorSheet(
	                workbook,
	                environmentalFactors,
	                analysis
	        );

	        createFinalCalculationSheet(
	                workbook,
	                analysis
	        );

	        workbook.write(out);

	        return out.toByteArray();
	    }
	}
	
	private void createActorSheet(
	        XSSFWorkbook workbook,
	        List<EstimationActor> actors,
	        EstimationAnalysis analysis
	) {

	    Sheet sheet = workbook.createSheet("Actor");

	    CellStyle titleStyle = workbook.createCellStyle();

	    Font titleFont = workbook.createFont();
	    titleFont.setBold(true);
	    titleFont.setFontHeightInPoints((short)14);

	    titleStyle.setFont(titleFont);

	    CellStyle headerStyle = workbook.createCellStyle();

	    headerStyle.setFillForegroundColor(
	            IndexedColors.ORANGE.getIndex());

	    headerStyle.setFillPattern(
	            FillPatternType.SOLID_FOREGROUND);

	    Font headerFont = workbook.createFont();

	    headerFont.setBold(true);
	    headerFont.setColor(
	            IndexedColors.WHITE.getIndex());

	    headerStyle.setFont(headerFont);

	    headerStyle.setAlignment(
	            HorizontalAlignment.CENTER);

	    headerStyle.setVerticalAlignment(
	            VerticalAlignment.CENTER);

	    headerStyle.setBorderTop(BorderStyle.THIN);
	    headerStyle.setBorderBottom(BorderStyle.THIN);
	    headerStyle.setBorderLeft(BorderStyle.THIN);
	    headerStyle.setBorderRight(BorderStyle.THIN);

	    CellStyle dataStyle = workbook.createCellStyle();

	    dataStyle.setBorderTop(BorderStyle.THIN);
	    dataStyle.setBorderBottom(BorderStyle.THIN);
	    dataStyle.setBorderLeft(BorderStyle.THIN);
	    dataStyle.setBorderRight(BorderStyle.THIN);
	    
	    dataStyle.setWrapText(true);
	    dataStyle.setVerticalAlignment(VerticalAlignment.TOP);

	    int rowIndex = 0;

	    Row titleRow = sheet.createRow(rowIndex++);

	    Cell titleCell = titleRow.createCell(0);

	    titleCell.setCellValue("Actor Details");

	    titleCell.setCellStyle(titleStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    0,
	                    0,
	                    0,
	                    2
	            )
	    );

	    rowIndex++;

	    Row headerRow =
	            sheet.createRow(rowIndex++);

	    String[] headers = {
	            "Actor Name",
	            "Actor Type",
	            "Multiplier"
	    };

	    for (int i = 0; i < headers.length; i++) {

	        Cell cell =
	                headerRow.createCell(i);

	        cell.setCellValue(headers[i]);

	        cell.setCellStyle(headerStyle);

	    }

	    for (EstimationActor actor : actors) {

	        Row row =
	                sheet.createRow(rowIndex++);

	        Cell name =
	                row.createCell(0);

	        name.setCellValue(
	                actor.getActorName());

	        name.setCellStyle(dataStyle);

	        Cell type =
	                row.createCell(1);

	        type.setCellValue(
	                actor.getActorType());

	        type.setCellStyle(dataStyle);

	        Cell multiplier =
	                row.createCell(2);

	        int value = switch (
	                actor.getActorType().toUpperCase()
	        ) {

	            case "SIMPLE" -> 1;

	            case "AVERAGE" -> 2;

	            default -> 3;
	        };

	        multiplier.setCellValue(value);

	        multiplier.setCellStyle(dataStyle);

	    }

	    sheet.setColumnWidth(0, 9000);

	    sheet.setColumnWidth(1, 5000);

	    sheet.setColumnWidth(2, 4000);
	    
	    rowIndex += 2;

	    Row summaryTitleRow = sheet.createRow(rowIndex++);

	    Cell summaryTitle = summaryTitleRow.createCell(0);

	    summaryTitle.setCellValue("Actor Summary");

	    summaryTitle.setCellStyle(titleStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    summaryTitleRow.getRowNum(),
	                    summaryTitleRow.getRowNum(),
	                    0,
	                    3
	            )
	    );

	    Row summaryHeader = sheet.createRow(rowIndex++);

	    String[] summaryHeaders = {
	            "Actor Summary",
	            "Multiplier",
	            "Number of Actors",
	            "Description"
	    };

	    for (int i = 0; i < summaryHeaders.length; i++) {

	        Cell cell = summaryHeader.createCell(i);

	        cell.setCellValue(summaryHeaders[i]);

	        cell.setCellStyle(headerStyle);

	    }

	    String[][] summaryData = {
	            {
	                    "Simple",
	                    "1",
	                    String.valueOf(
	                            actors.stream()
	                                    .filter(a -> "SIMPLE".equalsIgnoreCase(a.getActorType()))
	                                    .count()
	                    ),
	                    "Simple actors are other systems that communicate with your software via a pre-defined API. An API could be exposed through a dll, or as a REST, SOAP, or any web-service API or remote procedure call (RPC). The key element is that you are exposing interaction with your software through a specific, well-defined mechanism."
	            },
	            {
	                    "Average",
	                    "2",
	                    String.valueOf(
	                            actors.stream()
	                                    .filter(a -> "AVERAGE".equalsIgnoreCase(a.getActorType()))
	                                    .count()
	                    ),
	                    "Average actors can either be human beings interacting in a well defined protocol, or they could be systems that interact through a more complex or flexible API."
	            },
	            {
	                    "Complex",
	                    "3",
	                    String.valueOf(
	                            actors.stream()
	                                    .filter(a -> "COMPLEX".equalsIgnoreCase(a.getActorType()))
	                                    .count()
	                    ),
	                    "The original definition of complex actors specifies that users who interact with the software through a graphical user interface are complex actors. While that is true, the same classifcation should apply to users who interact with the system in unpredictable ways. An AJAX interface that exposes more of the underlying application (and data stores) than would be available through a rigid protocol might introduce similar complexity."
	            }
	    };

	    for (String[] data : summaryData) {

	        Row row = sheet.createRow(rowIndex++);
	        
	        row.setHeightInPoints(80);

	        for (int i = 0; i < data.length; i++) {

	            Cell cell = row.createCell(i);

	            cell.setCellValue(data[i]);

	            cell.setCellStyle(dataStyle);

	        }

	    }

	    Row totalRow = sheet.createRow(rowIndex);

	    Cell totalLabel = totalRow.createCell(0);

	    totalLabel.setCellValue("Calculated AW");

	    totalLabel.setCellStyle(headerStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    totalRow.getRowNum(),
	                    totalRow.getRowNum(),
	                    0,
	                    1
	            )
	    );

	    Cell totalValue = totalRow.createCell(2);

	    totalValue.setCellValue(
	            analysis.getActorWeight()
	    );

	    totalValue.setCellStyle(headerStyle);

	    sheet.setColumnWidth(3, 25000);
	}
	
	private void createUseCaseSheet(
	        XSSFWorkbook workbook,
	        List<EstimationUseCase> useCases,
	        EstimationAnalysis analysis
	) {

	    Sheet sheet = workbook.createSheet("Use Cases");

	    CellStyle titleStyle = workbook.createCellStyle();

	    Font titleFont = workbook.createFont();
	    titleFont.setBold(true);
	    titleFont.setFontHeightInPoints((short) 14);
	    titleStyle.setFont(titleFont);

	    CellStyle headerStyle = workbook.createCellStyle();

	    headerStyle.setFillForegroundColor(
	            IndexedColors.ORANGE.getIndex());

	    headerStyle.setFillPattern(
	            FillPatternType.SOLID_FOREGROUND);

	    Font headerFont = workbook.createFont();

	    headerFont.setBold(true);

	    headerFont.setColor(
	            IndexedColors.WHITE.getIndex());

	    headerStyle.setFont(headerFont);

	    headerStyle.setAlignment(
	            HorizontalAlignment.CENTER);

	    headerStyle.setVerticalAlignment(
	            VerticalAlignment.CENTER);

	    headerStyle.setBorderTop(BorderStyle.THIN);
	    headerStyle.setBorderBottom(BorderStyle.THIN);
	    headerStyle.setBorderLeft(BorderStyle.THIN);
	    headerStyle.setBorderRight(BorderStyle.THIN);

	    CellStyle dataStyle = workbook.createCellStyle();

	    dataStyle.setBorderTop(BorderStyle.THIN);
	    dataStyle.setBorderBottom(BorderStyle.THIN);
	    dataStyle.setBorderLeft(BorderStyle.THIN);
	    dataStyle.setBorderRight(BorderStyle.THIN);

	    dataStyle.setWrapText(true);

	    dataStyle.setVerticalAlignment(
	            VerticalAlignment.TOP);

	    int rowIndex = 0;

	    Row titleRow = sheet.createRow(rowIndex++);

	    Cell titleCell = titleRow.createCell(0);

	    titleCell.setCellValue("Use Case Details");

	    titleCell.setCellStyle(titleStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    0,
	                    0,
	                    0,
	                    2
	            )
	    );

	    rowIndex++;

	    Row headerRow = sheet.createRow(rowIndex++);

	    String[] headers = {
	            "Use Case Name",
	            "Use Case Type",
	            "Multiplier"
	    };

	    for (int i = 0; i < headers.length; i++) {

	        Cell cell = headerRow.createCell(i);

	        cell.setCellValue(headers[i]);

	        cell.setCellStyle(headerStyle);

	    }

	    for (EstimationUseCase useCase : useCases) {

	        Row row = sheet.createRow(rowIndex++);

	        Cell name = row.createCell(0);

	        name.setCellValue(
	                useCase.getUseCaseName());

	        name.setCellStyle(dataStyle);

	        Cell type = row.createCell(1);

	        type.setCellValue(
	                useCase.getComplexity());

	        type.setCellStyle(dataStyle);

	        Cell multiplier = row.createCell(2);

	        int value = switch (
	                useCase.getComplexity().toUpperCase()
	        ) {

	            case "SIMPLE" -> 5;

	            case "AVERAGE" -> 10;

	            default -> 15;
	        };

	        multiplier.setCellValue(value);

	        multiplier.setCellStyle(dataStyle);

	    }

	    sheet.setColumnWidth(0, 9000);
	    sheet.setColumnWidth(1, 5000);
	    sheet.setColumnWidth(2, 4000);
	    
	    rowIndex += 2;

	    Row summaryTitleRow = sheet.createRow(rowIndex++);

	    Cell summaryTitle = summaryTitleRow.createCell(0);

	    summaryTitle.setCellValue("Use Case Summary");

	    summaryTitle.setCellStyle(titleStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    summaryTitleRow.getRowNum(),
	                    summaryTitleRow.getRowNum(),
	                    0,
	                    3
	            )
	    );

	    Row summaryHeader = sheet.createRow(rowIndex++);

	    String[] summaryHeaders = {
	            "Use Case Summary",
	            "Multiplier",
	            "Number of Use Cases",
	            "Description"
	    };

	    for (int i = 0; i < summaryHeaders.length; i++) {

	        Cell cell = summaryHeader.createCell(i);

	        cell.setCellValue(summaryHeaders[i]);

	        cell.setCellStyle(headerStyle);

	    }

	    String[][] summaryData = {

	            {
	                    "Simple",
	                    "5",
	                    String.valueOf(
	                            useCases.stream()
	                                    .filter(u ->
	                                            "SIMPLE".equalsIgnoreCase(
	                                                    u.getComplexity()))
	                                    .count()
	                    ),
	                    "Simple Use Case - up to 3 transactions."
	            },

	            {
	                    "Average",
	                    "10",
	                    String.valueOf(
	                            useCases.stream()
	                                    .filter(u ->
	                                            "AVERAGE".equalsIgnoreCase(
	                                                    u.getComplexity()))
	                                    .count()
	                    ),
	                    "Average Use Case - 4 to 7 transactions."
	            },

	            {
	                    "Complex",
	                    "15",
	                    String.valueOf(
	                            useCases.stream()
	                                    .filter(u ->
	                                            "COMPLEX".equalsIgnoreCase(
	                                                    u.getComplexity()))
	                                    .count()
	                    ),
	                    "Complex Use Case - more than 7 transactions."
	            }

	    };

	    for (String[] data : summaryData) {

	        Row row = sheet.createRow(rowIndex++);

	        row.setHeightInPoints(40);

	        for (int i = 0; i < data.length; i++) {

	            Cell cell = row.createCell(i);

	            cell.setCellValue(data[i]);

	            cell.setCellStyle(dataStyle);

	        }

	    }

	    Row totalRow = sheet.createRow(rowIndex);

	    Cell totalLabel = totalRow.createCell(0);

	    totalLabel.setCellValue("Calculated UUCP");

	    totalLabel.setCellStyle(headerStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    totalRow.getRowNum(),
	                    totalRow.getRowNum(),
	                    0,
	                    1
	            )
	    );

	    Cell totalValue = totalRow.createCell(2);

	    totalValue.setCellValue(
	            analysis.getUucp()
	    );

	    totalValue.setCellStyle(headerStyle);

	    Cell empty = totalRow.createCell(3);

	    empty.setCellStyle(headerStyle);

	    sheet.setColumnWidth(3, 25000);

	}
	
	private void createTechnicalFactorSheet(
	        XSSFWorkbook workbook,
	        List<EstimationTechnicalFactor> technicalFactors,
	        EstimationAnalysis analysis
	) {

	    Sheet sheet = workbook.createSheet("Technical Factors");

	    CellStyle titleStyle = workbook.createCellStyle();

	    Font titleFont = workbook.createFont();
	    titleFont.setBold(true);
	    titleFont.setFontHeightInPoints((short) 14);
	    titleStyle.setFont(titleFont);

	    CellStyle headerStyle = workbook.createCellStyle();

	    headerStyle.setFillForegroundColor(
	            IndexedColors.ORANGE.getIndex());

	    headerStyle.setFillPattern(
	            FillPatternType.SOLID_FOREGROUND);

	    Font headerFont = workbook.createFont();

	    headerFont.setBold(true);

	    headerFont.setColor(
	            IndexedColors.WHITE.getIndex());

	    headerStyle.setFont(headerFont);

	    headerStyle.setAlignment(
	            HorizontalAlignment.CENTER);

	    headerStyle.setVerticalAlignment(
	            VerticalAlignment.CENTER);

	    headerStyle.setBorderTop(BorderStyle.THIN);
	    headerStyle.setBorderBottom(BorderStyle.THIN);
	    headerStyle.setBorderLeft(BorderStyle.THIN);
	    headerStyle.setBorderRight(BorderStyle.THIN);

	    CellStyle dataStyle = workbook.createCellStyle();

	    dataStyle.setBorderTop(BorderStyle.THIN);
	    dataStyle.setBorderBottom(BorderStyle.THIN);
	    dataStyle.setBorderLeft(BorderStyle.THIN);
	    dataStyle.setBorderRight(BorderStyle.THIN);

	    dataStyle.setWrapText(true);

	    dataStyle.setVerticalAlignment(
	            VerticalAlignment.TOP);

	    int rowIndex = 0;

	    Row titleRow = sheet.createRow(rowIndex++);

	    Cell titleCell = titleRow.createCell(0);

	    titleCell.setCellValue("Technical Factors");

	    titleCell.setCellStyle(titleStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    0,
	                    0,
	                    0,
	                    4
	            )
	    );

	    rowIndex++;

	    Row headerRow = sheet.createRow(rowIndex++);

	    String[] headers = {
	            "Factor",
	            "Multiplier",
	            "Magnitude",
	            "Weighted Value",
	            "Description"
	    };

	    for (int i = 0; i < headers.length; i++) {

	        Cell cell = headerRow.createCell(i);

	        cell.setCellValue(headers[i]);

	        cell.setCellStyle(headerStyle);

	    }

	    for (EstimationTechnicalFactor factor : technicalFactors) {

	        Row row = sheet.createRow(rowIndex++);

	        row.setHeightInPoints(60);

	        Cell c1 = row.createCell(0);
	        c1.setCellValue(factor.getFactorName());
	        c1.setCellStyle(dataStyle);

	        Cell c2 = row.createCell(1);
	        c2.setCellValue(factor.getMultiplier());
	        c2.setCellStyle(dataStyle);

	        Cell c3 = row.createCell(2);
	        c3.setCellValue(factor.getMagnitude());
	        c3.setCellStyle(dataStyle);

	        Cell c4 = row.createCell(3);
	        c4.setCellValue(
	                factor.getMultiplier()
	                        * factor.getMagnitude()
	        );
	        c4.setCellStyle(dataStyle);

	        Cell c5 = row.createCell(4);

	        c5.setCellValue(
	                factor.getDescription() == null
	                        ? ""
	                        : factor.getDescription()
	        );

	        c5.setCellStyle(dataStyle);

	    }

	    sheet.setColumnWidth(0, 9000);
	    sheet.setColumnWidth(1, 4000);
	    sheet.setColumnWidth(2, 4000);
	    sheet.setColumnWidth(3, 5000);
	    sheet.setColumnWidth(4, 25000);
	    
	    Row totalRow = sheet.createRow(rowIndex + 1);

	    Cell totalLabel = totalRow.createCell(0);

	    totalLabel.setCellValue("Calculated TCF");

	    totalLabel.setCellStyle(headerStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    totalRow.getRowNum(),
	                    totalRow.getRowNum(),
	                    0,
	                    3
	            )
	    );

	    Cell totalValue = totalRow.createCell(4);

	    totalValue.setCellValue(
	            analysis.getTcf()
	    );

	    totalValue.setCellStyle(headerStyle);

	}
	
	private void createEnvironmentalFactorSheet(
	        XSSFWorkbook workbook,
	        List<EstimationEnvironmentalFactor> environmentalFactors,
	        EstimationAnalysis analysis
	) {

	    Sheet sheet = workbook.createSheet("Environmental Factors");

	    CellStyle titleStyle = workbook.createCellStyle();

	    Font titleFont = workbook.createFont();
	    titleFont.setBold(true);
	    titleFont.setFontHeightInPoints((short) 14);
	    titleStyle.setFont(titleFont);

	    CellStyle headerStyle = workbook.createCellStyle();

	    headerStyle.setFillForegroundColor(
	            IndexedColors.ORANGE.getIndex());

	    headerStyle.setFillPattern(
	            FillPatternType.SOLID_FOREGROUND);

	    Font headerFont = workbook.createFont();

	    headerFont.setBold(true);

	    headerFont.setColor(
	            IndexedColors.WHITE.getIndex());

	    headerStyle.setFont(headerFont);

	    headerStyle.setAlignment(
	            HorizontalAlignment.CENTER);

	    headerStyle.setVerticalAlignment(
	            VerticalAlignment.CENTER);

	    headerStyle.setBorderTop(BorderStyle.THIN);
	    headerStyle.setBorderBottom(BorderStyle.THIN);
	    headerStyle.setBorderLeft(BorderStyle.THIN);
	    headerStyle.setBorderRight(BorderStyle.THIN);

	    CellStyle dataStyle = workbook.createCellStyle();

	    dataStyle.setBorderTop(BorderStyle.THIN);
	    dataStyle.setBorderBottom(BorderStyle.THIN);
	    dataStyle.setBorderLeft(BorderStyle.THIN);
	    dataStyle.setBorderRight(BorderStyle.THIN);

	    dataStyle.setWrapText(true);

	    dataStyle.setVerticalAlignment(
	            VerticalAlignment.TOP);

	    int rowIndex = 0;

	    Row titleRow = sheet.createRow(rowIndex++);

	    Cell titleCell = titleRow.createCell(0);

	    titleCell.setCellValue("Environmental Factors");

	    titleCell.setCellStyle(titleStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    0,
	                    0,
	                    0,
	                    4
	            )
	    );

	    rowIndex++;

	    Row headerRow = sheet.createRow(rowIndex++);

	    String[] headers = {
	            "Factor",
	            "Multiplier",
	            "Magnitude",
	            "Weighted Value",
	            "Description"
	    };

	    for (int i = 0; i < headers.length; i++) {

	        Cell cell = headerRow.createCell(i);

	        cell.setCellValue(headers[i]);

	        cell.setCellStyle(headerStyle);

	    }

	    for (EstimationEnvironmentalFactor factor : environmentalFactors) {

	        Row row = sheet.createRow(rowIndex++);

	        row.setHeightInPoints(70);

	        Cell c1 = row.createCell(0);
	        c1.setCellValue(factor.getFactorName());
	        c1.setCellStyle(dataStyle);

	        Cell c2 = row.createCell(1);
	        c2.setCellValue(factor.getMultiplier());
	        c2.setCellStyle(dataStyle);

	        Cell c3 = row.createCell(2);
	        c3.setCellValue(factor.getMagnitude());
	        c3.setCellStyle(dataStyle);

	        Cell c4 = row.createCell(3);
	        c4.setCellValue(
	                factor.getMultiplier()
	                        * factor.getMagnitude()
	        );
	        c4.setCellStyle(dataStyle);

	        Cell c5 = row.createCell(4);

	        c5.setCellValue(
	                factor.getDescription() == null
	                        ? ""
	                        : factor.getDescription()
	        );

	        c5.setCellStyle(dataStyle);

	    }

	    sheet.setColumnWidth(0, 9000);
	    sheet.setColumnWidth(1, 4000);
	    sheet.setColumnWidth(2, 4000);
	    sheet.setColumnWidth(3, 5000);
	    sheet.setColumnWidth(4, 25000);
	    
	    Row totalRow = sheet.createRow(rowIndex + 1);

	    Cell totalLabel = totalRow.createCell(0);

	    totalLabel.setCellValue("Calculated EF");

	    totalLabel.setCellStyle(headerStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    totalRow.getRowNum(),
	                    totalRow.getRowNum(),
	                    0,
	                    3
	            )
	    );

	    Cell totalValue = totalRow.createCell(4);

	    totalValue.setCellValue(
	            analysis.getEf()
	    );

	    totalValue.setCellStyle(headerStyle);

	}
	
	private void createFinalCalculationSheet(
	        XSSFWorkbook workbook,
	        EstimationAnalysis analysis
	) {

	    Sheet sheet =
	            workbook.createSheet("Final Calculation");

	    CellStyle titleStyle =
	            workbook.createCellStyle();

	    Font titleFont =
	            workbook.createFont();

	    titleFont.setBold(true);
	    titleFont.setFontHeightInPoints((short) 14);

	    titleStyle.setFont(titleFont);

	    CellStyle headerStyle =
	            workbook.createCellStyle();

	    headerStyle.setFillForegroundColor(
	            IndexedColors.ORANGE.getIndex());

	    headerStyle.setFillPattern(
	            FillPatternType.SOLID_FOREGROUND);

	    Font headerFont =
	            workbook.createFont();

	    headerFont.setBold(true);

	    headerFont.setColor(
	            IndexedColors.WHITE.getIndex());

	    headerStyle.setFont(headerFont);

	    headerStyle.setAlignment(
	            HorizontalAlignment.CENTER);

	    headerStyle.setVerticalAlignment(
	            VerticalAlignment.CENTER);

	    headerStyle.setBorderTop(BorderStyle.THIN);
	    headerStyle.setBorderBottom(BorderStyle.THIN);
	    headerStyle.setBorderLeft(BorderStyle.THIN);
	    headerStyle.setBorderRight(BorderStyle.THIN);

	    CellStyle dataStyle =
	            workbook.createCellStyle();

	    dataStyle.setBorderTop(BorderStyle.THIN);
	    dataStyle.setBorderBottom(BorderStyle.THIN);
	    dataStyle.setBorderLeft(BorderStyle.THIN);
	    dataStyle.setBorderRight(BorderStyle.THIN);

	    int rowIndex = 0;

	    Row titleRow =
	            sheet.createRow(rowIndex++);

	    Cell titleCell =
	            titleRow.createCell(0);

	    titleCell.setCellValue(
	            "Final Calculation");

	    titleCell.setCellStyle(titleStyle);

	    sheet.addMergedRegion(
	            new CellRangeAddress(
	                    0,
	                    0,
	                    0,
	                    1
	            )
	    );

	    rowIndex++;

	    String[][] rows = {

	            {
	                    "Actor Weight (AW)",
	                    String.valueOf(
	                            analysis.getActorWeight())
	            },

	            {
	                    "UUCP",
	                    String.valueOf(
	                            analysis.getUucp())
	            },

	            {
	                    "Technical Complexity Factor (TCF)",
	                    String.valueOf(
	                            analysis.getTcf())
	            },

	            {
	                    "Environmental Factor (EF)",
	                    String.valueOf(
	                            analysis.getEf())
	            },

	            {
	                    "Use Case Points (UCP)",
	                    String.valueOf(
	                            analysis.getUcp())
	            },

	            {
	                    "Benchmark Productivity Ratio",
	                    String.valueOf(
	                            analysis.getBenchmarkProductivityRatio())
	            },

	            {
	                    "Hours Of Effort",
	                    String.valueOf(
	                            analysis.getHoursOfEffort())
	            }

	    };

	    for (String[] data : rows) {

	        Row row =
	                sheet.createRow(rowIndex++);

	        Cell label =
	                row.createCell(0);

	        label.setCellValue(data[0]);

	        label.setCellStyle(headerStyle);

	        Cell value =
	                row.createCell(1);

	        value.setCellValue(data[1]);

	        value.setCellStyle(dataStyle);

	    }

	    sheet.setColumnWidth(0, 12000);

	    sheet.setColumnWidth(1, 5000);

	}
	
	public String getEstimateFileName(
	        DownloadEstimateRequest request
	) {

	    EstimationAnalysis analysis =
	            estimationAnalysisRepository
	                    .findByOpportunityId(request.getOpportunityId())
	                    .orElseThrow(() ->
	                            new ResourceNotFoundException("Estimation Analysis not found"));

	    String opportunityName =
	            analysis.getOpportunity().getOpportunityName();

	    opportunityName =
	            opportunityName.replaceAll("[\\\\/:*?\"<>|]", "_");

	    return opportunityName + "_Estimate.xlsx";
	}
	
	public EstimationResponse getEstimation(
        Long opportunityId) {

	    EstimationAnalysis analysis =
	            estimationAnalysisRepository
	                    .findByOpportunityId(opportunityId)
	                    .orElseThrow(() ->
	                            new ResourceNotFoundException(
	                                    "Estimation not found"));
	
	    List<EstimationActor> actors =
	            estimationActorRepository
	                    .findByEstimationAnalysisId(
	                            analysis.getId());
	
	    List<EstimationUseCase> useCases =
	            estimationUseCaseRepository
	                    .findByEstimationAnalysisId(
	                            analysis.getId());
	
	    List<EstimationTechnicalFactor> technicalFactors =
	            estimationTechnicalFactorRepository
	                    .findByEstimationAnalysisId(
	                            analysis.getId());
	
	    List<EstimationEnvironmentalFactor> environmentalFactors =
	            estimationEnvironmentalFactorRepository
	                    .findByEstimationAnalysisId(
	                            analysis.getId());
	
	    EstimationResponse response =
	            new EstimationResponse();
	
	    response.setActors(
	
	            actors.stream()
	
	                    .map(actor -> new EstimationActorResponse(
	
	                            actor.getActorName(),
	
	                            actor.getActorType()
	
	                    ))
	
	                    .toList()
	
	    );
	    
	    response.setUseCases(
	
	            useCases.stream()
	
	                    .map(useCase -> new EstimationUseCaseResponse(
	
	                            useCase.getUseCaseName(),
	
	                            useCase.getComplexity()
	
	                    ))
	
	                    .toList()
	
	    );
	    
	    response.setTechnicalFactors(
	
	            technicalFactors.stream()
	
	                    .map(factor ->
	
	                            new EstimationTechnicalFactorResponse(
	
	                                    factor.getFactorName(),
	
	                                    factor.getMultiplier(),
	
	                                    factor.getMagnitude(),
	
	                                    factor.getDescription()
	
	                            ))
	
	                    .toList()
	
	    );
	    
	    response.setEnvironmentalFactors(
	
	            environmentalFactors.stream()
	
	                    .map(factor ->
	
	                            new EstimationEnvironmentalFactorResponse(
	
	                                    factor.getFactorName(),
	
	                                    factor.getMultiplier(),
	
	                                    factor.getMagnitude(),
	
	                                    factor.getDescription()
	
	                            ))
	
	                    .toList()
	
	    );
	    
	    response.setActorWeight(
	            analysis.getActorWeight());
	
	    response.setUucp(
	            analysis.getUucp());
	
	    response.setTcf(
	            analysis.getTcf());
	
	    response.setEf(
	            analysis.getEf());
	
	    response.setUcp(
	            analysis.getUcp());
	
	    response.setBenchmarkProductivityRatio(
	            analysis.getBenchmarkProductivityRatio());
	
	    response.setHoursOfEffort(
	            analysis.getHoursOfEffort());
	    
	    response.setEstimationCompleted(
	            estimationAnalysisRepository
	                    .findByOpportunityId(opportunityId)
	                    .isPresent());

	    response.setProposalCompleted(
	            proposalRepository
	                    .existsByOpportunityId(opportunityId));

	    response.setWorkScheduleCompleted(
	    		projectScheduleRepository
	                    .existsByOpportunityId(opportunityId));

	    response.setSummaryMetricsCompleted(false);

	    
	    return response;
	}
	
	private void saveActors(
	        EstimationAnalysis analysis,
	        List<ActorDto> actors) {
	
	    for (ActorDto dto : actors) {
	
	        EstimationActor actor = new EstimationActor();
	
	        actor.setEstimationAnalysis(analysis);
	        actor.setActorName(dto.getActorName());
	        actor.setActorType(dto.getActorType());
	
	        estimationActorRepository.save(actor);
	    }
	}
	
	private void saveUseCases(
        EstimationAnalysis analysis,
        List<UseCaseDto> useCases) {

	    for (UseCaseDto dto : useCases) {
	
	        EstimationUseCase useCase = new EstimationUseCase();
	
	        useCase.setEstimationAnalysis(analysis);
	        useCase.setUseCaseName(dto.getUseCaseName());
	        useCase.setComplexity(dto.getComplexity());
	
	        estimationUseCaseRepository.save(useCase);
	    }
	}
	
	private void saveTechnicalFactors(
        EstimationAnalysis analysis,
        List<TechnicalFactorDto> technicalFactors) {

	    for (TechnicalFactorDto dto : technicalFactors) {
	
	        EstimationTechnicalFactor factor =
	                new EstimationTechnicalFactor();
	
	        factor.setEstimationAnalysis(analysis);
	        factor.setFactorName(dto.getFactorName());
	        factor.setMultiplier(dto.getMultiplier());
	        factor.setMagnitude(dto.getMagnitude());
	        factor.setDescription(dto.getDescription());
	
	        estimationTechnicalFactorRepository.save(factor);
	    }
	}
	
	private void saveEnvironmentalFactors(
        EstimationAnalysis analysis,
        List<EnvironmentalFactorDto> environmentalFactors) {

	    for (EnvironmentalFactorDto dto : environmentalFactors) {
	
	        EstimationEnvironmentalFactor factor =
	                new EstimationEnvironmentalFactor();
	
	        factor.setEstimationAnalysis(analysis);
	        factor.setFactorName(dto.getFactorName());
	        factor.setMultiplier(dto.getMultiplier());
	        factor.setMagnitude(dto.getMagnitude());
	        factor.setDescription(dto.getDescription());
	
	        estimationEnvironmentalFactorRepository.save(factor);
	    }
	}
	
	public EstimationResponse saveEstimation(
	        SaveEstimationRequest request) {

		System.out.println("Request Opportunity Id = " + request.getOpportunityId());
		
//		Optional<EstimationAnalysis> existing =
//		        estimationAnalysisRepository.findByOpportunityId(
//		                request.getOpportunityId());
//
//		System.out.println("Existing Analysis Found = " + existing.isPresent());
//
//		existing.ifPresent(existingAnalysis -> {
//
//		    System.out.println("Analysis Id = " + existingAnalysis.getId());
//
//		    Long analysisId = existingAnalysis.getId();
//
//		    estimationActorRepository.deleteByEstimationAnalysisId(analysisId);
//
//		    estimationUseCaseRepository.deleteByEstimationAnalysisId(analysisId);
//
//		    estimationTechnicalFactorRepository.deleteByEstimationAnalysisId(analysisId);
//
//		    estimationEnvironmentalFactorRepository.deleteByEstimationAnalysisId(analysisId);
//
//		    estimationAnalysisRepository.delete(existingAnalysis);
//		    estimationAnalysisRepository.flush();
//		});

		Opportunity opportunity =
		        opportunityRepository
		                .findById(request.getOpportunityId())
		                .orElseThrow(() ->
		                        new ResourceNotFoundException(
		                                "Opportunity not found"));

		EstimationAnalysis analysis =
		        estimationAnalysisRepository
		                .findByOpportunityId(request.getOpportunityId())
		                .orElse(new EstimationAnalysis());

		analysis.setOpportunity(opportunity);

		if (analysis.getId() != null) {

		    estimationActorRepository.deleteByEstimationAnalysisId(analysis.getId());

		    estimationUseCaseRepository.deleteByEstimationAnalysisId(analysis.getId());

		    estimationTechnicalFactorRepository.deleteByEstimationAnalysisId(analysis.getId());

		    estimationEnvironmentalFactorRepository.deleteByEstimationAnalysisId(analysis.getId());
		}

	    analysis.setActorWeight(
	            request.getActorWeight());

	    analysis.setUucp(
	            request.getUucp());

	    analysis.setTcf(
	            request.getTcf());

	    analysis.setEf(
	            request.getEf());

	    analysis.setUcp(
	            request.getUcp());

	    analysis.setBenchmarkProductivityRatio(
	            request.getBenchmarkProductivityRatio());

	    analysis.setHoursOfEffort(
	            request.getHoursOfEffort());
	    
	    analysis.setCurrency(request.getCurrency());
	    
	    analysis.setHourlyRate(request.getHourlyRate());
	    
	    analysis.setProjectPrice(request.getProjectPrice());

	    analysis =
	            estimationAnalysisRepository.save(analysis);

	    saveActors(
	            analysis,
	            request.getActors());

	    saveUseCases(
	            analysis,
	            request.getUseCases());

	    saveTechnicalFactors(
	            analysis,
	            request.getTechnicalFactors());

	    saveEnvironmentalFactors(
	            analysis,
	            request.getEnvironmentalFactors());

	    return getEstimation(
	            request.getOpportunityId());
	}
}
