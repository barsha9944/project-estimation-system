package com.projectestimation.backend.estimation.model;

import com.projectestimation.backend.auth.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "estimate_results")
public class EstimateResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirementSummary;

    @Column(nullable = false)
    private double complexityFactor;

    @Column(nullable = false)
    private double riskFactor;

    @Column(nullable = false)
    private double productivityFactor;

    @Column(nullable = false)
    private double hourlyRate;

    @Column(nullable = false)
    private int teamSize;

    @Column(nullable = false)
    private double totalEffortHours;

    @Column(nullable = false)
    private double estimatedCost;

    @Column(nullable = false)
    private double timelineWeeks;

    @Column(nullable = false)
    private double confidenceScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String breakdown;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculated_by", nullable = false)
    private User calculatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime calculatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getRequirementSummary() { return requirementSummary; }
    public void setRequirementSummary(String requirementSummary) { this.requirementSummary = requirementSummary; }
    public double getComplexityFactor() { return complexityFactor; }
    public void setComplexityFactor(double complexityFactor) { this.complexityFactor = complexityFactor; }
    public double getRiskFactor() { return riskFactor; }
    public void setRiskFactor(double riskFactor) { this.riskFactor = riskFactor; }
    public double getProductivityFactor() { return productivityFactor; }
    public void setProductivityFactor(double productivityFactor) { this.productivityFactor = productivityFactor; }
    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
    public double getTotalEffortHours() { return totalEffortHours; }
    public void setTotalEffortHours(double totalEffortHours) { this.totalEffortHours = totalEffortHours; }
    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
    public double getTimelineWeeks() { return timelineWeeks; }
    public void setTimelineWeeks(double timelineWeeks) { this.timelineWeeks = timelineWeeks; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getBreakdown() { return breakdown; }
    public void setBreakdown(String breakdown) { this.breakdown = breakdown; }
    public User getCalculatedBy() { return calculatedBy; }
    public void setCalculatedBy(User calculatedBy) { this.calculatedBy = calculatedBy; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
}
