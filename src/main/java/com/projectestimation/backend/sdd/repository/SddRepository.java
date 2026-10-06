package com.projectestimation.backend.sdd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.sdd.model.Sdd;

public interface SddRepository extends JpaRepository<Sdd, Long> {

    Optional<Sdd> findByOpportunityId(Long opportunityId);

    boolean existsByOpportunityId(Long opportunityId);
}