package com.projectestimation.backend.projectmetrics.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.projectmetrics.calculator.ProjectMetricsCalculator;
import com.projectestimation.backend.projectmetrics.dto.AnalysisMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.CodingMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.DesignMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.OtherActivityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.QualityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SitMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SprintMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SummaryMetricsResponse;
import com.projectestimation.backend.projectmetrics.model.ProjectMetrics;
import com.projectestimation.backend.projectmetrics.model.ProjectMetricsSprint;
import com.projectestimation.backend.projectmetrics.repository.ProjectMetricsRepository;

@Service
public class ProjectMetricsService {

    private final ProjectMetricsCalculator calculator;

    private final ProjectMetricsPersistenceService persistenceService;
    
    private final ProjectMetricsRepository projectMetricsRepository;

    public ProjectMetricsService(
            ProjectMetricsCalculator calculator,
            ProjectMetricsPersistenceService persistenceService,
            ProjectMetricsRepository projectMetricsRepository) {

        this.calculator = calculator;
        this.persistenceService = persistenceService;
        this.projectMetricsRepository = projectMetricsRepository;
    }

    @Transactional
    public ProjectMetricsResponse calculateMetrics(
            Long opportunityId) {

        ProjectMetricsResponse response =
                calculator.calculate(opportunityId);

        persistenceService.saveMetrics(
                opportunityId,
                response);

        return response;
    }
    
    @Transactional(readOnly = true)
    public ProjectMetricsResponse getMetrics(Long opportunityId) {

        ProjectMetrics metrics =
                projectMetricsRepository
                        .findByOpportunity_Id(opportunityId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Project metrics not found for opportunity: "
                                                + opportunityId));

        return mapEntityToResponse(metrics);
    }
    
    private ProjectMetricsResponse mapEntityToResponse(
            ProjectMetrics metrics) {

        ProjectMetricsResponse response =
                new ProjectMetricsResponse();

        // =========================
        // SUMMARY
        // =========================

        SummaryMetricsResponse summary =
                new SummaryMetricsResponse();

        summary.setProjectName(metrics.getProjectName());
        summary.setReleaseNo(metrics.getReleaseNo());
        summary.setOriginalSize(metrics.getOriginalSize());
        summary.setActualSize(metrics.getActualSize());
        summary.setSizeVariance(metrics.getSizeVariance());

        summary.setTotalPlannedEffortWithoutPm(
                metrics.getTotalPlannedEffortWithoutPm());

        summary.setTotalPlannedEffort(
                metrics.getTotalPlannedEffort());

        summary.setTotalActualEffortWithoutPm(
                metrics.getTotalActualEffortWithoutPm());

        summary.setTotalActualEffort(
                metrics.getTotalActualEffort());

        summary.setEffortVariance(
                metrics.getEffortVariance());

        summary.setPlannedDuration(
                metrics.getPlannedDuration());

        summary.setActualDuration(
                metrics.getActualDuration());

        summary.setScheduleVariance(
                metrics.getScheduleVariance());

        summary.setActualOverallProductivity(
                metrics.getActualOverallProductivity());

        summary.setReviewEffectiveness(
                metrics.getReviewEffectiveness());

        summary.setTestingEffectiveness(
                metrics.getTestingEffectiveness());

        response.setSummary(summary);

        // =========================
        // QUALITY / UAT
        // =========================

        QualityMetricsResponse quality =
                new QualityMetricsResponse();

        quality.setAveragePreDeliveryDefectDensity(
                metrics.getAveragePreDeliveryDefectDensity());

        quality.setUatDefects(
                metrics.getUatDefects());

        quality.setPostDeliveryDefectDensity(
                metrics.getPostDeliveryDefectDensity());

        quality.setOverallDefectDensity(
                metrics.getOverallDefectDensity());

        quality.setPlannedUatEffort(
                metrics.getPlannedUatEffort());

        quality.setActualUatEffort(
                metrics.getActualUatEffort());

        quality.setOverallDefectRate(
                metrics.getOverallDefectRate());

        quality.setDefectRemovalEfficiency(
                metrics.getDefectRemovalEfficiency());

        response.setQuality(quality);

        // =========================
        // SPRINTS
        // =========================

        List<SprintMetricsResponse> sprints =
                metrics.getSprints()
                        .stream()
                        .map(this::mapSprintToResponse)
                        .toList();

        response.setSprints(sprints);

        return response;
    }
    
    private SprintMetricsResponse mapSprintToResponse(
            ProjectMetricsSprint sprint) {

        SprintMetricsResponse response =
                new SprintMetricsResponse();

        // =========================
        // SPRINT NUMBER
        // =========================

        response.setSprintNumber(
                sprint.getSprintNumber()
        );

        // =========================
        // ANALYSIS
        // =========================

        AnalysisMetricsResponse analysis =
                new AnalysisMetricsResponse();

        analysis.setPlannedDuration(
                sprint.getAnalysisPlannedDuration()
        );

        analysis.setActualDuration(
                sprint.getAnalysisActualDuration()
        );

        analysis.setScheduleVariance(
                sprint.getAnalysisScheduleVariance()
        );

        analysis.setPlannedEffort(
                sprint.getAnalysisPlannedEffort()
        );

        analysis.setActualEffort(
                sprint.getAnalysisActualEffort()
        );

        analysis.setProductivity(
                sprint.getAnalysisProductivity()
        );

        analysis.setEffortVariance(
                sprint.getAnalysisEffortVariance()
        );

        analysis.setEffortInAnalysis(
                sprint.getAnalysisEffortInAnalysis()
        );

        analysis.setReviewDefects(
                sprint.getAnalysisReviewDefects()
        );

        analysis.setReviewEffort(
                sprint.getAnalysisReviewEffort()
        );

        analysis.setDefectDensity(
                sprint.getAnalysisDefectDensity()
        );

        analysis.setDefectDetectionRate(
                sprint.getAnalysisDefectDetectionRate()
        );

        analysis.setDefectRate(
                sprint.getAnalysisDefectRate()
        );

        response.setAnalysis(analysis);

        // =========================
        // DESIGN
        // =========================

        DesignMetricsResponse design =
                new DesignMetricsResponse();

        design.setPlannedDuration(
                sprint.getDesignPlannedDuration()
        );

        design.setActualDuration(
                sprint.getDesignActualDuration()
        );

        design.setScheduleVariance(
                sprint.getDesignScheduleVariance()
        );

        design.setPlannedEffort(
                sprint.getDesignPlannedEffort()
        );

        design.setActualEffort(
                sprint.getDesignActualEffort()
        );

        design.setProductivity(
                sprint.getDesignProductivity()
        );

        design.setEffortVariance(
                sprint.getDesignEffortVariance()
        );

        design.setEffortInAnalysis(
                sprint.getDesignEffortInAnalysis()
        );

        design.setReviewDefects(
                sprint.getDesignReviewDefects()
        );

        design.setReviewEffort(
                sprint.getDesignReviewEffort()
        );

        design.setDefectDensity(
                sprint.getDesignDefectDensity()
        );

        design.setDefectDetectionRate(
                sprint.getDesignDefectDetectionRate()
        );

        design.setDefectRate(
                sprint.getDesignDefectRate()
        );

        response.setDesign(design);

        // =========================
        // CODING
        // =========================

        CodingMetricsResponse coding =
                new CodingMetricsResponse();

        coding.setPlannedDuration(
                sprint.getCodingPlannedDuration()
        );

        coding.setActualDuration(
                sprint.getCodingActualDuration()
        );

        coding.setScheduleVariance(
                sprint.getCodingScheduleVariance()
        );

        coding.setPlannedEffort(
                sprint.getCodingPlannedEffort()
        );

        coding.setActualEffort(
                sprint.getCodingActualEffort()
        );

        coding.setEffortVariance(
                sprint.getCodingEffortVariance()
        );

        coding.setCodingEffort(
                sprint.getCodingEffort()
        );

        coding.setCodeReviewDefects(
                sprint.getCodeReviewDefects()
        );

        coding.setCodeReviewEffort(
                sprint.getCodeReviewEffort()
        );

        coding.setDefectDensity(
                sprint.getCodingDefectDensity()
        );

        coding.setCodeReviewDetectionRate(
                sprint.getCodeReviewDetectionRate()
        );

        coding.setUnitTestingDefects(
                sprint.getUnitTestingDefects()
        );

        coding.setUnitTestingEffort(
                sprint.getUnitTestingEffort()
        );

        coding.setUnitTestingDetectionRate(
                sprint.getUnitTestingDetectionRate()
        );

        coding.setDefectRate(
                sprint.getCodingDefectRate()
        );

        coding.setProductivity(
                sprint.getCodingProductivity()
        );

        response.setCoding(coding);

        // =========================
        // SIT
        // =========================

        SitMetricsResponse sit =
                new SitMetricsResponse();

        sit.setPlannedDuration(
                sprint.getSitPlannedDuration()
        );

        sit.setActualDuration(
                sprint.getSitActualDuration()
        );

        sit.setScheduleVariance(
                sprint.getSitScheduleVariance()
        );

        sit.setPlannedEffort(
                sprint.getSitPlannedEffort()
        );

        sit.setActualEffort(
                sprint.getSitActualEffort()
        );

        sit.setEffortVariance(
                sprint.getSitEffortVariance()
        );

        sit.setTotalTestConditions(
                sprint.getTotalTestConditions()
        );

        sit.setTestCaseWritingEffort(
                sprint.getTestCaseWritingEffort()
        );

        sit.setTestCaseReviewDefects(
                sprint.getTestCaseReviewDefects()
        );

        sit.setTestCaseReviewEffort(
                sprint.getTestCaseReviewEffort()
        );

        sit.setTestExecutionEffort(
                sprint.getTestExecutionEffort()
        );

        sit.setTestCaseReviewDetectionRate(
                sprint.getTestCaseReviewDetectionRate()
        );

        sit.setSitDefects(
                sprint.getSitDefects()
        );

        sit.setSitEffort(
                sprint.getSitEffort()
        );

        sit.setSitDetectionRate(
                sprint.getSitDetectionRate()
        );

        response.setSit(sit);

        // =========================
        // OTHER ACTIVITY
        // =========================

        OtherActivityMetricsResponse otherActivity =
                new OtherActivityMetricsResponse();

        otherActivity.setActualTotal(
                sprint.getOtherActualTotal()
        );

        otherActivity.setActualProjectManagement(
                sprint.getOtherActualProjectManagement()
        );

        otherActivity.setActualSupportGroup(
                sprint.getOtherActualSupportGroup()
        );

        otherActivity.setActualOthers(
                sprint.getOtherActualOthers()
        );

        otherActivity.setPlannedTotal(
                sprint.getOtherPlannedTotal()
        );

        otherActivity.setPlannedProjectManagement(
                sprint.getOtherPlannedProjectManagement()
        );

        otherActivity.setPlannedSupportGroup(
                sprint.getOtherPlannedSupportGroup()
        );

        otherActivity.setPlannedOthers(
                sprint.getOtherPlannedOthers()
        );

        response.setOtherActivity(otherActivity);

        return response;
    }
}