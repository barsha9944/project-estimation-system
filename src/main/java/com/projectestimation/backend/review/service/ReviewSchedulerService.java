package com.projectestimation.backend.review.service;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.review.model.DocumentReview;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewSchedulerService {

    private static final Logger log = LogManager.getLogger(ReviewSchedulerService.class);

    private final DocumentReviewService documentReviewService;

    @Scheduled(fixedDelay = 60000) // Runs every 60 seconds
    public void processDueReviews() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Process Due Cycle 1 Reviews
        try {
            List<DocumentReview> dueCycle1 = documentReviewService.findDueCycle1Reviews(now);
            if (dueCycle1 != null && !dueCycle1.isEmpty()) {
                log.info("Found {} due Cycle 1 document reviews to process", dueCycle1.size());
                for (DocumentReview review : dueCycle1) {
                    try {
                        documentReviewService.processDueCycle1(review.getId());
                    } catch (Exception e) {
                        log.error("Error processing Cycle 1 review ID {}: {}", review.getId(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking for due Cycle 1 reviews: {}", e.getMessage(), e);
        }

        // 2. Process Due Cycle 2 Reviews
        try {
            List<DocumentReview> dueCycle2 = documentReviewService.findDueCycle2Reviews(now);
            if (dueCycle2 != null && !dueCycle2.isEmpty()) {
                log.info("Found {} due Cycle 2 document reviews to process", dueCycle2.size());
                for (DocumentReview review : dueCycle2) {
                    try {
                        documentReviewService.processDueCycle2(review.getId());
                    } catch (Exception e) {
                        log.error("Error processing Cycle 2 review ID {}: {}", review.getId(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking for due Cycle 2 reviews: {}", e.getMessage(), e);
        }
    }
}
