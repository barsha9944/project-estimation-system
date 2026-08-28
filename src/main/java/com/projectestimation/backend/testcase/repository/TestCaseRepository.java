package com.projectestimation.backend.testcase.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.testcase.model.TestCase;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    void deleteByOpportunityId(Long opportunityId);

    List<TestCase> findByOpportunityId(Long opportunityId);

    Optional<TestCase> findByOpportunityIdAndTestCaseId(
            Long opportunityId,
            String testCaseId
    );
}