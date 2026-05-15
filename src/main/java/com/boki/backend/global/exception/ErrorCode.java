package com.boki.backend.global.exception;

//임시 
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    RULE_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "매매원칙 세트를 찾을 수 없습니다."),
    RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "매매원칙을 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
