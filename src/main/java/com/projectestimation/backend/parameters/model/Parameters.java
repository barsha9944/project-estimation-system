package com.projectestimation.backend.parameters.model;

import com.projectestimation.backend.opportunity.model.Opportunity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "opportunity_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parameters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false, unique = true)
    private Opportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplexityLevel complexity;

    @Column(nullable = false)
    private Double riskFactor;

    @Column(nullable = false)
    private Double productivityFactor;

    @Column(nullable = false)
    private Double hourlyRate;

    @Column(nullable = false)
    private Integer teamSize;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
