package com.projectestimation.backend.review.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projectestimation.backend.review.model.DocumentReview;
import com.projectestimation.backend.review.model.DocumentReviewType;
import com.projectestimation.backend.review.model.ReviewStatus;

@Repository
public interface DocumentReviewRepository extends JpaRepository<DocumentReview, Long> {

    @EntityGraph(attributePaths = {"opportunity"})
    Optional<DocumentReview> findByOpportunityIdAndDocumentType(
            Long opportunityId,
            DocumentReviewType documentType
    );

    @EntityGraph(attributePaths = {"opportunity"})
    List<DocumentReview> findByCycle1StatusAndCycle1ScheduledAtLessThanEqual(
            ReviewStatus cycle1Status,
            LocalDateTime currentTime
    );

    @EntityGraph(attributePaths = {"opportunity"})
    List<DocumentReview> findByCycle2StatusAndCycle2ScheduledAtLessThanEqual(
            ReviewStatus cycle2Status,
            LocalDateTime currentTime
    );

    @EntityGraph(attributePaths = {"opportunity"})
    List<DocumentReview> findByOpportunityId(Long opportunityId);
}
