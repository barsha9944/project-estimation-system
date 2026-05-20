package com.projectestimation.backend.proposal.model;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.opportunity.model.Opportunity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proposals")
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id")
    private EstimateResult estimateResult;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String proposalContent;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] fileContent;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private boolean generatedByAI = false;

    @Column(nullable = false)
    private int version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (generatedAt == null) {
            generatedAt = createdAt;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Opportunity getOpportunity() { return opportunity; }
    public void setOpportunity(Opportunity opportunity) { this.opportunity = opportunity; }
    public EstimateResult getEstimateResult() { return estimateResult; }
    public void setEstimateResult(EstimateResult estimateResult) { this.estimateResult = estimateResult; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getProposalContent() { return proposalContent; }
    public void setProposalContent(String proposalContent) { this.proposalContent = proposalContent; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public byte[] getFileContent() { return fileContent; }
    public void setFileContent(byte[] fileContent) { this.fileContent = fileContent; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public boolean isGeneratedByAI() { return generatedByAI; }
    public void setGeneratedByAI(boolean generatedByAI) { this.generatedByAI = generatedByAI; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public User getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(User generatedBy) { this.generatedBy = generatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}
