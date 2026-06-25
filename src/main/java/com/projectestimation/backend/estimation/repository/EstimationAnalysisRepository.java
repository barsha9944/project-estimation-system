package com.projectestimation.backend.estimation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.estimation.model.EstimationAnalysis;

@Repository
public interface EstimationAnalysisRepository
        extends JpaRepository<EstimationAnalysis, Long> {

    Optional<EstimationAnalysis> findByOpportunityId(Long opportunityId);
}
