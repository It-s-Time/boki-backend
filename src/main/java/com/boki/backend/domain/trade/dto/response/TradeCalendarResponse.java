package com.boki.backend.domain.trade.dto.response;

import java.util.List;

public record TradeCalendarResponse(
        int year,
        int month,
        List<TradeCalendarDayResponse> days
) {
}
