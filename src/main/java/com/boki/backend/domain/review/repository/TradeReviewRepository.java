package com.boki.backend.domain.review.repository;

import com.boki.backend.domain.review.entity.TradeReview;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeReviewRepository extends JpaRepository<TradeReview, Long> {

    Optional<TradeReview> findByTradeId(Long tradeId);

    Optional<TradeReview> findByTradeIdAndMemberId(Long tradeId, Long memberId);

    boolean existsByTradeId(Long tradeId);
}
