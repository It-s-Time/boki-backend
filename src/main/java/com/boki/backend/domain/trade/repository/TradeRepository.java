package com.boki.backend.domain.trade.repository;

import com.boki.backend.domain.trade.entity.Trade;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findAllByMemberIdOrderByTradedAtDescTradeIdDesc(Long memberId);

    List<Trade> findAllByMemberIdAndTradedAtGreaterThanEqualAndTradedAtLessThanOrderByTradedAtDescTradeIdDesc(
            Long memberId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    boolean existsByMemberIdAndExternalTradeId(Long memberId, String externalTradeId);
}
