package com.boki.backend.domain.exchange.exception;

import com.boki.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExchangeErrorCode implements BaseErrorCode {

    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "EXCHANGE400", "유효하지 않은 거래소 API Key입니다."),
    REQUIRED_PERMISSION_MISSING(
            HttpStatus.BAD_REQUEST,
            "EXCHANGE400_1",
            "거래소 API Key에 자산조회와 주문조회 권한이 필요합니다."
    ),
    UNNECESSARY_PERMISSION_INCLUDED(
            HttpStatus.BAD_REQUEST,
            "EXCHANGE400_2",
            "거래소 API Key에 허용되지 않은 추가 권한이 포함되어 있습니다."
    ),
    CREDENTIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "EXCHANGE404", "등록된 거래소 API Key가 없습니다."),
    API_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "EXCHANGE502", "거래소 API 요청에 실패했습니다."),
    ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EXCHANGE500", "거래소 API Key 암호화 처리에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
