package com.projectestimation.backend.pmp.review.repository;

import com.projectestimation.backend.pmp.review.entity.PmpReview;
import com.projectestimation.backend.pmp.review.entity.PmpReviewDocument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PmpReviewDocumentRepository
        extends JpaRepository<PmpReviewDocument, Long> {

    List<PmpReviewDocument> findByReview(PmpReview review);

    Optional<PmpReviewDocument> findByReviewAndDocumentType(
        PmpReview review,
        String documentType
    );

    List<PmpReviewDocument> findByStatusIn(List<String> statuses);
}