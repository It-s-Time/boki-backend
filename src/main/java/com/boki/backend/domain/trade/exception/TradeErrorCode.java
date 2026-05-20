package com.boki.backend.domain.trade.exception;

import com.boki.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TradeErrorCode implements BaseErrorCode {

    TRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "TRADE404", "거래를 찾을 수 없습니다."),
    TRADE_FORBIDDEN(HttpStatus.FORBIDDEN, "TRADE403", "해당 거래에 접근할 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
