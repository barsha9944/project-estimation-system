package com.projectestimation.backend.srs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.srs.model.Srs;

@Repository
public interface SrsRepository extends JpaRepository<Srs, Long> {

    Optional<Srs> findByOpportunityId(Long opportunityId);

    boolean existsByOpportunityId(Long opportunityId);
}