package com.projectestimation.backend.estimation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.estimation.model.EstimationUseCase;

@Repository
public interface EstimationUseCaseRepository
        extends JpaRepository<EstimationUseCase, Long> {

    void deleteByEstimationAnalysisId(Long estimationAnalysisId);

    List<EstimationUseCase> findByEstimationAnalysisId(Long estimationAnalysisId);
}