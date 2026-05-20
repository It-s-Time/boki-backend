package com.boki.backend.domain.exchange.dto.response;

import com.boki.backend.domain.trade.dto.response.TradeResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "거래소 거래 동기화 응답")
public record ExchangeTradeSyncResponse(
        int syncedCount,
        int skippedCount,
        List<TradeResponse> trades
) {
}
