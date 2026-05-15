package com.boki.backend.domain.trade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "API 연동 거래 동기화 요청")
public record TradeSyncRequest(
        @Schema(description = "동기화할 거래 목록")
        @NotEmpty(message = "동기화할 거래 목록은 비어 있을 수 없습니다.")
        List<@Valid TradeSyncItemRequest> trades
) {
}
