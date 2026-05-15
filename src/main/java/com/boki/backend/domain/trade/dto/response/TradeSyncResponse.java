package com.boki.backend.domain.trade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "API 연동 거래 동기화 응답")
public record TradeSyncResponse(
        int requestedCount,
        int savedCount,
        List<TradeResponse> trades
) {
}
