package com.projectestimation.backend.estimation.repository;

import com.projectestimation.backend.estimation.model.EstimateResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstimateResultRepository extends JpaRepository<EstimateResult, Long> {

    @Query("SELECT e FROM EstimateResult e WHERE e.opportunity.id = :opportunityId ORDER BY e.createdAt DESC")
    List<EstimateResult> findByOpportunityId(@Param("opportunityId") Long opportunityId);

    Optional<EstimateResult> findFirstByOpportunity_IdOrderByCreatedAtDesc(Long opportunityId);
}
