package com.projectestimation.backend.testcase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.testcase.model.TestCaseStep;

@Repository
public interface TestCaseStepRepository
        extends JpaRepository<TestCaseStep, Long> {
}