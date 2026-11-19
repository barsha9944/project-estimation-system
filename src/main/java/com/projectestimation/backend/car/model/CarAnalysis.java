
package com.projectestimation.backend.car.model;

import com.projectestimation.backend.opportunity.model.Opportunity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "car_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    // POSITIVE or NEGATIVE
    @Column(nullable = false)
    private String carType;

    private String triggerForCar;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String problemStatement;

    private String originatorName;

    @Column(columnDefinition = "TEXT")
    private String performanceMonitoringDetails;

    // FIVE_WHYS, FISHBONE, PARETO, HISTOGRAM, COMBINATION
    private String causalAnalysisTechnique;

    @Column(columnDefinition = "TEXT")
    private String rootCauseSummary;

    @Column(columnDefinition = "TEXT")
    private String fiveWhysDetails;

    @Column(columnDefinition = "TEXT")
    private String fishboneAnalysis;

    @Column(columnDefinition = "TEXT")
    private String paretoAnalysis;

    @Column(columnDefinition = "TEXT")
    private String histogramAnalysis;

    @Column(columnDefinition = "TEXT")
    private String actionProposedSummary;

    @Column(columnDefinition = "TEXT")
    private String lessonsLearned;

    // DRAFT, OPEN, IN_PROGRESS, UNDER_REVIEW, CLOSED
    @Builder.Default
    private String status = "DRAFT";

    @OneToMany(
        mappedBy = "carAnalysis",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<CarAction> actions = new ArrayList<>();

    @OneToMany(
        mappedBy = "carAnalysis",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<CarEffectiveness> effectivenessReviews =
        new ArrayList<>();

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "file_last_modified")
    private LocalDateTime fileLastModified;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = "DRAFT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
