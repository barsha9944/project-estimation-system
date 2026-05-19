package com.projectestimation.backend.opportunity.repository;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    List<Opportunity> findByStatus(OpportunityStatus status);

    List<Opportunity> findByOpportunityNameContainingIgnoreCase(String opportunityName);

    List<Opportunity> findByClientNameContainingIgnoreCase(String clientName);
}
