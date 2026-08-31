package com.projectestimation.backend.testcase.model;

import java.util.ArrayList;
import java.util.List;

import com.projectestimation.backend.opportunity.model.Opportunity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @Column(nullable = false)
    private String reqId;

    @Column(nullable = false)
    private String testCaseId;
    
    @Column
    private String phase;

    @Column(nullable = false)
    private String testCaseName;

    @Column(columnDefinition = "TEXT")
    private String testCaseDescription;

    @Column(columnDefinition = "TEXT")
    private String testData;

    @OneToMany(
            mappedBy = "testCase",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<TestCaseStep> steps = new ArrayList<>();
}