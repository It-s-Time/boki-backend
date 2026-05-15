package com.boki.backend.domain.exchange.controller;

import com.boki.backend.domain.exchange.dto.request.ApiKeySaveRequest;
import com.boki.backend.domain.exchange.dto.response.ApiKeySaveResponse;
import com.boki.backend.domain.exchange.dto.response.ExchangeTradeSyncResponse;
import com.boki.backend.domain.exchange.service.ExchangeApiKeyService;
import com.boki.backend.domain.exchange.service.ExchangeTradeSyncService;
import com.boki.backend.global.apiPayload.ApiResponse;
import com.boki.backend.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Exchange", description = "거래소 연동 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exchange")
public class ExchangeController {

    private final ExchangeApiKeyService exchangeApiKeyService;
    private final ExchangeTradeSyncService exchangeTradeSyncService;

    @Operation(summary = "거래소 API Key 등록/갱신", description = "현재 사용자 기준 거래소 API Key를 등록하거나 갱신합니다.")
    @PostMapping("/api-key")
    public ResponseEntity<ApiResponse<ApiKeySaveResponse>> saveApiKey(
            @Valid @RequestBody ApiKeySaveRequest request
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, exchangeApiKeyService.saveCredential(request)));
    }

    @Operation(summary = "거래소 거래 수동 동기화", description = "현재 사용자 API Key로 거래소 종료 주문을 조회해 거래 내역을 저장합니다.")
    @PostMapping("/sync/trades")
    public ResponseEntity<ApiResponse<ExchangeTradeSyncResponse>> syncTrades() {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, exchangeTradeSyncService.syncCurrentUserTrades()));
    }
}
