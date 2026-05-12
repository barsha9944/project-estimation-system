package com.projectestimation.backend.proposal.repository;

import com.projectestimation.backend.proposal.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
}
