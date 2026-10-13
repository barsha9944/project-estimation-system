package com.projectestimation.backend.dar.review.repository;

import com.projectestimation.backend.dar.review.entity.DarReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DarReviewRepository
        extends JpaRepository<DarReview, Long> {

    Optional<DarReview> findByOpportunityId(Long opportunityId);
}
