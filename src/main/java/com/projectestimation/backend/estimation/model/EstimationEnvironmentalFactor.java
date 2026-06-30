package com.projectestimation.backend.estimation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "estimation_environmental_factor")
@Data
public class EstimationEnvironmentalFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimation_analysis_id")
    private EstimationAnalysis estimationAnalysis;

    private String factorName;

    private Double multiplier;

    private Integer magnitude;

    @Column(columnDefinition = "TEXT")
    private String description;
}