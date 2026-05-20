package com.boki.backend.domain.exchange.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpbitClosedOrderResponse(
        String uuid,
        String side,
        String market,
        BigDecimal price,
        BigDecimal volume,
        @JsonProperty("executed_volume")
        BigDecimal executedVolume,
        @JsonProperty("executed_funds")
        BigDecimal executedFunds,
        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
}
