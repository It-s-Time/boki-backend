package com.boki.backend.domain.review.repository;

import com.boki.backend.domain.review.entity.ReviewScore;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewScoreRepository extends JpaRepository<ReviewScore, Long> {

    List<ReviewScore> findAllByReviewReviewId(Long reviewId);

    void deleteAllByReviewReviewId(Long reviewId);
}
