package com.boki.backend.domain.ai.exception;

import com.boki.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiReportErrorCode implements BaseErrorCode {

    AI_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI404_1", "AI 분석 리포트를 찾을 수 없습니다."),
    AI_REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "AI409_1", "이미 생성된 AI 분석 리포트가 있습니다."),
    TRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "AI404_2", "거래를 찾을 수 없습니다."),
    TRADE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AI403_1", "해당 거래에 접근할 권한이 없습니다."),
    AI_API_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI500_1", "AI API 호출에 실패했습니다."),
    AI_RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI500_2", "AI 응답 파싱에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
