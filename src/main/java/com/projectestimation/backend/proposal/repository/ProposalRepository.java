package com.projectestimation.backend.proposal.repository;

import com.projectestimation.backend.proposal.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    @Query("SELECT p FROM Proposal p WHERE p.opportunity.id = :opportunityId ORDER BY p.version DESC")
    List<Proposal> findByOpportunityId(@Param("opportunityId") Long opportunityId);

    Optional<Proposal> findFirstByOpportunity_IdOrderByVersionDesc(Long opportunityId);
    
    List<Proposal> findAllByOrderByCreatedAtDesc();
}
