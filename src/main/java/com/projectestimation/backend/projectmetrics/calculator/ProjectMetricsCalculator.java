package com.projectestimation.backend.projectmetrics.calculator;

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
import com.projectestimation.backend.projectmetrics.dto.SitMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SummaryMetricsResponse;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.proposal.model.Proposal;
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

    	Proposal proposal =
    	        proposalRepository
    	        .findFirstByOpportunity_IdOrderByVersionDesc(opportunityId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Proposal not found"));

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
        		new OtherActivityMetricsResponse();
    	
 
    	
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
    	
    	summary.setPlannedUatEffort(
    	        calculateTaskEffort(
    	                uatTask.getDuration(),
    	                schedule
    	        )
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
    	
    	ProjectMetricsResponse response =
    	        new ProjectMetricsResponse();
    	

    	response.setSummary(summary);
    	response.setAnalysis(analysisResponse);
    	response.setDesign(designResponse);
    	response.setCoding(codingResponse);
    	response.setSit(sitResponse);
    	response.setOtherActivity(otherActivityMetricsResponse);
    
//    	response.setSummary(new SummaryMetricsResponse());
//    	response.setAnalysis(new AnalysisMetricsResponse());
//    	response.setDesign(new DesignMetricsResponse());
//    	response.setCoding(new CodingMetricsResponse());
//    	response.setSit(new SitMetricsResponse());
//    	response.setOtherActivity(new OtherActivityMetricsResponse());
//    	response.setQuality(new QualityMetricsResponse());

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
                .filter(task ->
                        task.getTaskName()
                                .equalsIgnoreCase("User Acceptance Testing"))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User Acceptance Testing task not found"));
    }
}