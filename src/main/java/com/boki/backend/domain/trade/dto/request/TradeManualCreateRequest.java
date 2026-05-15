package com.boki.backend.domain.trade.dto.request;

import com.boki.backend.domain.trade.entity.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "수동 거래 입력 요청")
public record TradeManualCreateRequest(
        @Schema(description = "룰셋 ID", example = "1")
        Long ruleSetId,

        @Schema(description = "코인 종류", example = "BTC")
        @NotBlank(message = "코인 종류는 필수입니다.")
        @Size(max = 50, message = "코인 종류는 50자 이하여야 합니다.")
        String coinType,

        @Schema(description = "거래 유형", example = "BUY")
        @NotNull(message = "거래 유형은 필수입니다.")
        TradeType tradeType,

        @Schema(description = "거래 단가", example = "90000000")
        @NotNull(message = "거래 단가는 필수입니다.")
        @Positive(message = "거래 단가는 0보다 커야 합니다.")
        BigDecimal price,

        @Schema(description = "거래 수량", example = "0.01")
        @NotNull(message = "거래 수량은 필수입니다.")
        @Positive(message = "거래 수량은 0보다 커야 합니다.")
        BigDecimal quantity,

        @Schema(description = "거래 일시", example = "2026-05-08T10:30:00")
        @NotNull(message = "거래 일시는 필수입니다.")
        LocalDateTime tradedAt
) {
}
