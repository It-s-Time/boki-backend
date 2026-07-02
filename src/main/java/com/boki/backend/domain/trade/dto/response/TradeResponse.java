package com.boki.backend.domain.trade.dto.response;

import com.boki.backend.domain.trade.entity.Trade;
import com.boki.backend.domain.trade.entity.TradeInputType;
import com.boki.backend.domain.trade.entity.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "거래 응답")
public record TradeResponse(
        Long tradeId,
        Long ruleSetId,
        Long memberId,
        String coinType,
        TradeType tradeType,
        BigDecimal price,
        BigDecimal totalAmount,
        BigDecimal quantity,
        TradeInputType inputType,
        LocalDateTime tradedAt,
        String externalTradeId,
        LocalDateTime createdAt
) {

    public static TradeResponse from(Trade trade) {
        return new TradeResponse(
                trade.getTradeId(),
                trade.getRuleSetId(),
                trade.getMemberId(),
                trade.getCoinType(),
                trade.getTradeType(),
                trade.getPrice(),
                trade.getTotalAmount(),
                trade.getQuantity(),
                trade.getInputType(),
                trade.getTradedAt(),
                trade.getExternalTradeId(),
                trade.getCreatedAt()
        );
    }
}
