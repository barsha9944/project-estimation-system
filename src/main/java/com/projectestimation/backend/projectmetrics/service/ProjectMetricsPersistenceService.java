package com.projectestimation.backend.projectmetrics.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SprintMetricsResponse;
import com.projectestimation.backend.projectmetrics.model.ProjectMetrics;
import com.projectestimation.backend.projectmetrics.model.ProjectMetricsSprint;
import com.projectestimation.backend.projectmetrics.repository.ProjectMetricsRepository;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleTaskBreakdownRepository;

@Service
public class ProjectMetricsPersistenceService {

    private final ProjectMetricsRepository projectMetricsRepository;
    private final OpportunityRepository opportunityRepository;
    private final ProjectScheduleRepository projectScheduleRepository;
    private final ProjectScheduleTaskBreakdownRepository breakdownRepository;

    public ProjectMetricsPersistenceService(
            ProjectMetricsRepository projectMetricsRepository,
            OpportunityRepository opportunityRepository,
            ProjectScheduleRepository projectScheduleRepository,
            ProjectScheduleTaskBreakdownRepository breakdownRepository) {

        this.projectMetricsRepository = projectMetricsRepository;
        this.opportunityRepository = opportunityRepository;
        this.projectScheduleRepository = projectScheduleRepository;
        this.breakdownRepository = breakdownRepository;
    }

    @Transactional
    public void saveMetrics(
            Long opportunityId,
            ProjectMetricsResponse response) {

        Opportunity opportunity = opportunityRepository
                .findById(opportunityId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Opportunity not found: " + opportunityId));

//        ProjectSchedule schedule = projectScheduleRepository
//        		.findByOpportunityIdWithTasks(opportunityId)
//                .orElseThrow(() ->
//                        new RuntimeException(
//                                "Project schedule not found for opportunity: "
//                                        + opportunityId));

        /*
         * If metrics already exist for this opportunity,
         * update them instead of creating duplicates.
         */
        ProjectMetrics metrics = projectMetricsRepository
                .findByOpportunity_Id(opportunityId)
                .orElseGet(ProjectMetrics::new);

        metrics.setOpportunity(opportunity);

        mapSummaryAndQuality(metrics, response);

        /*
         * Remove old sprint rows.
         * orphanRemoval=true will delete them from DB.
         */
        metrics.getSprints().clear();

        List<ProjectScheduleTask> codingTasks =
                breakdownRepository
                        .findByProjectScheduleTaskProjectScheduleOpportunityId(
                                opportunityId)
                        .stream()
                        .filter(breakdown ->
                                "Coding".equalsIgnoreCase(
                                        breakdown.getActivityName()))
                        .map(ProjectScheduleTaskBreakdown::getProjectScheduleTask)
                        .distinct()
                        .sorted((task1, task2) ->
                                Integer.compare(
                                        task1.getSequence(),
                                        task2.getSequence()))
                        .toList();

        if (codingTasks.size() != response.getSprints().size()) {
            throw new IllegalStateException(
                    "Coding task count does not match sprint metrics count. "
                    + "Coding tasks: " + codingTasks.size()
                    + ", sprint metrics: " + response.getSprints().size());
        }

        for (int i = 0; i < response.getSprints().size(); i++) {

            SprintMetricsResponse sprintResponse =
                    response.getSprints().get(i);

            ProjectScheduleTask codingTask =
                    codingTasks.get(i);

            ProjectMetricsSprint sprint =
                    createSprintEntity(
                            sprintResponse,
                            codingTask);

            metrics.addSprint(sprint);
        }

        projectMetricsRepository.save(metrics);
    }
    
    private void mapSummaryAndQuality(
            ProjectMetrics metrics,
            ProjectMetricsResponse response) {

        if (response.getSummary() != null) {

            var summary = response.getSummary();

            metrics.setProjectName(summary.getProjectName());
            metrics.setReleaseNo(summary.getReleaseNo());
            metrics.setOriginalSize(summary.getOriginalSize());
            metrics.setActualSize(summary.getActualSize());
            metrics.setSizeVariance(summary.getSizeVariance());

            metrics.setTotalPlannedEffortWithoutPm(
                    summary.getTotalPlannedEffortWithoutPm());

            metrics.setTotalPlannedEffort(
                    summary.getTotalPlannedEffort());

            metrics.setTotalActualEffortWithoutPm(
                    summary.getTotalActualEffortWithoutPm());

            metrics.setTotalActualEffort(
                    summary.getTotalActualEffort());

            metrics.setEffortVariance(
                    summary.getEffortVariance());

            metrics.setPlannedDuration(
                    summary.getPlannedDuration());

            metrics.setActualDuration(
                    summary.getActualDuration());

            metrics.setScheduleVariance(
                    summary.getScheduleVariance());

            metrics.setActualOverallProductivity(
                    summary.getActualOverallProductivity());

            metrics.setReviewEffectiveness(
                    summary.getReviewEffectiveness());

            metrics.setTestingEffectiveness(
                    summary.getTestingEffectiveness());
        }

        if (response.getQuality() != null) {

            var quality = response.getQuality();

            metrics.setAveragePreDeliveryDefectDensity(
                    quality.getAveragePreDeliveryDefectDensity());

            metrics.setUatDefects(
                    quality.getUatDefects());

            metrics.setPostDeliveryDefectDensity(
                    quality.getPostDeliveryDefectDensity());

            metrics.setOverallDefectDensity(
                    quality.getOverallDefectDensity());

            metrics.setPlannedUatEffort(
                    quality.getPlannedUatEffort());

            metrics.setActualUatEffort(
                    quality.getActualUatEffort());

            metrics.setOverallDefectRate(
                    quality.getOverallDefectRate());

            metrics.setDefectRemovalEfficiency(
                    quality.getDefectRemovalEfficiency());
        }
    }
    
//    private List<ProjectScheduleTask> getCodingTasks(
//            ProjectSchedule schedule) {
//
//        return schedule.getTasks()
//                .stream()
//                .filter(task ->
//                        task.getTaskBreakdowns() != null
//                        && task.getTaskBreakdowns()
//                                .stream()
//                                .anyMatch(breakdown ->
//                                        "Coding".equalsIgnoreCase(
//                                                breakdown.getActivityName())))
//                .sorted((task1, task2) ->
//                        Integer.compare(
//                                task1.getSequence(),
//                                task2.getSequence()))
//                .toList();
//    }
    
    
    private ProjectMetricsSprint createSprintEntity(
            SprintMetricsResponse response,
            ProjectScheduleTask codingTask) {

        ProjectMetricsSprint sprint =
                new ProjectMetricsSprint();

        sprint.setProjectScheduleTask(codingTask);
        sprint.setSprintNumber(response.getSprintNumber());
        sprint.setTaskName(codingTask.getTaskName());

        // =========================
        // ANALYSIS
        // =========================

        if (response.getAnalysis() != null) {

            var analysis = response.getAnalysis();

            sprint.setAnalysisPlannedDuration(
                    analysis.getPlannedDuration());

            sprint.setAnalysisActualDuration(
                    analysis.getActualDuration());

            sprint.setAnalysisScheduleVariance(
                    analysis.getScheduleVariance());

            sprint.setAnalysisPlannedEffort(
                    analysis.getPlannedEffort());

            sprint.setAnalysisActualEffort(
                    analysis.getActualEffort());

            sprint.setAnalysisProductivity(
                    analysis.getProductivity());

            sprint.setAnalysisEffortVariance(
                    analysis.getEffortVariance());

            sprint.setAnalysisEffortInAnalysis(
                    analysis.getEffortInAnalysis());

            sprint.setAnalysisReviewDefects(
                    analysis.getReviewDefects());

            sprint.setAnalysisReviewEffort(
                    analysis.getReviewEffort());

            sprint.setAnalysisDefectDensity(
                    analysis.getDefectDensity());

            sprint.setAnalysisDefectDetectionRate(
                    analysis.getDefectDetectionRate());

            sprint.setAnalysisDefectRate(
                    analysis.getDefectRate());
        }

        // =========================
        // DESIGN
        // =========================

        if (response.getDesign() != null) {

            var design = response.getDesign();

            sprint.setDesignPlannedDuration(
                    design.getPlannedDuration());

            sprint.setDesignActualDuration(
                    design.getActualDuration());

            sprint.setDesignScheduleVariance(
                    design.getScheduleVariance());

            sprint.setDesignPlannedEffort(
                    design.getPlannedEffort());

            sprint.setDesignActualEffort(
                    design.getActualEffort());

            sprint.setDesignProductivity(
                    design.getProductivity());

            sprint.setDesignEffortVariance(
                    design.getEffortVariance());

            sprint.setDesignEffortInAnalysis(
                    design.getEffortInAnalysis());

            sprint.setDesignReviewDefects(
                    design.getReviewDefects());

            sprint.setDesignReviewEffort(
                    design.getReviewEffort());

            sprint.setDesignDefectDensity(
                    design.getDefectDensity());

            sprint.setDesignDefectDetectionRate(
                    design.getDefectDetectionRate());

            sprint.setDesignDefectRate(
                    design.getDefectRate());
        }

        // =========================
        // CODING
        // =========================

        if (response.getCoding() != null) {

            var coding = response.getCoding();

            sprint.setCodingPlannedDuration(
                    coding.getPlannedDuration());

            sprint.setCodingActualDuration(
                    coding.getActualDuration());

            sprint.setCodingScheduleVariance(
                    coding.getScheduleVariance());

            sprint.setCodingPlannedEffort(
                    coding.getPlannedEffort());

            sprint.setCodingActualEffort(
                    coding.getActualEffort());

            sprint.setCodingEffortVariance(
                    coding.getEffortVariance());

            sprint.setCodingEffort(
                    coding.getCodingEffort());

            sprint.setCodeReviewDefects(
                    coding.getCodeReviewDefects());

            sprint.setCodeReviewEffort(
                    coding.getCodeReviewEffort());

            sprint.setCodingDefectDensity(
                    coding.getDefectDensity());

            sprint.setCodeReviewDetectionRate(
                    coding.getCodeReviewDetectionRate());

            sprint.setUnitTestingDefects(
                    coding.getUnitTestingDefects());

            sprint.setUnitTestingEffort(
                    coding.getUnitTestingEffort());

            sprint.setUnitTestingDetectionRate(
                    coding.getUnitTestingDetectionRate());

            sprint.setCodingDefectRate(
                    coding.getDefectRate());

            sprint.setCodingProductivity(
                    coding.getProductivity());
        }
        
        // =========================
        // SIT
        // =========================

        if (response.getSit() != null) {

            var sit = response.getSit();

            sprint.setSitPlannedDuration(
                    sit.getPlannedDuration());

            sprint.setSitActualDuration(
                    sit.getActualDuration());

            sprint.setSitScheduleVariance(
                    sit.getScheduleVariance());

            sprint.setSitPlannedEffort(
                    sit.getPlannedEffort());

            sprint.setSitActualEffort(
                    sit.getActualEffort());

            sprint.setSitEffortVariance(
                    sit.getEffortVariance());

            sprint.setTotalTestConditions(
                    sit.getTotalTestConditions());

            sprint.setTestCaseWritingEffort(
                    sit.getTestCaseWritingEffort());

            sprint.setTestCaseReviewDefects(
                    sit.getTestCaseReviewDefects());

            sprint.setTestCaseReviewEffort(
                    sit.getTestCaseReviewEffort());

            sprint.setTestExecutionEffort(
                    sit.getTestExecutionEffort());

            sprint.setTestCaseReviewDetectionRate(
                    sit.getTestCaseReviewDetectionRate());

            sprint.setSitDefects(
                    sit.getSitDefects());

            sprint.setSitEffort(
                    sit.getSitEffort());

            sprint.setSitDetectionRate(
                    sit.getSitDetectionRate());
        }

        // =========================
        // OTHER ACTIVITY
        // =========================

        if (response.getOtherActivity() != null) {

            var other = response.getOtherActivity();

            sprint.setOtherActualTotal(
                    other.getActualTotal());

            sprint.setOtherActualProjectManagement(
                    other.getActualProjectManagement());

            sprint.setOtherActualSupportGroup(
                    other.getActualSupportGroup());

            sprint.setOtherActualOthers(
                    other.getActualOthers());

            sprint.setOtherPlannedTotal(
                    other.getPlannedTotal());

            sprint.setOtherPlannedProjectManagement(
                    other.getPlannedProjectManagement());

            sprint.setOtherPlannedSupportGroup(
                    other.getPlannedSupportGroup());

            sprint.setOtherPlannedOthers(
                    other.getPlannedOthers());
        }

        return sprint;
    }
}
    
    