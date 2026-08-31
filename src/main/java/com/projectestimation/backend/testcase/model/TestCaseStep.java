package com.projectestimation.backend.testcase.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_case_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Column(nullable = false)
    private Integer stepNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String stepDescription;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String expectedResult;
    
    @Column(columnDefinition = "TEXT")
    private String actualResult;

    private String testStatus;

    private String passFail;

    private String defectId;

    private String severity;

    private String defectType;

    @Column(columnDefinition = "TEXT")
    private String rootCause;

    private String phaseIntroduced;
}