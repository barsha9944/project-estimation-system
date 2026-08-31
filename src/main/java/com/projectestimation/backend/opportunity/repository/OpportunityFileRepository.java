package com.projectestimation.backend.opportunity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projectestimation.backend.util.FileStorage;

public interface OpportunityFileRepository extends JpaRepository<FileStorage, String> {

	@Query("SELECT o FROM FileStorage o WHERE o.id = :id")
	Optional<FileStorage> findByFileId(@Param("id") String id);

}
