package com.projectestimation.backend.estimation.repository;

import com.projectestimation.backend.estimation.model.EstimateResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstimateResultRepository extends JpaRepository<EstimateResult, Long> {
}
