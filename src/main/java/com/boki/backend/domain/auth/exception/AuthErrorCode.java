package com.boki.backend.domain.auth.exception;

import com.boki.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400_1", "지원하지 않는 소셜 로그인 provider입니다."),
    INVALID_OAUTH_CODE(HttpStatus.UNAUTHORIZED, "AUTH401_1", "소셜 로그인 인증 코드가 유효하지 않습니다."),
    OAUTH_USER_INFO_FAILED(HttpStatus.UNAUTHORIZED, "AUTH401_2", "소셜 사용자 정보 조회에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_3", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_4", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH401_5", "Refresh token을 찾을 수 없습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH401_6", "Refresh token이 일치하지 않습니다."),
    OAUTH_CONFIGURATION_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_1", "OAuth 설정이 누락되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
