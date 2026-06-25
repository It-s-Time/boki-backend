package com.boki.backend.domain.ai.controller;

import com.boki.backend.domain.ai.dto.response.AiReportResDTO;
import com.boki.backend.domain.ai.service.AiReportService;
import com.boki.backend.global.apiPayload.ApiResponse;
import com.boki.backend.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Report", description = "AI 분석 리포트 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-reports")
public class AiReportController {

    private final AiReportService aiReportService;

    @Operation(summary = "AI 분석 리포트 생성", description = "거래에 대한 AI 분석 리포트를 생성합니다.")
    @PostMapping("/{tradeId}")
    public ResponseEntity<ApiResponse<AiReportResDTO>> generateReport(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("tradeId") @Positive(message = "거래 ID는 0보다 커야 합니다.") Long tradeId
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.CREATED.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, aiReportService.generateReport(memberId, tradeId)));
    }

    @Operation(summary = "AI 분석 리포트 조회", description = "거래에 대한 기존 AI 분석 리포트를 조회합니다.")
    @GetMapping("/{tradeId}")
    public ResponseEntity<ApiResponse<AiReportResDTO>> getReport(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("tradeId") @Positive(message = "거래 ID는 0보다 커야 합니다.") Long tradeId
    ) {
        return ResponseEntity
                .status(GeneralSuccessCode.OK.getStatus())
                .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, aiReportService.getReport(memberId, tradeId)));
    }
}
