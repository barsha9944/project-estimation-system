package com.projectestimation.backend.estimation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.estimation.model.EstimationTechnicalFactor;

@Repository
public interface EstimationTechnicalFactorRepository
        extends JpaRepository<EstimationTechnicalFactor, Long> {

    void deleteByEstimationAnalysisId(Long estimationAnalysisId);

    List<EstimationTechnicalFactor> findByEstimationAnalysisId(
            Long estimationAnalysisId
    );
}