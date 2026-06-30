package com.projectestimation.backend.estimation.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "estimation_technical_factor")
@Data
public class EstimationTechnicalFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_analysis_id", nullable = false)
    private EstimationAnalysis estimationAnalysis;

    @Column(nullable = false)
    private String factorName;

    @Column(nullable = false)
    private Double multiplier;

    @Column(nullable = false)
    private Integer magnitude;

    @Column(columnDefinition = "TEXT")
    private String description;
}