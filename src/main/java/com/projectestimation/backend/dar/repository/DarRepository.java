package com.projectestimation.backend.dar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.dar.model.Dar;

public interface DarRepository extends JpaRepository<Dar, Long> {

    Optional<Dar> findByOpportunityId(Long opportunityId);

    boolean existsByOpportunityId(Long opportunityId);
}