package com.projectestimation.backend.estimation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.estimation.model.EstimationActor;

@Repository
public interface EstimationActorRepository
        extends JpaRepository<EstimationActor, Long> {

    void deleteByEstimationAnalysisId(Long estimationAnalysisId);

    List<EstimationActor> findByEstimationAnalysisId(Long estimationAnalysisId);
}
