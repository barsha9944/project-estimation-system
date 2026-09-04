package com.projectestimation.backend.opportunity.repository;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.proposal.model.Proposal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    List<Opportunity> findByStatus(OpportunityStatus status);

    List<Opportunity> findByOpportunityNameContainingIgnoreCase(String opportunityName);

    List<Opportunity> findByClientNameContainingIgnoreCase(String clientName);
    
    long countByStatus(OpportunityStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Opportunity o WHERE o.id = :opportunityId")
    Optional<Opportunity> findByIdForPsrSynchronization(@Param("opportunityId") Long opportunityId);

   
}
