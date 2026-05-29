package com.boki.backend.domain.ruleset.exception;

import com.boki.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RuleSetErrorCode implements BaseErrorCode {

    RULE_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "RULESET404", "매매원칙 세트를 찾을 수 없습니다."),
    RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "RULESET404_1", "매매원칙을 찾을 수 없습니다."),
    RULE_SET_FORBIDDEN(HttpStatus.FORBIDDEN, "RULESET403", "해당 매매원칙 세트에 접근할 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
