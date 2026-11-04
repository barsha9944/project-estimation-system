
package com.projectestimation.backend.car.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "car_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_analysis_id", nullable = false)
    private CarAnalysis carAnalysis;

    // CORRECTIVE or PREVENTIVE
    private String actionCategory;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String actionDescription;

    private String assignedTo;

    // LOW, MEDIUM, HIGH, CRITICAL
    private String priority;

    private LocalDate targetCompletionDate;

    @Column(columnDefinition = "TEXT")
    private String actionTaken;

    private LocalDate implementationDate;

    @Column(columnDefinition = "TEXT")
    private String evidenceReference;

    // PENDING, IN_PROGRESS, COMPLETED, OVERDUE
    @Builder.Default
    private String status = "PENDING";

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
