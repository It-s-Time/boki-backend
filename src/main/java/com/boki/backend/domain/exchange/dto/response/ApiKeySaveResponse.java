package com.boki.backend.domain.exchange.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래소 API Key 등록/갱신 응답")
public record ApiKeySaveResponse(
        Long memberId,
        boolean connected
) {
}
