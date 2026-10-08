package com.projectestimation.backend.mom.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "minutes_of_meeting")
public class Mom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long opportunityId;

    @Column(nullable = false)
    private String projectName;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private String meetingType;

    @Column(nullable = false)
    private Integer meetingSequence;

    @Column(nullable = false)
    private LocalDate meetingDate;

    @Column(nullable = false)
    private String meetingName;

    private String meetingTime;

    private String meetingLocation;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String invitees;

    private String recordedBy;

    private String circulation;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String agenda;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String discussionNotes;

    private String documentName;

    private LocalDateTime createdAt;

    public Mom() {
    }

    public Long getId() {
        return id;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getClientName() {
        return clientName;
    }

    public String getMeetingType() {
        return meetingType;
    }

    public Integer getMeetingSequence() {
        return meetingSequence;
    }

    public LocalDate getMeetingDate() {
        return meetingDate;
    }

    public String getMeetingName() {
        return meetingName;
    }

    public String getMeetingTime() {
        return meetingTime;
    }

    public String getMeetingLocation() {
        return meetingLocation;
    }

    public String getInvitees() {
        return invitees;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public String getCirculation() {
        return circulation;
    }

    public String getAgenda() {
        return agenda;
    }

    public String getDiscussionNotes() {
        return discussionNotes;
    }

    public String getDocumentName() {
        return documentName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setMeetingType(String meetingType) {
        this.meetingType = meetingType;
    }

    public void setMeetingSequence(Integer meetingSequence) {
        this.meetingSequence = meetingSequence;
    }

    public void setMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    public void setMeetingName(String meetingName) {
        this.meetingName = meetingName;
    }

    public void setMeetingTime(String meetingTime) {
        this.meetingTime = meetingTime;
    }

    public void setMeetingLocation(String meetingLocation) {
        this.meetingLocation = meetingLocation;
    }

    public void setInvitees(String invitees) {
        this.invitees = invitees;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public void setCirculation(String circulation) {
        this.circulation = circulation;
    }

    public void setAgenda(String agenda) {
        this.agenda = agenda;
    }

    public void setDiscussionNotes(String discussionNotes) {
        this.discussionNotes = discussionNotes;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}