package com.projectestimation.backend.estimation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.estimation.model.EstimationEnvironmentalFactor;

@Repository
public interface EstimationEnvironmentalFactorRepository
        extends JpaRepository<EstimationEnvironmentalFactor, Long> {

    void deleteByEstimationAnalysisId(Long estimationAnalysisId);

    List<EstimationEnvironmentalFactor> findByEstimationAnalysisId(
            Long estimationAnalysisId
    );
}