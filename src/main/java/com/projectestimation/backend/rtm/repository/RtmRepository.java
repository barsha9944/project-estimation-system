package com.projectestimation.backend.rtm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.rtm.model.Rtm;

@Repository
public interface RtmRepository extends JpaRepository<Rtm, Long> {

    Optional<Rtm> findByOpportunityId(Long opportunityId);

    boolean existsByOpportunityId(Long opportunityId);
}
