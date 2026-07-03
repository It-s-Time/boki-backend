package com.boki.backend.domain.trade.dto.request;

import com.boki.backend.domain.trade.entity.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "거래 수정 요청")
public record TradeUpdateRequest(
        @Schema(description = "룰셋 ID", example = "1")
        Long ruleSetId,

        @Schema(description = "코인 종류", example = "BTC")
        @Size(max = 50, message = "코인 종류는 50자 이하여야 합니다.")
        String coinType,

        @Schema(description = "거래 유형", example = "BUY")
        TradeType tradeType,

        @Schema(description = "거래 단가", example = "91000000")
        @Positive(message = "거래 단가는 0보다 커야 합니다.")
        BigDecimal price,

        @Schema(description = "거래 총금액", example = "5000000")
        @Positive(message = "거래 총금액은 0보다 커야 합니다.")
        BigDecimal totalAmount,

        @Schema(description = "거래 일시", example = "2026-05-08T10:40:00")
        LocalDateTime tradedAt
) {
}
