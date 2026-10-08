package com.projectestimation.backend.review.model;

import java.time.LocalDateTime;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "document_reviews",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_doc_review_opp_type", columnNames = {"opportunity_id", "document_type"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentReviewType documentType;

    @Column(name = "original_generated_at", nullable = false)
    private LocalDateTime originalGeneratedAt;

    // Cycle 1 Tracking
    @Column(name = "cycle1_scheduled_at", nullable = false)
    private LocalDateTime cycle1ScheduledAt;

    @Column(name = "cycle1_generated_at")
    private LocalDateTime cycle1GeneratedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle1_status", nullable = false, length = 30)
    @Builder.Default
    private ReviewStatus cycle1Status = ReviewStatus.PENDING;

    @Column(name = "cycle1_file_path", length = 500)
    private String cycle1FilePath;

    // Cycle 2 Tracking
    @Column(name = "cycle2_scheduled_at", nullable = false)
    private LocalDateTime cycle2ScheduledAt;

    @Column(name = "cycle2_generated_at")
    private LocalDateTime cycle2GeneratedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle2_status", nullable = false, length = 30)
    @Builder.Default
    private ReviewStatus cycle2Status = ReviewStatus.PENDING;

    @Column(name = "cycle2_file_path", length = 500)
    private String cycle2FilePath;

    // Review Data Sheet Tracking (Shared between Cycle 1 & Cycle 2)
    @Column(name = "data_sheet_scheduled_at", nullable = false)
    private LocalDateTime dataSheetScheduledAt;

    @Column(name = "data_sheet_generated_at")
    private LocalDateTime dataSheetGeneratedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_sheet_status", nullable = false, length = 30)
    @Builder.Default
    private ReviewStatus dataSheetStatus = ReviewStatus.PENDING;

    @Column(name = "data_sheet_file_path", length = 500)
    private String dataSheetFilePath;

    @Column(name = "findings_data", columnDefinition = "TEXT")
    private String findingsData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.cycle1Status == null) this.cycle1Status = ReviewStatus.PENDING;
        if (this.cycle2Status == null) this.cycle2Status = ReviewStatus.PENDING;
        if (this.dataSheetStatus == null) this.dataSheetStatus = ReviewStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
