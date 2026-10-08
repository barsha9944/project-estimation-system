package com.projectestimation.backend.mom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.mom.model.Mom;

public interface MomRepository extends JpaRepository<Mom, Long> {

    List<Mom> findByOpportunityIdOrderByMeetingSequenceAsc(
            Long opportunityId
    );

    boolean existsByOpportunityId(
            Long opportunityId
    );
}