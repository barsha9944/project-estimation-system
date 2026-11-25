
package com.projectestimation.backend.car.repository;

import com.projectestimation.backend.car.model.CarAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarAnalysisRepository
        extends JpaRepository<CarAnalysis, Long> {

    List<CarAnalysis> findByOpportunityIdOrderByCreatedAtDesc(
        Long opportunityId
    );

    List<CarAnalysis> findByStatus(String status);
    
    boolean existsByOpportunityId(Long opportunityId);
}
