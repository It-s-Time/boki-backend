package com.boki.backend.domain.trade.dto.response;

import java.time.LocalDate;

public record TradeCalendarDayResponse(
        LocalDate date,
        boolean hasTrade,
        long tradeCount,
        long buyCount,
        long sellCount
) {
}
