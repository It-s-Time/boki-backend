package com.boki.backend.domain.exchange.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래소 API 키 등록 여부 응답")
public record ApiKeyStatusResponse(
        boolean connected
) {
}
