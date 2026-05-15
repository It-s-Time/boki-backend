package com.boki.backend.domain.trade.controller;

import com.boki.backend.domain.trade.dto.request.TradeManualCreateRequest;
import com.boki.backend.domain.trade.dto.request.TradeUpdateRequest;
import com.boki.backend.domain.trade.dto.response.TradeResponse;
import com.boki.backend.domain.trade.service.TradeService;
import com.boki.backend.global.apiPayload.ApiResponse;
import com.boki.backend.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Trade", description = "거래 입력 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "거래 목록 조회", description = "인증 사용자 기준 거래 목록을 최신 거래일 순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TradeResponse>>> getTrades() {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, tradeService.getTrades()));
    }

    @Operation(summary = "수동 거래 입력", description = "인증 사용자 기준 수동 거래 데이터를 생성합니다.")
    @PostMapping("/manual")
    public ResponseEntity<ApiResponse<TradeResponse>> createManualTrade(
            @Valid @RequestBody TradeManualCreateRequest request
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.CREATED.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, tradeService.createManualTrade(request)));
    }

    @Operation(summary = "거래 상세 조회", description = "인증 사용자 소유 거래 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TradeResponse>> getTrade(
            @PathVariable("id") @Positive(message = "거래 ID는 0보다 커야 합니다.") Long tradeId
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, tradeService.getTrade(tradeId)));
    }

    @Operation(summary = "거래 수정", description = "인증 사용자 소유 거래 정보를 부분 수정합니다.")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TradeResponse>> updateTrade(
            @PathVariable("id") @Positive(message = "거래 ID는 0보다 커야 합니다.") Long tradeId,
            @Valid @RequestBody TradeUpdateRequest request
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, tradeService.updateTrade(tradeId, request)));
    }

    @Operation(summary = "거래 삭제", description = "인증 사용자 소유 거래를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrade(
            @PathVariable("id") @Positive(message = "거래 ID는 0보다 커야 합니다.") Long tradeId
    ) {
        tradeService.deleteTrade(tradeId);
        return ResponseEntity
                .status(GeneralSuccessCode.DELETED.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.DELETED, null));
    }
}
