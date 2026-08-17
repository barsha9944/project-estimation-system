package com.projectestimation.backend.projectschedule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;

public interface ProjectScheduleTaskBreakdownRepository
        extends JpaRepository<ProjectScheduleTaskBreakdown, Long> {

    List<ProjectScheduleTaskBreakdown> findByProjectScheduleTaskProjectScheduleOpportunityId(
            Long opportunityId);
}