package com.projectestimation.backend.parameters.repository;

import com.projectestimation.backend.parameters.model.Parameters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParametersRepository extends JpaRepository<Parameters, Long> {

    @Query("SELECT p FROM Parameters p WHERE p.opportunity.id = :opportunityId")
    Optional<Parameters> findByOpportunityId(@Param("opportunityId") Long opportunityId);
}
