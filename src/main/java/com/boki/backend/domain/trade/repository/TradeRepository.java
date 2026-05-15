package com.boki.backend.domain.trade.repository;

import com.boki.backend.domain.trade.entity.Trade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findAllByMemberIdOrderByTradedAtDescTradeIdDesc(Long memberId);

    boolean existsByMemberIdAndExternalTradeId(Long memberId, String externalTradeId);
}
