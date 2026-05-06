package com.boki.backend.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK, "COMMON200", "성공입니다."),
    CREATED(HttpStatus.CREATED, "COMMON201", "생성되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON204", "삭제되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
