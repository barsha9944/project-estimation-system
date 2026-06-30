package com.projectestimation.backend.estimation.model;

import java.time.LocalDateTime;

import com.projectestimation.backend.opportunity.model.Opportunity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "estimation_analysis")
@Data
public class EstimationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    private Integer actorWeight;      // AW

    private Integer uucp;

    private Double tcf;

    private Double ef;

    private Double ucp;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    
    private Double benchmarkProductivityRatio;
    
    private Double hoursOfEffort;
}