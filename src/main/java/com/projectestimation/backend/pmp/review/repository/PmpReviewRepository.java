package com.projectestimation.backend.pmp.review.repository;

import com.projectestimation.backend.pmp.review.entity.PmpReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PmpReviewRepository
        extends JpaRepository<PmpReview, Long> {

    Optional<PmpReview> findByOpportunityId(Long opportunityId);

    List<PmpReview> findByStatusIn(List<String> statuses);
}
