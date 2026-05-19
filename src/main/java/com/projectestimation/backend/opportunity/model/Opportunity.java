package com.projectestimation.backend.opportunity.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "opportunities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImplementationType implementationType;

    @ElementCollection
    @CollectionTable(name = "opportunity_platforms", joinColumns = @JoinColumn(name = "opportunity_id"))
    @Column(name = "platform", nullable = false)
    @Builder.Default
    private List<String> platforms = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "opportunity_technology_categories", joinColumns = @JoinColumn(name = "opportunity_id"))
    @Column(name = "technology_category", nullable = false)
    @Builder.Default
    private List<String> technologyCategories = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "opportunity_enterprise_contexts", joinColumns = @JoinColumn(name = "opportunity_id"))
    @Column(name = "enterprise_context", nullable = false)
    @Builder.Default
    private List<String> enterpriseContexts = new ArrayList<>();

    @Column(nullable = false)
    private String opportunityName;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false, length = 5000)
    private String requirementSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    private LocalDate expectedDeliveryDate;

    @ElementCollection
    @CollectionTable(name = "opportunity_components", joinColumns = @JoinColumn(name = "opportunity_id"))
    @Column(name = "component", nullable = false)
    @Builder.Default
    private List<String> components = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OpportunityStatus status = OpportunityStatus.NEW;

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
            status = OpportunityStatus.NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
