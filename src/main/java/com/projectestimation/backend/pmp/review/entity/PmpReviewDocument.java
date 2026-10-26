package com.projectestimation.backend.pmp.review.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "pmp_review_documents",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_pmp_review_document_type",
        columnNames = {"review_id", "document_type"}
    )
)
public class PmpReviewDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private PmpReview review;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(nullable = false)
    private String status = "PENDING";

    @Lob
    @Column(name = "document_content")
    private String documentContent;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(nullable = false)
    private int attempts = 0;

    public Long getId() { return id; }

    public PmpReview getReview() { return review; }
    public void setReview(PmpReview value) { review = value; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String value) {
        documentType = value;
    }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    public String getDocumentContent() { return documentContent; }
    public void setDocumentContent(String value) {
        documentContent = value;
    }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime value) {
        generatedAt = value;
    }

    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int value) { attempts = value; }
}
