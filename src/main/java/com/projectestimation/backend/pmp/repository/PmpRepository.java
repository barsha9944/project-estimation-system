package com.projectestimation.backend.pmp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.pmp.model.Pmp;

public interface PmpRepository extends JpaRepository<Pmp, Long> {

    Optional<Pmp> findByOpportunityId(Long opportunityId);

}