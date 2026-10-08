package com.projectestimation.backend.opportunity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.opportunity.model.ProjectTeam;

public interface ProjectTeamRepository
        extends JpaRepository<ProjectTeam, Long> {

    Optional<ProjectTeam> findByOpportunityId(Long opportunityId);
}