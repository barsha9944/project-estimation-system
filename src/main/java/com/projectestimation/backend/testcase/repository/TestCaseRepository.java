package com.projectestimation.backend.testcase.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.testcase.model.TestCase;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByOpportunityId(Long opportunityId);

    void deleteByOpportunityId(Long opportunityId);
}