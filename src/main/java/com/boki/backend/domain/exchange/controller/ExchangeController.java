package com.boki.backend.domain.exchange.controller;

import com.boki.backend.domain.exchange.dto.request.ApiKeySaveRequest;
import com.boki.backend.domain.exchange.dto.response.ApiKeySaveResponse;
import com.boki.backend.domain.exchange.dto.response.ApiKeyStatusResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Operation(summary = "거래소 API 키 등록 여부 조회", description = "현재 사용자의 거래소 API 키 등록 여부를 조회합니다.")
    @GetMapping("/api-key")
    public ResponseEntity<ApiResponse<ApiKeyStatusResponse>> getApiKeyStatus(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, exchangeApiKeyService.getApiKeyStatus(memberId)));
    }

    @Operation(summary = "거래소 API Key 등록/갱신", description = "현재 사용자 기준 거래소 API Key를 등록하거나 갱신합니다.")
    @PostMapping("/api-key")
    public ResponseEntity<ApiResponse<ApiKeySaveResponse>> saveApiKey(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ApiKeySaveRequest request
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, exchangeApiKeyService.saveCredential(memberId, request)));
    }

    @Operation(
            summary = "검증된 거래소 API Key 등록/갱신",
            description = "자산조회와 주문조회만 허용된 API Key를 검증한 뒤 등록하거나 갱신합니다."
    )
    @PostMapping("/api-key/verified")
    public ResponseEntity<ApiResponse<ApiKeySaveResponse>> saveVerifiedApiKey(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ApiKeySaveRequest request
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(
                        GeneralSuccessCode.OK,
                        exchangeApiKeyService.saveVerifiedCredential(memberId, request)
                ));
    }

    @Operation(summary = "거래소 API Key 삭제", description = "현재 사용자의 거래소 API Key를 삭제합니다.")
    @DeleteMapping("/api-key")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(
            @AuthenticationPrincipal Long memberId
    ) {
        exchangeApiKeyService.deleteCredential(memberId);
        return ResponseEntity
                .status(GeneralSuccessCode.DELETED.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.DELETED, null));
    }

    @Operation(summary = "거래소 거래 수동 동기화", description = "현재 사용자 API Key로 거래소 종료 주문을 조회해 거래 내역을 저장합니다.")
    @PostMapping("/sync/trades")
    public ResponseEntity<ApiResponse<ExchangeTradeSyncResponse>> syncTrades(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, exchangeTradeSyncService.syncCurrentUserTrades(memberId)));
    }
}
