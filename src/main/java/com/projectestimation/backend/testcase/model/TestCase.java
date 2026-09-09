package com.projectestimation.backend.testcase.model;

import java.util.ArrayList;
import java.util.List;

import com.projectestimation.backend.opportunity.model.Opportunity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    @OneToMany(
            mappedBy = "testCase",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<TestCaseScenario> testCaseScenario = new ArrayList<>();

     
    

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