package com.projectestimation.backend.proposal.model;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.estimation.model.EstimateResult;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proposals")
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estimate_id", nullable = false)
    private EstimateResult estimateResult;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] fileContent;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public EstimateResult getEstimateResult() { return estimateResult; }
    public void setEstimateResult(EstimateResult estimateResult) { this.estimateResult = estimateResult; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public byte[] getFileContent() { return fileContent; }
    public void setFileContent(byte[] fileContent) { this.fileContent = fileContent; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public User getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(User generatedBy) { this.generatedBy = generatedBy; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}
