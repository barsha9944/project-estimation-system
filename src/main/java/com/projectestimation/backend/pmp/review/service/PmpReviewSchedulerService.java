
package com.projectestimation.backend.pmp.review.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.pmp.review.entity.PmpReview;
import com.projectestimation.backend.pmp.review.repository.PmpReviewRepository;

@Service
public class PmpReviewSchedulerService {

    private static final Logger log =
            LoggerFactory.getLogger(PmpReviewSchedulerService.class);

    private final PmpReviewRepository reviewRepository;
    private final PmpReviewService reviewService;

    public PmpReviewSchedulerService(
            PmpReviewRepository reviewRepository,
            PmpReviewService reviewService) {

        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
    }

    @Scheduled(fixedDelay = 60000)
    public void processReviews() {

        LocalDateTime now = LocalDateTime.now();

        List<PmpReview> reviews = reviewRepository.findByStatusIn(
                List.of(
                        "SCHEDULED",
                        "RETRY_PENDING",
                        "WAITING_FOR_FOLLOW_UP"
                )
        );

        for (PmpReview review : reviews) {
            try {
                String status = review.getStatus();

                if ("WAITING_FOR_FOLLOW_UP".equals(status)) {

                    if (review.getFollowUpDueAt() != null
                            && !now.isBefore(review.getFollowUpDueAt())) {

                        reviewService.generateFollowUp(
                                review.getOpportunityId());
                    }

                } else if (review.getReviewDocumentsDueAt() != null
                        && !now.isBefore(
                                review.getReviewDocumentsDueAt())) {

                    reviewService.generateReviewDocuments(
                            review.getOpportunityId());
                }

            } catch (Exception ex) {
                log.error(
                        "PMP review processing failed for opportunity {}",
                        review.getOpportunityId(),
                        ex);
            }
        }
    }
}
