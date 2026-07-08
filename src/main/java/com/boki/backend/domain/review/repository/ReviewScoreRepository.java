package com.boki.backend.domain.review.repository;

import com.boki.backend.domain.review.entity.ReviewScore;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewScoreRepository extends JpaRepository<ReviewScore, Long> {

    List<ReviewScore> findAllByReviewReviewId(Long reviewId);

    void deleteAllByReviewReviewId(Long reviewId);

    @Query("SELECT rs.ruleId, AVG(rs.score) FROM ReviewScore rs " +
           "JOIN TradeReview tr ON rs.review.reviewId = tr.reviewId " +
           "WHERE tr.memberId = :memberId " +
           "GROUP BY rs.ruleId " +
           "ORDER BY AVG(rs.score) ASC " +
           "LIMIT 3")
    List<Object[]> findWorstRulesByMemberId(@Param("memberId") Long memberId);
}
