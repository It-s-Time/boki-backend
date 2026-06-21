package com.boki.backend.domain.review.repository;

import com.boki.backend.domain.review.entity.ReviewImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findAllByReviewReviewIdOrderByOrderIndexAsc(Long reviewId);

    void deleteAllByReviewReviewId(Long reviewId);
}
