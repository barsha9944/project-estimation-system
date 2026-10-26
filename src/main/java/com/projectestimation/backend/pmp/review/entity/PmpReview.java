package com.projectestimation.backend.pmp.review.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "pmp_reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_pmp_review_opportunity",
        columnNames = "opportunity_id"
    )
)
public class PmpReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opportunity_id", nullable = false)
    private Long opportunityId;

    @Column(name = "pmp_generated_at", nullable = false)
    private LocalDateTime pmpGeneratedAt;

    @Column(name = "review_documents_due_at", nullable = false)
    private LocalDateTime reviewDocumentsDueAt;

    @Column(name = "follow_up_due_at")
    private LocalDateTime followUpDueAt;

    @Column(nullable = false)
    private String status = "SCHEDULED";

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(Long value) { opportunityId = value; }

    public LocalDateTime getPmpGeneratedAt() { return pmpGeneratedAt; }
    public void setPmpGeneratedAt(LocalDateTime value) {
        pmpGeneratedAt = value;
    }

    public LocalDateTime getReviewDocumentsDueAt() {
        return reviewDocumentsDueAt;
    }
    public void setReviewDocumentsDueAt(LocalDateTime value) {
        reviewDocumentsDueAt = value;
    }

    public LocalDateTime getFollowUpDueAt() { return followUpDueAt; }
    public void setFollowUpDueAt(LocalDateTime value) {
        followUpDueAt = value;
    }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}