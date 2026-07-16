package com.projectestimation.backend.projectschedule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;

public interface ProjectScheduleTaskRepository
        extends JpaRepository<ProjectScheduleTask, Long> {
	
	List<ProjectScheduleTask> findByProjectScheduleId(Long projectScheduleId);
	
	void deleteByProjectScheduleId(Long projectScheduleId);

}