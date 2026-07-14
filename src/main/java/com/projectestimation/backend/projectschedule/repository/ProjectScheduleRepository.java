package com.projectestimation.backend.projectschedule.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.projectschedule.model.ProjectSchedule;

public interface ProjectScheduleRepository
        extends JpaRepository<ProjectSchedule, Long> {

    Optional<ProjectSchedule> findByOpportunityId(Long opportunityId);

}