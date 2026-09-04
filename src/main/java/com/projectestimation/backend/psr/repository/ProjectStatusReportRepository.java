package com.projectestimation.backend.psr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.psr.model.ProjectStatusReport;

public interface ProjectStatusReportRepository
        extends JpaRepository<ProjectStatusReport, Long> {

    Optional<ProjectStatusReport> findByOpportunityIdAndVersion(
            Long opportunityId,
            Integer version
    );

    List<ProjectStatusReport> findByOpportunityIdOrderByVersionAsc(
            Long opportunityId
    );
}