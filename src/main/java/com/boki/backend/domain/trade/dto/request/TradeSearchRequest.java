package com.boki.backend.domain.trade.dto.request;

import com.boki.backend.domain.trade.entity.TradeType;
import java.time.LocalDate;

public record TradeSearchRequest(
        LocalDate date,
        TradeType tradeType,
        ReviewStatus reviewStatus
) {
}
