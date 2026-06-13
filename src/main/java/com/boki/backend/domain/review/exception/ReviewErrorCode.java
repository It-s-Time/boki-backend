package com.boki.backend.domain.review.exception;

import com.boki.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReviewErrorCode implements BaseErrorCode {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404", "복기를 찾을 수 없습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW409", "이미 복기한 거래입니다."),
    REVIEW_RULE_SET_FORBIDDEN(HttpStatus.FORBIDDEN, "REVIEW403_1", "해당 룰셋에 접근할 권한이 없습니다."),
    REVIEW_RULE_SET_MISMATCH(HttpStatus.BAD_REQUEST, "REVIEW400_2", "거래에 연결된 룰셋과 복기 룰셋이 일치하지 않습니다."),
    REVIEW_INVALID_RULE_SCORE(HttpStatus.BAD_REQUEST, "REVIEW400_3", "복기 원칙 점수 정보가 올바르지 않습니다."),
    REVIEW_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "REVIEW400_4", "복기 이미지는 최대 3장까지 업로드할 수 있습니다."),
    REVIEW_IMAGE_UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "REVIEW400_5", "지원하지 않는 이미지 형식입니다."),
    REVIEW_IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "REVIEW400_6", "이미지 파일 크기를 초과했습니다."),
    REVIEW_IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REVIEW500_1", "복기 이미지 업로드에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
