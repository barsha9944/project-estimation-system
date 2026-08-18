package com.projectestimation.backend.projectmetrics.calculator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.projectmetrics.dto.AnalysisMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.CodingMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.DesignMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.OtherActivityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.QualityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SitMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SprintMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SummaryMetricsResponse;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.proposal.repository.ProposalRepository;

@Component
public class ProjectMetricsCalculator {

    private final OpportunityRepository opportunityRepository;

    private final EstimationAnalysisRepository estimationAnalysisRepository;

    private final ProposalRepository proposalRepository;

    private final ProjectScheduleRepository projectScheduleRepository;

    public ProjectMetricsCalculator(

            OpportunityRepository opportunityRepository,

            EstimationAnalysisRepository estimationAnalysisRepository,

            ProposalRepository proposalRepository,

            ProjectScheduleRepository projectScheduleRepository) {

        this.opportunityRepository = opportunityRepository;
        this.estimationAnalysisRepository = estimationAnalysisRepository;
        this.proposalRepository = proposalRepository;
        this.projectScheduleRepository = projectScheduleRepository;
    }

    @Transactional(readOnly = true)
    public ProjectMetricsResponse calculate(
            Long opportunityId) {

    	Opportunity opportunity =
    	        opportunityRepository.findById(opportunityId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Opportunity not found"));

    	EstimationAnalysis analysis =
    	        estimationAnalysisRepository
    	        .findByOpportunityId(opportunityId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Estimation not found"));

//    	Proposal proposal =
//    	        proposalRepository
//    	        .findFirstByOpportunity_IdOrderByVersionDesc(opportunityId)
//    	        .orElseThrow(() ->
//    	                new ResourceNotFoundException("Proposal not found"));

    	ProjectSchedule schedule =
    	        projectScheduleRepository
    	        .findByOpportunityIdWithTasks(opportunityId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Project Schedule not found"));

    	SummaryMetricsResponse summary =
    	        new SummaryMetricsResponse();
    	AnalysisMetricsResponse analysisResponse =
    	        new AnalysisMetricsResponse();
    	DesignMetricsResponse designResponse =
    	        new DesignMetricsResponse();
    	CodingMetricsResponse codingResponse =
    	        new CodingMetricsResponse();
    	SitMetricsResponse sitResponse =
    	        new SitMetricsResponse();
    	OtherActivityMetricsResponse otherActivityMetricsResponse =
    	        calculateOtherActivity(schedule);

    	QualityMetricsResponse qualityResponse =
    	        new QualityMetricsResponse();
    	
 
    	
    	summary.setProjectName(
    	        opportunity.getOpportunityName()
    	);

//    	summary.setReleaseNo(
//    	        proposal.getVersion()
//    	);
    	
    	// Barsha: need to come later
    	
    	summary.setTotalPlannedEffortWithoutPm(
    	        schedule.getEstimatedHours()
    	);

    	summary.setTotalPlannedEffort(
    	        schedule.getEstimatedHours()
    	);
    	
    	summary.setPlannedDuration(
    	        schedule.getDurationDays()
    	);
    	
    	summary.setOriginalSize(
    	        analysis.getUcp()
    	);
    	
    	summary.setActualSize(
    	        analysis.getUcp()
    	);
    	
    	summary.setSizeVariance(

    	        calculateSizeVariance(

    	                summary.getOriginalSize(),

    	                summary.getActualSize()

    	        )

    	);
    	
    	summary.setTotalActualEffortWithoutPm(
    	        summary.getTotalPlannedEffortWithoutPm()
    	);
    	
    	summary.setTotalActualEffort(
    	        summary.getTotalActualEffortWithoutPm()
    	);
    	
    	summary.setEffortVariance(

    	        calculateEffortVariance(

    	                summary.getTotalPlannedEffort(),

    	                summary.getTotalActualEffort()

    	        )

    	);
    	
    	summary.setActualDuration(
    	        schedule.getDurationDays()
    	);
    	
    	summary.setScheduleVariance(

    	        calculateScheduleVariance(

    	                summary.getPlannedDuration(),

    	                summary.getActualDuration()

    	        )

    	);
    	
    	summary.setActualOverallProductivity(

    	        calculateProductivity(

    	                summary.getActualSize(),

    	                summary.getTotalActualEffort()

    	        )

    	);
    	
    	ProjectScheduleTask uatTask =
    	        getUatTask(schedule);

    	Double plannedUatEffort =
    	        calculateTaskEffort(
    	                uatTask.getDuration(),
    	                schedule
    	        );

    	Integer actualUatDuration =
    	        getActualTaskDuration(uatTask);

    	Double actualUatEffort =
    	        calculateTaskEffort(
    	                actualUatDuration,
    	                schedule
    	        );

    	qualityResponse.setPlannedUatEffort(
    	        plannedUatEffort
    	);

    	qualityResponse.setActualUatEffort(
    	        actualUatEffort
    	);
    	
    	ProjectScheduleTask analysisTask =
    	        getAnalysisTask(schedule);

    	analysisResponse.setPlannedDuration(
    	        analysisTask.getDuration()
    	);
    	
    	analysisResponse.setActualDuration(
    	        analysisTask.getDuration()
    	);
    	
    	analysisResponse.setScheduleVariance(

    	        calculateScheduleVariance(

    	                analysisResponse.getPlannedDuration(),

    	                analysisResponse.getActualDuration()

    	        )

    	);
    	
    	analysisResponse.setPlannedEffort(

    	        calculateTaskEffort(

    	                analysisTask.getDuration(),

    	                schedule

    	        )

    	);
    	
    	analysisResponse.setActualEffort(

    	        calculateTaskEffort(

    	                analysisTask.getDuration(),

    	                schedule

    	        )

    	);
    	
    	analysisResponse.setProductivity(

    	        calculateProductivity(

    	                analysis.getUcp(),

    	                analysisResponse.getActualEffort()

    	        )

    	);
    	
    	analysisResponse.setEffortVariance(

    	        calculateEffortVariance(

    	                analysisResponse.getPlannedEffort(),

    	                analysisResponse.getActualEffort()

    	        )

    	);
    	
    	analysisResponse.setEffortInAnalysis(null);

    	analysisResponse.setReviewDefects(null);

    	analysisResponse.setReviewEffort(null);
    	
    	analysisResponse.setDefectDensity(null);

    	analysisResponse.setDefectDetectionRate(null);

    	analysisResponse.setDefectRate(null);
    	
    	ProjectScheduleTask designTask =
    	        getDesignTask(schedule);
    	
    	designResponse.setPlannedDuration(
    	        designTask.getDuration()
    	);

    	designResponse.setActualDuration(
    	        designTask.getDuration()
    	);

    	designResponse.setScheduleVariance(
    	        calculateScheduleVariance(
    	                designResponse.getPlannedDuration(),
    	                designResponse.getActualDuration()
    	        )
    	);

    	designResponse.setPlannedEffort(
    	        calculateTaskEffort(
    	                designTask.getDuration(),
    	                schedule
    	        )
    	);

    	designResponse.setActualEffort(
    	        calculateTaskEffort(
    	                designTask.getDuration(),
    	                schedule
    	        )
    	);

    	designResponse.setProductivity(
    	        calculateProductivity(
    	                analysis.getUcp(),
    	                designResponse.getActualEffort()
    	        )
    	);

    	designResponse.setEffortVariance(
    	        calculateEffortVariance(
    	                designResponse.getPlannedEffort(),
    	                designResponse.getActualEffort()
    	        )
    	);
    	
    	designResponse.setEffortInAnalysis(null);

    	designResponse.setReviewDefects(null);

    	designResponse.setReviewEffort(null);

    	designResponse.setDefectDensity(null);

    	designResponse.setDefectDetectionRate(null);

    	designResponse.setDefectRate(null);
    	
    	codingResponse.setPlannedDuration(
    	        getCodingTaskDuration(schedule)
    	);
    	
    	codingResponse.setActualDuration(
    	        getCodingTaskDuration(schedule)
    	);
    	
    	codingResponse.setScheduleVariance(
    	        calculateScheduleVariance(
    	                codingResponse.getPlannedDuration(),
    	                codingResponse.getActualDuration()
    	        )
    	);
    	
    	codingResponse.setPlannedEffort(
    	        calculateCodingPlannedEffort(schedule)
    	);
    	
    	codingResponse.setActualEffort(
    	        calculateCodingPlannedEffort(schedule)
    	);
    	
    	codingResponse.setCodingEffort(
    	        calculateBreakdownEffort(
    	                schedule,
    	                "Coding")
    	);
    	
    	codingResponse.setCodeReviewEffort(
    	        calculateBreakdownEffort(
    	                schedule,
    	                "Code Review")
    	);
    	
    	codingResponse.setUnitTestingEffort(
    	        calculateBreakdownEffort(
    	                schedule,
    	                "Unit Testing")
    	);
    	
    	codingResponse.setProductivity(

    	        calculateProductivity(

    	                analysis.getUcp(),

    	                codingResponse.getActualEffort()

    	        )

    	);
    	
    	codingResponse.setEffortVariance(

    	        calculateEffortVariance(

    	                codingResponse.getPlannedEffort(),

    	                codingResponse.getActualEffort()

    	        )

    	);
    	
    	ProjectScheduleTask sitTask =
    	        getSitTask(schedule);
    	
    	sitResponse.setPlannedDuration(
    	        sitTask.getDuration()
    	);
    	
    	sitResponse.setActualDuration(
    	        sitTask.getDuration()
    	);
    	
    	sitResponse.setScheduleVariance(

    	        calculateScheduleVariance(

    	                sitResponse.getPlannedDuration(),

    	                sitResponse.getActualDuration()

    	        )

    	);
    	
    	sitResponse.setPlannedEffort(

    	        calculateTaskEffort(

    	                sitTask.getDuration(),

    	                schedule

    	        )

    	);
    	
    	sitResponse.setActualEffort(

    	        calculateTaskEffort(

    	                sitTask.getDuration(),

    	                schedule

    	        )

    	);
    	
    	sitResponse.setEffortVariance(

    	        calculateEffortVariance(

    	                sitResponse.getPlannedEffort(),

    	                sitResponse.getActualEffort()

    	        )

    	);
    	
    	sitResponse.setTestExecutionEffort(
    	        calculateBreakdownEffort(
    	                schedule,
    	                "Testing"
    	        )
    	);
    	
    	sitResponse.setSitEffort(
    	        calculateSitEffort(schedule)
    	);
    	
    	List<SprintMetricsResponse> sprintMetrics =
    	        calculateSprintMetrics(
    	                schedule,
    	                analysis.getUcp(),
    	                analysisResponse,
    	                designResponse,
    	                codingResponse,
    	                sitResponse,
    	                otherActivityMetricsResponse
    	        );
    	
    	ProjectMetricsResponse response =
    	        new ProjectMetricsResponse();

    	response.setSummary(summary);
    	response.setQuality(qualityResponse);

    	// Overall / cumulative metrics
    	response.setAnalysis(analysisResponse);
    	response.setDesign(designResponse);
    	response.setCoding(codingResponse);
    	response.setSit(sitResponse);
    	response.setOtherActivity(otherActivityMetricsResponse);

    	// Individual coding sprint metrics
    	response.setSprints(sprintMetrics);

    	return response;
    }
    
    private double calculatePlannedEffort(
            ProjectSchedule schedule) {

        return schedule.getEstimatedHours();
    }
    
    private int calculateActualDuration(
            ProjectSchedule schedule) {

        return schedule.getTasks()
                .stream()
                .map(ProjectScheduleTask::getDuration)
                .reduce(0, Integer::sum);
    }
    
    private int calculatePlannedDuration(
            ProjectSchedule schedule) {

        return schedule.getDurationDays();
    }
    
    

    private Double calculateSizeVariance(
            Double originalSize,
            Double actualSize) {

        if (originalSize == null || originalSize == 0) {
            return 0.0;
        }

        return ((actualSize - originalSize) * 100)
                / originalSize;
    }
    
    private Double calculateEffortVariance(
            Double plannedEffort,
            Double actualEffort) {

        if (plannedEffort == null || plannedEffort == 0) {
            return 0.0;
        }

        return ((actualEffort - plannedEffort) * 100)
                / plannedEffort;
    }
    
    private Double calculateScheduleVariance(
            Integer plannedDuration,
            Integer actualDuration) {

        if (plannedDuration == null || plannedDuration == 0) {
            return 0.0;
        }

        return ((actualDuration - plannedDuration) * 100.0)
                / plannedDuration;
    }
    
    private Double calculateProductivity(
            Double actualSize,
            Double actualEffort) {

        if (actualEffort == null || actualEffort == 0) {
            return 0.0;
        }

        return actualSize / actualEffort;
    }
    
    private ProjectScheduleTask getAnalysisTask(
            ProjectSchedule schedule) {

        return schedule.getTasks()
                .stream()
                .filter(task ->
                        task.getTaskName()
                                .equalsIgnoreCase("Requirement Analysis"))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Requirement Analysis task not found"));
    }
    
    private Double calculateTaskEffort(
            Integer duration,
            ProjectSchedule schedule) {

        return duration
                * schedule.getWorkingHoursPerDay()
                * schedule.getTeamSize()
                * 1.0;
    }
    
    private ProjectScheduleTask getDesignTask(
            ProjectSchedule schedule) {

        return schedule.getTasks()
                .stream()
                .filter(task ->
                        task.getTaskName()
                                .equalsIgnoreCase("Solution Design"))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Solution Design task not found"));
    }
    
    private Integer getBreakdownDuration(
            ProjectSchedule schedule,
            String activityName) {

        return schedule.getTasks()
                .stream()
                .flatMap(task -> task.getTaskBreakdowns().stream())
                .filter(b ->
                        b.getActivityName()
                                .equalsIgnoreCase(activityName))
                .map(ProjectScheduleTaskBreakdown::getDuration)
                .reduce(0, Integer::sum);
    }
    
    private Integer getCodingTaskDuration(
            ProjectSchedule schedule) {

        return schedule.getTasks()
                .stream()
                .filter(task ->
                        task.getTaskBreakdowns()
                                .stream()
                                .anyMatch(b ->
                                        b.getActivityName()
                                                .equalsIgnoreCase("Coding")))
                .map(ProjectScheduleTask::getDuration)
                .reduce(0, Integer::sum);
    }
    
    private List<ProjectScheduleTask> getCodingTasks(
            ProjectSchedule schedule) {

        return schedule.getTasks()
                .stream()
                .filter(task ->
                        task.getTaskBreakdowns()
                                .stream()
                                .anyMatch(b ->
                                        b.getActivityName()
                                                .equalsIgnoreCase("Coding")))
                .toList();
    }
    
    private double calculateSprintRatio(
            ProjectScheduleTask codingTask,
            int totalCodingDuration) {

        if (totalCodingDuration == 0) {
            return 0.0;
        }

        return (double) codingTask.getDuration()
                / totalCodingDuration;
    }
    
    private Double proportionalValue(
            Double totalValue,
            double ratio) {

        if (totalValue == null) {
            return null;
        }

        return totalValue * ratio;
    }
    
    private Integer proportionalInteger(
            Integer totalValue,
            double ratio) {

        if (totalValue == null) {
            return null;
        }

        return (int) Math.round(totalValue * ratio);
    }
    
    private DesignMetricsResponse calculateSprintDesign(
            DesignMetricsResponse overall,
            Double ucp,
            double ratio) {

        DesignMetricsResponse sprint =
                new DesignMetricsResponse();

        sprint.setPlannedDuration(
                proportionalDuration(
                        overall.getPlannedDuration(),
                        ratio
                )
        );

        sprint.setActualDuration(
                proportionalDuration(
                        overall.getActualDuration(),
                        ratio
                )
        );

        sprint.setScheduleVariance(
                calculateScheduleVariance(
                        sprint.getPlannedDuration(),
                        sprint.getActualDuration()
                )
        );

        sprint.setPlannedEffort(
                proportionalValue(
                        overall.getPlannedEffort(),
                        ratio
                )
        );

        sprint.setActualEffort(
                proportionalValue(
                        overall.getActualEffort(),
                        ratio
                )
        );

        sprint.setProductivity(
                calculateProductivity(
                        proportionalValue(ucp, ratio),
                        sprint.getActualEffort()
                )
        );

        sprint.setEffortVariance(
                calculateEffortVariance(
                        sprint.getPlannedEffort(),
                        sprint.getActualEffort()
                )
        );

        sprint.setEffortInAnalysis(
                proportionalValue(
                        overall.getEffortInAnalysis(),
                        ratio
                )
        );

        sprint.setReviewDefects(
                proportionalInteger(
                        overall.getReviewDefects(),
                        ratio
                )
        );

        sprint.setReviewEffort(
                proportionalValue(
                        overall.getReviewEffort(),
                        ratio
                )
        );

        sprint.setDefectDensity(null);
        sprint.setDefectDetectionRate(null);
        sprint.setDefectRate(null);

        return sprint;
    }
    
    private CodingMetricsResponse calculateSprintCoding(
            ProjectScheduleTask codingTask,
            ProjectSchedule schedule,
            Double ucp) {

        CodingMetricsResponse sprint =
                new CodingMetricsResponse();

        int duration =
                codingTask.getDuration();

        sprint.setPlannedDuration(duration);

        sprint.setActualDuration(duration);

        sprint.setScheduleVariance(
                calculateScheduleVariance(
                        duration,
                        duration
                )
        );

        Double plannedEffort =
                calculateTaskEffort(
                        duration,
                        schedule
                );

        sprint.setPlannedEffort(plannedEffort);

        sprint.setActualEffort(plannedEffort);

        sprint.setCodingEffort(
                calculateTaskBreakdownEffort(
                        codingTask,
                        "Coding",
                        schedule
                )
        );

        sprint.setCodeReviewEffort(
                calculateTaskBreakdownEffort(
                        codingTask,
                        "Code Review",
                        schedule
                )
        );

        sprint.setUnitTestingEffort(
                calculateTaskBreakdownEffort(
                        codingTask,
                        "Unit Testing",
                        schedule
                )
        );

        sprint.setProductivity(
                calculateProductivity(
                        ucp,
                        sprint.getCodingEffort()
                )
        );

        sprint.setEffortVariance(
                calculateEffortVariance(
                        sprint.getPlannedEffort(),
                        sprint.getActualEffort()
                )
        );

        return sprint;
    }
    
    private Double calculateTaskBreakdownEffort(
            ProjectScheduleTask task,
            String activityName,
            ProjectSchedule schedule) {

        Integer duration =
                task.getTaskBreakdowns()
                        .stream()
                        .filter(b ->
                                b.getActivityName()
                                        .equalsIgnoreCase(activityName))
                        .map(ProjectScheduleTaskBreakdown::getDuration)
                        .reduce(0, Integer::sum);

        return duration
                * schedule.getWorkingHoursPerDay()
                * schedule.getTeamSize()
                * 1.0;
    }
    
    private SitMetricsResponse calculateSprintSit(
            SitMetricsResponse overall,
            double ratio) {

        SitMetricsResponse sprint =
                new SitMetricsResponse();

        sprint.setPlannedDuration(
                proportionalDuration(
                        overall.getPlannedDuration(),
                        ratio
                )
        );

        sprint.setActualDuration(
                proportionalDuration(
                        overall.getActualDuration(),
                        ratio
                )
        );

        sprint.setScheduleVariance(
                calculateScheduleVariance(
                        sprint.getPlannedDuration(),
                        sprint.getActualDuration()
                )
        );

        sprint.setPlannedEffort(
                proportionalValue(
                        overall.getPlannedEffort(),
                        ratio
                )
        );

        sprint.setActualEffort(
                proportionalValue(
                        overall.getActualEffort(),
                        ratio
                )
        );

        sprint.setEffortVariance(
                calculateEffortVariance(
                        sprint.getPlannedEffort(),
                        sprint.getActualEffort()
                )
        );

        sprint.setTotalTestConditions(
                proportionalInteger(
                        overall.getTotalTestConditions(),
                        ratio
                )
        );

        sprint.setTestCaseWritingEffort(
                proportionalValue(
                        overall.getTestCaseWritingEffort(),
                        ratio
                )
        );

        sprint.setTestCaseReviewDefects(
                proportionalInteger(
                        overall.getTestCaseReviewDefects(),
                        ratio
                )
        );

        sprint.setTestCaseReviewEffort(
                proportionalValue(
                        overall.getTestCaseReviewEffort(),
                        ratio
                )
        );

        sprint.setTestExecutionEffort(
                proportionalValue(
                        overall.getTestExecutionEffort(),
                        ratio
                )
        );

        sprint.setTestCaseReviewDetectionRate(
                overall.getTestCaseReviewDetectionRate()
        );

        sprint.setSitDefects(
                proportionalInteger(
                        overall.getSitDefects(),
                        ratio
                )
        );

        sprint.setSitEffort(
                proportionalValue(
                        overall.getSitEffort(),
                        ratio
                )
        );

        sprint.setSitDetectionRate(
                overall.getSitDetectionRate()
        );

        return sprint;
    }
    
    private OtherActivityMetricsResponse calculateSprintOtherActivity(
            OtherActivityMetricsResponse overall,
            double ratio) {

        OtherActivityMetricsResponse sprint =
                new OtherActivityMetricsResponse();

        sprint.setActualTotal(
                proportionalValue(
                        overall.getActualTotal(),
                        ratio
                )
        );

        sprint.setActualProjectManagement(
                proportionalValue(
                        overall.getActualProjectManagement(),
                        ratio
                )
        );

        sprint.setActualSupportGroup(
                proportionalValue(
                        overall.getActualSupportGroup(),
                        ratio
                )
        );

        sprint.setActualOthers(
                proportionalValue(
                        overall.getActualOthers(),
                        ratio
                )
        );

        sprint.setPlannedTotal(
                proportionalValue(
                        overall.getPlannedTotal(),
                        ratio
                )
        );

        sprint.setPlannedProjectManagement(
                proportionalValue(
                        overall.getPlannedProjectManagement(),
                        ratio
                )
        );

        sprint.setPlannedSupportGroup(
                proportionalValue(
                        overall.getPlannedSupportGroup(),
                        ratio
                )
        );

        sprint.setPlannedOthers(
                proportionalValue(
                        overall.getPlannedOthers(),
                        ratio
                )
        );

        return sprint;
    }
    private Integer proportionalDuration(
            Integer totalDuration,
            double ratio) {

        if (totalDuration == null) {
            return 0;
        }

        return (int) Math.round(
                totalDuration * ratio
        );
    }
    
    private List<SprintMetricsResponse> calculateSprintMetrics(
            ProjectSchedule schedule,
            Double analysisUcp,
            AnalysisMetricsResponse overallAnalysis,
            DesignMetricsResponse overallDesign,
            CodingMetricsResponse overallCoding,
            SitMetricsResponse overallSit,
            OtherActivityMetricsResponse overallOther) {

        List<ProjectScheduleTask> codingTasks =
                getCodingTasks(schedule);

        List<SprintMetricsResponse> sprints =
                new ArrayList<>();

        if (codingTasks.isEmpty()) {
            return sprints;
        }

        int totalCodingDuration =
                codingTasks.stream()
                        .mapToInt(ProjectScheduleTask::getDuration)
                        .sum();

        for (int i = 0; i < codingTasks.size(); i++) {

            ProjectScheduleTask codingTask =
                    codingTasks.get(i);

            double ratio =
                    calculateSprintRatio(
                            codingTask,
                            totalCodingDuration
                    );

            SprintMetricsResponse sprint =
                    new SprintMetricsResponse();

            sprint.setSprintNumber(i + 1);

            sprint.setTaskName(
                    codingTask.getTaskName()
            );

            sprint.setAnalysis(
                    calculateSprintAnalysis(
                            overallAnalysis,
                            analysisUcp,
                            ratio
                    )
            );

            sprint.setDesign(
                    calculateSprintDesign(
                            overallDesign,
                            analysisUcp,
                            ratio
                    )
            );

            sprint.setCoding(
                    calculateSprintCoding(
                            codingTask,
                            schedule,
                            analysisUcp
                    )
            );

            sprint.setSit(
                    calculateSprintSit(
                            overallSit,
                            ratio
                    )
            );

            sprint.setOtherActivity(
                    calculateSprintOtherActivity(
                            overallOther,
                            ratio
                    )
            );

            sprints.add(sprint);
        }

        return sprints;
    }
    
    private AnalysisMetricsResponse calculateSprintAnalysis(
            AnalysisMetricsResponse overall,
            Double ucp,
            double ratio) {

        AnalysisMetricsResponse sprint =
                new AnalysisMetricsResponse();

        sprint.setPlannedDuration(
                proportionalDuration(
                        overall.getPlannedDuration(),
                        ratio
                )
        );

        sprint.setActualDuration(
                proportionalDuration(
                        overall.getActualDuration(),
                        ratio
                )
        );

        sprint.setScheduleVariance(
                calculateScheduleVariance(
                        sprint.getPlannedDuration(),
                        sprint.getActualDuration()
                )
        );

        sprint.setPlannedEffort(
                proportionalValue(
                        overall.getPlannedEffort(),
                        ratio
                )
        );

        sprint.setActualEffort(
                proportionalValue(
                        overall.getActualEffort(),
                        ratio
                )
        );

        sprint.setProductivity(
                calculateProductivity(
                        proportionalValue(ucp, ratio),
                        sprint.getActualEffort()
                )
        );

        sprint.setEffortVariance(
                calculateEffortVariance(
                        sprint.getPlannedEffort(),
                        sprint.getActualEffort()
                )
        );

        sprint.setEffortInAnalysis(
                proportionalValue(
                        overall.getEffortInAnalysis(),
                        ratio
                )
        );

        sprint.setReviewDefects(
                proportionalInteger(
                        overall.getReviewDefects(),
                        ratio
                )
        );

        sprint.setReviewEffort(
                proportionalValue(
                        overall.getReviewEffort(),
                        ratio
                )
        );
        
        sprint.setDefectDensity(null);
        sprint.setDefectDetectionRate(null);
        sprint.setDefectRate(null);

        return sprint;
    }
    private Double calculateCodingPlannedEffort(
            ProjectSchedule schedule) {

        return schedule.getTasks()
                .stream()
                .filter(task ->
                        task.getTaskBreakdowns()
                                .stream()
                                .anyMatch(b ->
                                        b.getActivityName()
                                                .equalsIgnoreCase("Coding")))
                .mapToDouble(task ->
                        calculateTaskEffort(
                                task.getDuration(),
                                schedule))
                .sum();
    }
    
    private Double calculateBreakdownEffort(
            ProjectSchedule schedule,
            String activityName) {

        return getBreakdownDuration(schedule, activityName)
                * schedule.getWorkingHoursPerDay()
                * schedule.getTeamSize()
                * 1.0;
    }
    
    private ProjectScheduleTask getSitTask(
            ProjectSchedule schedule) {

        return schedule.getTasks()
                .stream()
                .filter(task ->
                        task.getTaskName()
                                .equalsIgnoreCase("System Integration Testing"))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "System Integration Testing task not found"));
    }
    
    private Double calculateSitEffort(
            ProjectSchedule schedule) {

        return calculateBreakdownEffort(schedule, "Testing")
                + calculateBreakdownEffort(schedule, "Debugging");
    }
    
    private ProjectScheduleTask getUatTask(
            ProjectSchedule schedule) {

        return schedule.getTasks()
                .stream()
                .filter(task -> {

                    String taskName =
                            task.getTaskName() == null
                                    ? ""
                                    : task.getTaskName()
                                            .trim()
                                            .toLowerCase();

                    return taskName.contains("user acceptance testing")
                            || taskName.equals("uat")
                            || taskName.contains("user acceptance test");
                })
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User Acceptance Testing task not found"));
    }
    
    private Integer getActualTaskDuration(
            ProjectScheduleTask task) {

        if (task.getActualStartDate() == null
                || task.getActualEndDate() == null) {

            return task.getDuration();
        }

        return (int) (
                java.time.temporal.ChronoUnit.DAYS.between(
                        task.getActualStartDate(),
                        task.getActualEndDate()
                ) + 1
        );
    }
    private boolean isPrimaryActivity(String taskName) {

        return taskName.contains("requirement analysis")
                || taskName.contains("analysis")
                || taskName.contains("solution design")
                || taskName.contains("design")
                || taskName.contains("coding")
                || taskName.contains("development")
                || taskName.contains("system integration testing")
                || taskName.equals("sit")
                || taskName.contains("user acceptance testing")
                || taskName.equals("uat");
    }
    
    private OtherActivityMetricsResponse calculateOtherActivity(
            ProjectSchedule schedule) {

        double plannedProjectManagement = 0.0;
        double plannedSupportGroup = 0.0;
        double plannedOthers = 0.0;

        double actualProjectManagement = 0.0;
        double actualSupportGroup = 0.0;
        double actualOthers = 0.0;

        for (ProjectScheduleTask task : schedule.getTasks()) {

            String taskName =
                    task.getTaskName() == null
                            ? ""
                            : task.getTaskName().trim().toLowerCase();

            /*
             * These are already represented by their own
             * metrics and must NOT be counted as Other Activity.
             */
            if (isPrimaryActivity(taskName)) {
                continue;
            }

            double plannedEffort =
                    calculateTaskEffort(
                            task.getDuration(),
                            schedule
                    );

            double actualEffort = plannedEffort;

            if (taskName.contains("project management")
                    || taskName.equals("pm")) {

                plannedProjectManagement += plannedEffort;
                actualProjectManagement += actualEffort;

            } else if (taskName.contains("support group")
                    || taskName.contains("support")) {

                plannedSupportGroup += plannedEffort;
                actualSupportGroup += actualEffort;

            } else {

                plannedOthers += plannedEffort;
                actualOthers += actualEffort;
            }
        }

        OtherActivityMetricsResponse response =
                new OtherActivityMetricsResponse();

        response.setPlannedProjectManagement(
                plannedProjectManagement
        );

        response.setPlannedSupportGroup(
                plannedSupportGroup
        );

        response.setPlannedOthers(
                plannedOthers
        );

        response.setPlannedTotal(
                plannedProjectManagement
                        + plannedSupportGroup
                        + plannedOthers
        );

        response.setActualProjectManagement(
                actualProjectManagement
        );

        response.setActualSupportGroup(
                actualSupportGroup
        );

        response.setActualOthers(
                actualOthers
        );

        response.setActualTotal(
                actualProjectManagement
                        + actualSupportGroup
                        + actualOthers
        );

        return response;
    }
}