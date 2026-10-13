package com.projectestimation.backend.dar.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "decision_analysis_resolution")
public class Dar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long opportunityId;

    @Column(nullable = false)
    private String projectPhase;

    @Column(nullable = false)
    private LocalDate meetingDate;

    @Column(nullable = false)
    private String meetingName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String invitees;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String darContent;

    private String documentName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public String getProjectPhase() {
        return projectPhase;
    }

    public LocalDate getMeetingDate() {
        return meetingDate;
    }

    public String getMeetingName() {
        return meetingName;
    }

    public String getInvitees() {
        return invitees;
    }

    public String getDarContent() {
        return darContent;
    }

    public String getDocumentName() {
        return documentName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
    }

    public void setProjectPhase(String projectPhase) {
        this.projectPhase = projectPhase;
    }

    public void setMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    public void setMeetingName(String meetingName) {
        this.meetingName = meetingName;
    }

    public void setInvitees(String invitees) {
        this.invitees = invitees;
    }

    public void setDarContent(String darContent) {
        this.darContent = darContent;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
}