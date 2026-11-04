
package com.projectestimation.backend.car.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "car_effectiveness_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarEffectiveness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_analysis_id", nullable = false)
    private CarAnalysis carAnalysis;

    private LocalDate reviewDate;

    private String evaluationPeriod;

    @Column(columnDefinition = "TEXT")
    private String expectedOutcome;

    @Column(columnDefinition = "TEXT")
    private String actualOutcome;

    private String beforeImplementationMetric;

    private String afterImplementationMetric;

    // EFFECTIVE, PARTIALLY_EFFECTIVE, NOT_EFFECTIVE
    private String effectivenessResult;

    @Column(columnDefinition = "TEXT")
    private String supportingEvidence;

    private String reviewedBy;

    @Column(columnDefinition = "TEXT")
    private String additionalActionsRequired;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
