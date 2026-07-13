package com.projectestimation.backend.proposal.model;

import java.time.LocalDateTime;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.enums.ProposalType;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.opportunity.model.Opportunity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

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
    @JoinColumn(name = "estimation_analysis_id")
    private EstimationAnalysis estimationAnalysis;

    @Column(nullable = false)
    private String title;

    @Column(name = "markdown_content", columnDefinition = "TEXT", nullable = false)
    private String markdownContent;

    @Column(columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "generated_doc_path")
    private String generatedDocPath;

    @Column(columnDefinition = "BYTEA")
    private byte[] fileContent;

    private String fileName;

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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "proposal_type")
    private ProposalType proposalType;
    
    @Column(columnDefinition = "TEXT")
    private String architectureHtml;

    @Column(columnDefinition = "TEXT")
    private String processFlowHtml;
    
    @Column(name = "markdown_file_path")
    private String markdownFilePath;

    @Column(name = "proposal_directory")
    private String proposalDirectory;
    
    @Column(name = "architecture_image_path")
    private String architectureImagePath;

    @Column(name = "process_flow_directory")
    private String processFlowDirectory;

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
    public EstimationAnalysis getEstimationAnalysis() { return estimationAnalysis; }
    public void setEstimationAnalysis(EstimationAnalysis estimationAnalysis) { this.estimationAnalysis = estimationAnalysis; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMarkdownContent() { return markdownContent; }
    public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public String getGeneratedDocPath() { return generatedDocPath; }
    public void setGeneratedDocPath(String generatedDocPath) { this.generatedDocPath = generatedDocPath; }
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
   

    /** @deprecated use {@link #getMarkdownContent()} */
    @Deprecated
    public String getProposalContent() { return markdownContent; }

    public ProposalType getProposalType() {
		return proposalType;
	}

	public void setProposalType(ProposalType proposalType) {
		this.proposalType = proposalType;
	}
	
	

	public String getArchitectureHtml() {
		return architectureHtml;
	}

	public void setArchitectureHtml(String architectureHtml) {
		this.architectureHtml = architectureHtml;
	}

	public String getProcessFlowHtml() {
		return processFlowHtml;
	}

	public String getMarkdownFilePath() {
		return markdownFilePath;
	}

	public void setMarkdownFilePath(String markdownFilePath) {
		this.markdownFilePath = markdownFilePath;
	}

	public String getProposalDirectory() {
		return proposalDirectory;
	}

	public void setProposalDirectory(String proposalDirectory) {
		this.proposalDirectory = proposalDirectory;
	}

	public void setProcessFlowHtml(String processFlowHtml) {
		this.processFlowHtml = processFlowHtml;
	}

	
	
	public String getArchitectureImagePath() {
		return architectureImagePath;
	}

	public void setArchitectureImagePath(String architectureImagePath) {
		this.architectureImagePath = architectureImagePath;
	}

	public String getProcessFlowDirectory() {
		return processFlowDirectory;
	}

	public void setProcessFlowDirectory(String processFlowDirectory) {
		this.processFlowDirectory = processFlowDirectory;
	}

	/** @deprecated use {@link #setMarkdownContent(String)} */
    @Deprecated
    public void setProposalContent(String proposalContent) { this.markdownContent = proposalContent; }
}
